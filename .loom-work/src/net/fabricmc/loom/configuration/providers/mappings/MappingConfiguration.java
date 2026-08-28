/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016-2023 FabricMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.fabricmc.loom.configuration.providers.mappings;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.base.Stopwatch;
import com.google.common.base.Supplier;
import com.google.gson.JsonObject;
import dev.architectury.loom.util.MappingOption;
import org.apache.tools.ant.util.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.LoomGradlePlugin;
import net.fabricmc.loom.api.mappings.layered.MappingContext;
import net.fabricmc.loom.configuration.DependencyInfo;
import net.fabricmc.loom.configuration.providers.forge.ForgeMigratedMappingConfiguration;
import net.fabricmc.loom.configuration.providers.forge.SrgProvider;
import net.fabricmc.loom.configuration.providers.mappings.tiny.MappingsMerger;
import net.fabricmc.loom.configuration.providers.mappings.tiny.TinyJarInfo;
import net.fabricmc.loom.configuration.providers.minecraft.MinecraftProvider;
import net.fabricmc.loom.util.Constants;
import net.fabricmc.loom.util.DeletingFileVisitor;
import net.fabricmc.loom.util.FileSystemUtil;
import net.fabricmc.loom.util.ZipUtils;
import net.fabricmc.loom.util.service.ScopedServiceFactory;
import net.fabricmc.loom.util.service.ServiceFactory;
import net.fabricmc.loom.util.srg.ForgeMappingsMerger;
import net.fabricmc.loom.util.srg.MCPReader;
import net.fabricmc.loom.util.srg.SrgNamedWriter;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.format.tiny.Tiny2FileWriter;
import net.fabricmc.stitch.Command;
import net.fabricmc.stitch.commands.CommandProposeFieldNames;
import net.fabricmc.stitch.commands.tinyv2.TinyFile;
import net.fabricmc.stitch.commands.tinyv2.TinyV2Writer;

public class MappingConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(MappingConfiguration.class);

	public final String mappingsIdentifier;

	private final Path mappingsWorkingDir;
	// The mappings that gradle gives us
	private final Path baseTinyMappings;
	// The mappings we use in practice
	public Path tinyMappings;
	public final Path tinyMappingsJar;
	public Path tinyMappingsWithMojang;
	public Path tinyMappingsWithSrg;
	public final Map<String, Path> mixinTinyMappings; // The mixin mappings have other names in intermediary.
	public final Path srgToNamedSrg; // FORGE: srg to named in srg file format
	private final Map<MappingOption, Supplier<Path>> mappingOptions;
	private final Path unpickDefinitions;

	private boolean hasUnpickDefinitions;
	private UnpickMetadata unpickMetadata;
	private Map<String, String> signatureFixes;

	protected MappingConfiguration(String mappingsIdentifier, Path mappingsWorkingDir) {
		this.mappingsIdentifier = mappingsIdentifier;

		this.mappingsWorkingDir = mappingsWorkingDir;
		this.baseTinyMappings = mappingsWorkingDir.resolve("mappings-base.tiny");
		this.tinyMappings = mappingsWorkingDir.resolve("mappings.tiny");
		this.tinyMappingsJar = mappingsWorkingDir.resolve("mappings.jar");
		this.unpickDefinitions = mappingsWorkingDir.resolve("mappings.unpick");
		this.tinyMappingsWithSrg = mappingsWorkingDir.resolve("mappings-srg.tiny");
		this.tinyMappingsWithMojang = mappingsWorkingDir.resolve("mappings-mojang.tiny");
		this.mixinTinyMappings = new HashMap<>();
		this.srgToNamedSrg = mappingsWorkingDir.resolve("mappings-srg-named.srg");
		this.mappingOptions = new EnumMap<>(MappingOption.class);
		this.mappingOptions.put(MappingOption.DEFAULT, () -> this.tinyMappings);
	}

	public static MappingConfiguration create(Project project, ServiceFactory serviceFactory, DependencyInfo dependency, MinecraftProvider minecraftProvider) {
		final String version = dependency.getResolvedVersion();
		final Path inputJar = dependency.resolveFile().orElseThrow(() -> new RuntimeException("Could not resolve mappings: " + dependency)).toPath();
		final String mappingsName = StringUtils.removeSuffix(dependency.getDependency().getGroup() + "." + dependency.getDependency().getName(), "-unmerged");

		final TinyJarInfo jarInfo = TinyJarInfo.get(inputJar);
		jarInfo.minecraftVersionId().ifPresent(id -> {
			if (!minecraftProvider.minecraftVersion().equals(id)) {
				LOGGER.warn("The mappings (%s) were not built for Minecraft version %s, proceed with caution.".formatted(dependency.getDepString(), minecraftProvider.minecraftVersion()));
			}
		});

		final LoomGradleExtension extension = LoomGradleExtension.get(project);
		String mappingsIdentifier;

		if (extension.isForgeLike()) {
			mappingsIdentifier = createForgeMappingsIdentifier(extension, mappingsName, version, getMappingsClassifier(dependency, jarInfo.v2()), minecraftProvider.minecraftVersion());
		} else {
			mappingsIdentifier = createMappingsIdentifier(mappingsName, version, getMappingsClassifier(dependency, jarInfo.v2()), minecraftProvider.minecraftVersion());
		}

		if (extension.isQuilt()) {
			mappingsIdentifier += "-arch-quilt";
		}

		final Path workingDir = minecraftProvider.dir(mappingsIdentifier).toPath();

		MappingConfiguration mappingProvider;

		if (extension.isForgeLike()) {
			mappingProvider = new ForgeMigratedMappingConfiguration(mappingsIdentifier, workingDir);
		} else {
			mappingProvider = new MappingConfiguration(mappingsIdentifier, workingDir);
		}

		try {
			mappingProvider.setup(project, serviceFactory, minecraftProvider, inputJar);
		} catch (IOException e) {
			cleanWorkingDirectory(workingDir);
			throw new UncheckedIOException("Failed to setup mappings: " + dependency.getDepString(), e);
		}

		return mappingProvider;
	}

	public Path getMappingsPath(MappingOption mappingOption) {
		Supplier<Path> mappingsSupplier = this.mappingOptions.get(mappingOption);

		if (mappingsSupplier == null) {
			throw new UnsupportedOperationException("Unsupported mapping option: " + mappingOption + ", it is possible that this option is not supported by this project / platform!");
		} else if (Files.notExists(mappingsSupplier.get())) {
			throw new UnsupportedOperationException("Mapping option " + mappingOption + " found but file does not exist!");
		}

		return Objects.requireNonNull(mappingsSupplier.get());
	}

	public Provider<TinyMappingsService.Options> getMappingsServiceOptions(Project project) {
		return getMappingsServiceOptions(project, MappingOption.DEFAULT);
	}

	public Provider<TinyMappingsService.Options> getMappingsServiceOptions(Project project, MappingOption mappingOption) {
		return TinyMappingsService.createOptions(project, Objects.requireNonNull(getMappingsPath(mappingOption)));
	}

	public TinyMappingsService getMappingsService(Project project, ServiceFactory serviceFactory) {
		return serviceFactory.get(getMappingsServiceOptions(project));
	}

	public TinyMappingsService getMappingsService(Project project, ServiceFactory serviceFactory, MappingOption mappingOption) {
		return serviceFactory.get(getMappingsServiceOptions(project, mappingOption));
	}

	protected void setup(Project project, ServiceFactory serviceFactory, MinecraftProvider minecraftProvider, Path inputJar) throws IOException {
		if (minecraftProvider.refreshDeps()) {
			cleanWorkingDirectory(mappingsWorkingDir);
		}

		if (Files.notExists(tinyMappings) || minecraftProvider.refreshDeps()) {
			storeMappings(project, serviceFactory, minecraftProvider, inputJar);
		} else {
			try (FileSystemUtil.Delegate fileSystem = FileSystemUtil.getJarFileSystem(inputJar, false)) {
				extractExtras(fileSystem.get());
			}
		}

		if (Files.notExists(tinyMappingsJar) || minecraftProvider.refreshDeps()) {
			Files.deleteIfExists(tinyMappingsJar);
			ZipUtils.add(tinyMappingsJar, "mappings/mappings.tiny", Files.readAllBytes(tinyMappings));
		}
	}

	public void setupPost(Project project) throws IOException {
		LoomGradleExtension extension = LoomGradleExtension.get(project);

		if (extension.isNeoForge()) {
			this.mappingOptions.put(MappingOption.WITH_MOJANG, () -> this.tinyMappingsWithMojang);

			// Generate the Mojmap-merged mappings if needed.
			// Note that this needs to happen before manipulateMappings for FieldMigratedMappingConfiguration.
			if (Files.notExists(tinyMappingsWithMojang) || extension.refreshDeps()) {
				mergeMojang(project, tinyMappings, tinyMappingsWithMojang);
			}
		}

		if (extension.shouldGenerateSrgTiny()) {
			this.mappingOptions.put(MappingOption.WITH_SRG, () -> this.tinyMappingsWithSrg);

			if (extension.isForge() && extension.getForgeProvider().usesMojangAtRuntime()) {
				this.mappingOptions.put(MappingOption.WITH_MOJANG, () -> this.tinyMappingsWithSrg);
			}

			if (Files.notExists(tinyMappingsWithSrg) || extension.refreshDeps()) {
				if (extension.isForge() && extension.getForgeProvider().usesMojangAtRuntime()) {
					Path tmp = Files.createTempFile("mappings", ".tiny");
					mergeMojang(project, tinyMappings, tmp);
					mergeSrg(project, tmp, tinyMappingsWithSrg);
					Files.deleteIfExists(tmp);
				} else {
					mergeSrg(project, tinyMappings, tinyMappingsWithSrg);
				}
			}
		}

		manipulateMappings(project, tinyMappingsJar);
	}

	public void applyToProject(Project project, DependencyInfo dependency) throws IOException {
		if (hasUnpickDefinitions()) {
			String notation = String.format("%s:%s:%s:constants",
					dependency.getDependency().getGroup(),
					dependency.getDependency().getName(),
					dependency.getDependency().getVersion()
			);

			project.getDependencies().add(Constants.Configurations.MAPPING_CONSTANTS, notation);
			populateUnpickClasspath(project);
		}

		LoomGradleExtension extension = LoomGradleExtension.get(project);

		if (extension.isForge()) {
			if (!extension.shouldGenerateSrgTiny()) {
				throw new IllegalStateException("We have to generate srg tiny in a forge environment!");
			}

			if (Files.notExists(srgToNamedSrg) || extension.refreshDeps()) {
				try (var serviceFactory = new ScopedServiceFactory()) {
					TinyMappingsService mappingsService = getMappingsService(project, serviceFactory, MappingOption.WITH_SRG);
					SrgNamedWriter.writeTo(project.getLogger(), srgToNamedSrg, mappingsService.getMappingTree(), "srg", "named");
				}
			}
		}

		project.getDependencies().add(Constants.Configurations.MAPPINGS_FINAL, project.files(tinyMappingsJar.toFile()));
	}

	public static Path getRawSrgFile(Project project) throws IOException {
		LoomGradleExtension extension = LoomGradleExtension.get(project);

		if (extension.getSrgProvider().isTsrgV2()) {
			return extension.getSrgProvider().getMergedMojangTrimmed();
		}

		return extension.getSrgProvider().getSrg();
	}

	public static Path getMojmapSrgFileIfPossible(Project project) {
		try {
			LoomGradleExtension extension = LoomGradleExtension.get(project);
			return SrgProvider.getMojmapTsrg2(project, extension);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void mergeMojang(Project project, Path source, Path target) throws IOException {
		final Stopwatch stopwatch = Stopwatch.createStarted();
		final MappingContext context = new GradleMappingContext(project, "tmp-mojang");

		try (Tiny2FileWriter writer = new Tiny2FileWriter(Files.newBufferedWriter(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), false)) {
			ForgeMappingsMerger.mergeMojang(context, source, null, true).accept(writer);
		}

		project.getLogger().info(":merged mojang mappings in {}", stopwatch.stop());
	}

	private static void mergeSrg(Project project, Path source, Path target) throws IOException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		ForgeMappingsMerger.ExtraMappings extraMappings = ForgeMappingsMerger.ExtraMappings.ofMojmapTsrg(getMojmapSrgFileIfPossible(project));

		try (Tiny2FileWriter writer = new Tiny2FileWriter(Files.newBufferedWriter(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), false)) {
			ForgeMappingsMerger.mergeSrg(getRawSrgFile(project), source, extraMappings, true).accept(writer);
		}

		project.getLogger().info(":merged srg mappings in " + stopwatch.stop());
	}

	protected void manipulateMappings(Project project, Path mappingsJar) throws IOException {
	}

	private static String getMappingsClassifier(DependencyInfo dependency, boolean isV2) {
		String[] depStringSplit = dependency.getDepString().split(":");

		if (depStringSplit.length >= 4) {
			return "-" + depStringSplit[3] + (isV2 ? "-v2" : "");
		}

		return isV2 ? "-v2" : "";
	}

	private void storeMappings(Project project, ServiceFactory serviceFactory, MinecraftProvider minecraftProvider, Path inputJar) throws IOException {
		LOGGER.info(":extracting " + inputJar.getFileName());

		if (isMCP(inputJar)) {
			try {
				readAndMergeMCP(project, serviceFactory, minecraftProvider, inputJar);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			return;
		}

		try (FileSystemUtil.Delegate delegate = FileSystemUtil.getJarFileSystem(inputJar)) {
			extractMappings(delegate.fs(), baseTinyMappings);
			extractExtras(delegate.fs());
		}

		if (areMappingsV2(baseTinyMappings)) {
			// These are unmerged v2 mappings
			IntermediateMappingsService intermediateMappingsService = serviceFactory.get(IntermediateMappingsService.createOptions(project, minecraftProvider));

			MappingsMerger.mergeAndSaveMappings(baseTinyMappings, tinyMappings, minecraftProvider, intermediateMappingsService);
		} else {
			if (LoomGradleExtension.get(project).isForgeLike()) {
				// (2022-09-11) This is due to ordering issues.
				// To complete V1 mappings, we need the full MC jar.
				// On Forge, producing the full MC jar needs the list of all Forge dependencies
				//   -> needs our remapped dependency from srg to named class names (1.19+)
				//   -> needs the mappings
				//   = a circular dependency
				throw new UnsupportedOperationException("Forge cannot be used with V1 mappings!");
			}

			final List<Path> minecraftJars = minecraftProvider.getMinecraftJars();

			if (minecraftJars.size() != 1) {
				throw new UnsupportedOperationException("V1 mappings only support single jar minecraft providers");
			}

			// These are merged v1 mappings
			Files.deleteIfExists(tinyMappings);
			LOGGER.info(":populating field names");
			suggestFieldNames(minecraftJars.get(0), baseTinyMappings, tinyMappings);
		}
	}

	private void readAndMergeMCP(Project project, ServiceFactory serviceFactory, MinecraftProvider minecraftProvider, Path mcpJar) throws Exception {
		LoomGradleExtension extension = LoomGradleExtension.get(project);
		IntermediateMappingsService intermediateMappingsService = serviceFactory.get(IntermediateMappingsService.createOptions(project, minecraftProvider));
		Path intermediaryTinyPath = intermediateMappingsService.getIntermediaryTiny();
		SrgProvider provider = extension.getSrgProvider();

		if (provider == null) {
			if (!extension.shouldGenerateSrgTiny()) {
				Configuration srg = project.getConfigurations().maybeCreate(Constants.Configurations.SRG);
				srg.setTransitive(false);
			}

			provider = new SrgProvider(project);
			project.getDependencies().add(provider.getTargetConfig(), "de.oceanlabs.mcp:mcp_config:" + extension.getMinecraftProvider().minecraftVersion());
			Configuration configuration = project.getConfigurations().getByName(provider.getTargetConfig());
			provider.provide(DependencyInfo.create(project, configuration.getDependencies().iterator().next(), configuration));
		}

		Path srgPath = getRawSrgFile(project);
		TinyFile file = new MCPReader(intermediaryTinyPath, srgPath).read(mcpJar);
		TinyV2Writer.write(file, tinyMappings);
	}

	private boolean isMCP(Path path) throws IOException {
		try (FileSystemUtil.Delegate fs = FileSystemUtil.getJarFileSystem(path, false)) {
			return Files.exists(fs.getPath("fields.csv")) && Files.exists(fs.getPath("methods.csv"));
		}
	}

	private static boolean areMappingsV2(Path path) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			return MappingReader.detectFormat(reader) == MappingFormat.TINY_2_FILE;
		} catch (NoSuchFileException e) {
			// TODO: just check the mappings version when Parser supports V1 in readMetadata()
			return false;
		}
	}

	public static void extractMappings(Path jar, Path extractTo) throws IOException {
		try (FileSystemUtil.Delegate delegate = FileSystemUtil.getJarFileSystem(jar)) {
			extractMappings(delegate.fs(), extractTo);
		}
	}

	public static void extractMappings(FileSystem jar, Path extractTo) throws IOException {
		Files.copy(jar.getPath("mappings/mappings.tiny"), extractTo, StandardCopyOption.REPLACE_EXISTING);
	}

	private void extractExtras(FileSystem jar) throws IOException {
		extractUnpickDefinitions(jar);
		extractSignatureFixes(jar);
	}

	private void extractUnpickDefinitions(FileSystem jar) throws IOException {
		Path unpickPath = jar.getPath("extras/definitions.unpick");
		Path unpickMetadataPath = jar.getPath("extras/unpick.json");

		if (!Files.exists(unpickPath) || !Files.exists(unpickMetadataPath)) {
			return;
		}

		Files.copy(unpickPath, unpickDefinitions, StandardCopyOption.REPLACE_EXISTING);

		unpickMetadata = parseUnpickMetadata(unpickMetadataPath);
		hasUnpickDefinitions = true;
	}

	private void extractSignatureFixes(FileSystem jar) throws IOException {
		Path recordSignaturesJsonPath = jar.getPath("extras/record_signatures.json");

		if (!Files.exists(recordSignaturesJsonPath)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(recordSignaturesJsonPath, StandardCharsets.UTF_8)) {
			//noinspection unchecked
			signatureFixes = LoomGradlePlugin.GSON.fromJson(reader, Map.class);
		}
	}

	private UnpickMetadata parseUnpickMetadata(Path input) throws IOException {
		JsonObject jsonObject = LoomGradlePlugin.GSON.fromJson(Files.readString(input, StandardCharsets.UTF_8), JsonObject.class);

		if (!jsonObject.has("version") || jsonObject.get("version").getAsInt() != 1) {
			throw new UnsupportedOperationException("Unsupported unpick version");
		}

		return new UnpickMetadata(
				jsonObject.get("unpickGroup").getAsString(),
				jsonObject.get("unpickVersion").getAsString()
		);
	}

	private void populateUnpickClasspath(Project project) {
		String unpickCliName = "unpick-cli";
		project.getDependencies().add(Constants.Configurations.UNPICK_CLASSPATH,
				String.format("%s:%s:%s", unpickMetadata.unpickGroup, unpickCliName, unpickMetadata.unpickVersion)
		);

		// Unpick ships with a slightly older version of asm, ensure it runs with at least the same version as loom.
		String[] asmDeps = new String[] {
				"org.ow2.asm:asm:%s",
				"org.ow2.asm:asm-tree:%s",
				"org.ow2.asm:asm-commons:%s",
				"org.ow2.asm:asm-util:%s"
		};

		for (String asm : asmDeps) {
			project.getDependencies().add(Constants.Configurations.UNPICK_CLASSPATH,
					asm.formatted(Opcodes.class.getPackage().getImplementationVersion())
			);
		}
	}

	private void suggestFieldNames(Path inputJar, Path oldMappings, Path newMappings) {
		Command command = new CommandProposeFieldNames();
		runCommand(command, inputJar.toFile().getAbsolutePath(),
						oldMappings.toAbsolutePath().toString(),
						newMappings.toAbsolutePath().toString());
	}

	private void runCommand(Command command, String... args) {
		try {
			command.run(args);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void cleanWorkingDirectory(Path mappingsWorkingDir) {
		try {
			if (Files.exists(mappingsWorkingDir)) {
				Files.walkFileTree(mappingsWorkingDir, new DeletingFileVisitor());
			}

			Files.createDirectories(mappingsWorkingDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Path mappingsWorkingDir() {
		return mappingsWorkingDir;
	}

	protected static String createMappingsIdentifier(String mappingsName, String version, String classifier, String minecraftVersion) {
		//          mappingsName      . mcVersion . version        classifier
		// Example: net.fabricmc.yarn . 1_16_5    . 1.16.5+build.5 -v2
		return mappingsName + "." + minecraftVersion.replace(' ', '_').replace('.', '_').replace('-', '_') + "." + version + classifier;
	}

	protected static String createForgeMappingsIdentifier(LoomGradleExtension extension, String mappingsName, String version, String classifier, String minecraftVersion) {
		final String base = createMappingsIdentifier(mappingsName, version, classifier, minecraftVersion);
		final String platform = extension.getPlatform().get().id();
		final String forgeVersion = extension.getForgeProvider().getVersion().getCombined();
		return base + "-" + platform + "-" + forgeVersion;
	}

	public String mappingsIdentifier() {
		return mappingsIdentifier;
	}

	public File getUnpickDefinitionsFile() {
		return unpickDefinitions.toFile();
	}

	public boolean hasUnpickDefinitions() {
		return hasUnpickDefinitions;
	}

	@Nullable
	public Map<String, String> getSignatureFixes() {
		return signatureFixes;
	}

	public String getBuildServiceName(String name, String from, String to) {
		return "%s:%s:%s>%S".formatted(name, mappingsIdentifier(), from, to);
	}

	public Path getReplacedTarget(LoomGradleExtension loom, String namespace) {
		if (namespace.equals("intermediary")) return getPlatformMappingFile(loom);

		return mixinTinyMappings.computeIfAbsent(namespace, k -> {
			Path path = mappingsWorkingDir.resolve("mappings-mixin-" + namespace + ".tiny");

			try {
				if (Files.notExists(path) || loom.refreshDeps()) {
					List<String> lines = new ArrayList<>(Files.readAllLines(getPlatformMappingFile(loom)));
					lines.set(0, lines.get(0).replace("intermediary", "yraidemretni").replace(namespace, "intermediary"));
					Files.deleteIfExists(path);
					Files.write(path, lines);
				}

				return path;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	/**
	 * The mapping file that is specific to the platform settings.
	 * It contains SRG (Forge/common) or Mojang mappings (NeoForge) as needed.
	 *
	 * @return the platform mapping file path
	 */
	public Path getPlatformMappingFile(LoomGradleExtension extension) {
		if (extension.shouldGenerateSrgTiny()) {
			return tinyMappingsWithSrg;
		} else if (extension.isNeoForge()) {
			return tinyMappingsWithMojang;
		} else {
			return tinyMappings;
		}
	}

	public record UnpickMetadata(String unpickGroup, String unpickVersion) {
	}
}
