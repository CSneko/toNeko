/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2020-2024 FabricMC
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

package net.fabricmc.loom.configuration.providers.forge;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import de.oceanlabs.mcp.mcinjector.adaptors.ParameterAnnotationFixer;
import dev.architectury.loom.forge.UserdevConfig;
import dev.architectury.loom.forge.tool.ForgeToolValueSource;
import dev.architectury.loom.util.MappingOption;
import dev.architectury.loom.util.TempFiles;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.build.IntermediaryNamespaces;
import net.fabricmc.loom.configuration.accesstransformer.AccessTransformerJarProcessor;
import net.fabricmc.loom.configuration.providers.forge.mcpconfig.McpConfigProvider;
import net.fabricmc.loom.configuration.providers.forge.mcpconfig.McpExecutor;
import net.fabricmc.loom.configuration.providers.forge.minecraft.ForgeMinecraftProvider;
import net.fabricmc.loom.configuration.providers.mappings.TinyMappingsService;
import net.fabricmc.loom.configuration.providers.minecraft.MinecraftProvider;
import net.fabricmc.loom.util.Constants;
import net.fabricmc.loom.util.DependencyDownloader;
import net.fabricmc.loom.util.FileSystemUtil;
import net.fabricmc.loom.util.ThreadingUtils;
import net.fabricmc.loom.util.TinyRemapperHelper;
import net.fabricmc.loom.util.ZipUtils;
import net.fabricmc.loom.util.function.FsPathConsumer;
import net.fabricmc.loom.util.service.ServiceFactory;
import net.fabricmc.loom.util.srg.CoreModClassRemapper;
import net.fabricmc.loom.util.srg.InnerClassRemapper;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.extension.mixin.MixinExtension;

public class MinecraftPatchedProvider {
	private static final String LOOM_PATCH_VERSION_KEY = "Loom-Patch-Version";
	private static final String CURRENT_LOOM_PATCH_VERSION = "9";
	private static final String NAME_MAPPING_SERVICE_PATH = "/inject/META-INF/services/cpw.mods.modlauncher.api.INameMappingService";

	private final Project project;
	private final Logger logger;
	private final MinecraftProvider minecraftProvider;
	private final Type type;

	// Step 1: Remap Minecraft to intermediate mappings, merge if needed
	private Path minecraftIntermediateJar;
	// Step 2: Binary Patch
	private Path minecraftPatchedIntermediateJar;
	// Step 3: Access Transform
	private Path minecraftPatchedIntermediateAtJar;
	// Step 4: Remap Patched AT & Forge to official
	private Path minecraftPatchedJar;
	private Path minecraftClientExtra;

	private boolean dirty = false;

	public static MinecraftPatchedProvider get(Project project) {
		MinecraftProvider provider = LoomGradleExtension.get(project).getMinecraftProvider();

		if (provider instanceof ForgeMinecraftProvider patched) {
			return patched.getPatchedProvider();
		} else {
			throw new UnsupportedOperationException("Project " + project.getPath() + " does not use MinecraftPatchedProvider!");
		}
	}

	public MinecraftPatchedProvider(Project project, MinecraftProvider minecraftProvider, Type type) {
		this.project = project;
		this.logger = project.getLogger();
		this.minecraftProvider = minecraftProvider;
		this.type = type;
	}

	private LoomGradleExtension getExtension() {
		return LoomGradleExtension.get(project);
	}

	private void initPatchedFiles() {
		String forgeVersion = getExtension().getForgeProvider().getVersion().getCombined();
		Path forgeWorkingDir = ForgeProvider.getForgeCache(project);
		// Note: strings used instead of platform id since FML requires one of these exact strings
		// depending on the loader to recognise Minecraft.
		String patchId = (getExtension().isNeoForge() ? "neoforge" : "forge") + "-" + forgeVersion + "-";

		minecraftProvider.setJarPrefix(patchId);

		final String intermediateId = getExtension().isNeoForge() ? "mojang" : "srg";
		minecraftIntermediateJar = forgeWorkingDir.resolve("minecraft-" + type.id + "-" + intermediateId + ".jar");
		minecraftPatchedIntermediateJar = forgeWorkingDir.resolve("minecraft-" + type.id + "-" + intermediateId + "-patched.jar");
		minecraftPatchedIntermediateAtJar = forgeWorkingDir.resolve("minecraft-" + type.id + "-" + intermediateId + "-at-patched.jar");
		minecraftPatchedJar = forgeWorkingDir.resolve("minecraft-" + type.id + "-patched.jar");
		minecraftClientExtra = forgeWorkingDir.resolve("client-extra.jar");
	}

	private void cleanAllCache() throws IOException {
		for (Path path : getGlobalCaches()) {
			Files.deleteIfExists(path);
		}
	}

	private Path[] getGlobalCaches() {
		Path[] files = {
				minecraftIntermediateJar,
				minecraftPatchedIntermediateJar,
				minecraftPatchedIntermediateAtJar,
				minecraftPatchedJar,
				minecraftClientExtra,
		};

		return files;
	}

	private void checkCache() throws IOException {
		if (getExtension().refreshDeps() || Stream.of(getGlobalCaches()).anyMatch(Files::notExists)
				|| !isPatchedJarUpToDate(minecraftPatchedJar)) {
			cleanAllCache();
		}
	}

	public void provide() throws Exception {
		initPatchedFiles();
		checkCache();

		this.dirty = false;

		if (Files.notExists(minecraftIntermediateJar)) {
			this.dirty = true;

			try (var tempFiles = new TempFiles()) {
				McpExecutor executor = createMcpExecutor(tempFiles.directory("loom-mcp"));
				Path output = executor.enqueue("rename").execute();
				Files.copy(output, minecraftIntermediateJar);
			}
		}

		if (dirty || Files.notExists(minecraftPatchedIntermediateJar)) {
			this.dirty = true;
			patchJars();
		}

		if (dirty || Files.notExists(minecraftPatchedIntermediateAtJar)) {
			this.dirty = true;
			accessTransformForge();
		}
	}

	public void remapJar(ServiceFactory serviceFactory) throws Exception {
		if (dirty) {
			remapPatchedJar(serviceFactory);
			fillClientExtraJar();
		}

		DependencyProvider.addDependency(project, minecraftClientExtra, Constants.Configurations.FORGE_EXTRA);
	}

	private void fillClientExtraJar() throws IOException {
		Files.deleteIfExists(minecraftClientExtra);
		FileSystemUtil.getJarFileSystem(minecraftClientExtra, true).close();

		copyNonClassFiles(minecraftProvider.getMinecraftClientJar().toPath(), minecraftClientExtra);
	}

	private TinyRemapper buildRemapper(ServiceFactory serviceFactory, Path input) throws IOException {
		final MappingOption mappingOption = MappingOption.forPlatform(getExtension());
		TinyMappingsService mappingsService = getExtension().getMappingConfiguration().getMappingsService(project, serviceFactory, mappingOption);
		final String sourceNamespace = IntermediaryNamespaces.intermediary(project);
		MemoryMappingTree mappings = mappingsService.getMappingTree();

		TinyRemapper.Builder builder = TinyRemapper.newRemapper()
				.withMappings(TinyRemapperHelper.create(mappings, sourceNamespace, "official", true))
				.withMappings(InnerClassRemapper.of(InnerClassRemapper.readClassNames(input), mappings, sourceNamespace, "official"))
				.renameInvalidLocals(true)
				.rebuildSourceFilenames(true);

		if (getExtension().isNeoForge()) {
			builder.extension(new MixinExtension(inputTag -> true));
		}

		return builder.build();
	}

	private void fixParameterAnnotation(Path jarFile) throws Exception {
		logger.info(":fixing parameter annotations for " + jarFile.toAbsolutePath());
		Stopwatch stopwatch = Stopwatch.createStarted();

		try (FileSystemUtil.Delegate fs = FileSystemUtil.getJarFileSystem(jarFile, false)) {
			ThreadingUtils.TaskCompleter completer = ThreadingUtils.taskCompleter();

			for (Path file : (Iterable<? extends Path>) Files.walk(fs.getPath("/"))::iterator) {
				if (!file.toString().endsWith(".class")) continue;

				completer.add(() -> {
					byte[] bytes = Files.readAllBytes(file);
					ClassReader reader = new ClassReader(bytes);
					ClassNode node = new ClassNode();
					ClassVisitor visitor = new ParameterAnnotationFixer(node, null);
					reader.accept(visitor, 0);

					ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
					node.accept(writer);
					byte[] out = writer.toByteArray();

					if (!Arrays.equals(bytes, out)) {
						Files.delete(file);
						Files.write(file, out);
					}
				});
			}

			completer.complete();
		}

		logger.info(":fixed parameter annotations for " + jarFile.toAbsolutePath() + " in " + stopwatch);
	}

	private void deleteParameterNames(Path jarFile) throws Exception {
		logger.info(":deleting parameter names for " + jarFile.toAbsolutePath());
		Stopwatch stopwatch = Stopwatch.createStarted();

		try (FileSystemUtil.Delegate fs = FileSystemUtil.getJarFileSystem(jarFile, false)) {
			ThreadingUtils.TaskCompleter completer = ThreadingUtils.taskCompleter();
			Pattern vignetteParameters = Pattern.compile("p_[0-9a-zA-Z]+_(?:[0-9a-zA-Z]+_)?");

			for (Path file : (Iterable<? extends Path>) Files.walk(fs.getPath("/"))::iterator) {
				if (!file.toString().endsWith(".class")) continue;

				completer.add(() -> {
					byte[] bytes = Files.readAllBytes(file);
					ClassReader reader = new ClassReader(bytes);
					ClassWriter writer = new ClassWriter(0);

					reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
						@Override
						public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
							return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
								@Override
								public void visitParameter(String name, int access) {
									if (name != null && vignetteParameters.matcher(name).matches()) {
										super.visitParameter(null, access);
									} else {
										super.visitParameter(name, access);
									}
								}

								@Override
								public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
									if (!vignetteParameters.matcher(name).matches()) {
										super.visitLocalVariable(name, descriptor, signature, start, end, index);
									}
								}
							};
						}
					}, 0);

					byte[] out = writer.toByteArray();

					if (!Arrays.equals(bytes, out)) {
						Files.delete(file);
						Files.write(file, out);
					}
				});
			}

			completer.complete();
		}

		logger.info(":deleted parameter names for " + jarFile.toAbsolutePath() + " in " + stopwatch);
	}

	private File getForgeJar() {
		return getExtension().getForgeUniversalProvider().getForge();
	}

	private File getForgeUserdevJar() {
		return getExtension().getForgeUserdevProvider().getUserdevJar();
	}

	private boolean isPatchedJarUpToDate(Path jar) throws IOException {
		if (Files.notExists(jar)) return false;

		byte[] manifestBytes = ZipUtils.unpackNullable(jar, "META-INF/MANIFEST.MF");

		if (manifestBytes == null) {
			return false;
		}

		Manifest manifest = new Manifest(new ByteArrayInputStream(manifestBytes));
		Attributes attributes = manifest.getMainAttributes();
		String value = attributes.getValue(LOOM_PATCH_VERSION_KEY);

		if (Objects.equals(value, CURRENT_LOOM_PATCH_VERSION)) {
			return true;
		} else {
			logger.lifecycle(":forge patched jars not up to date. current version: " + value);
			return false;
		}
	}

	private void accessTransformForge() throws IOException {
		Path input = minecraftPatchedIntermediateJar;
		Path target = minecraftPatchedIntermediateAtJar;
		accessTransform(project, input, target);
	}

	public static void accessTransform(Project project, Path input, Path target) throws IOException {
		Stopwatch stopwatch = Stopwatch.createStarted();

		project.getLogger().lifecycle(":access transforming minecraft");

		LoomGradleExtension extension = LoomGradleExtension.get(project);
		Path userdevJar = extension.getForgeUserdevProvider().getUserdevJar().toPath();
		Files.deleteIfExists(target);

		try (var tempFiles = new TempFiles()) {
			AccessTransformerJarProcessor.executeAt(project, input, target, args -> {
				for (String atFile : extractAccessTransformers(userdevJar, extension.getForgeUserdevProvider().getConfig().ats(), tempFiles)) {
					args.add("--atFile");
					args.add(atFile);
				}
			});
		}

		project.getLogger().lifecycle(":access transformed minecraft in " + stopwatch.stop());
	}

	private static List<String> extractAccessTransformers(Path jar, UserdevConfig.AccessTransformerLocation location, TempFiles tempFiles) throws IOException {
		final List<String> extracted = new ArrayList<>();

		try (FileSystemUtil.Delegate fs = FileSystemUtil.getJarFileSystem(jar)) {
			for (Path atFile : getAccessTransformerPaths(fs, location)) {
				byte[] atBytes;

				try {
					atBytes = Files.readAllBytes(atFile);
				} catch (NoSuchFileException e) {
					continue;
				}

				Path tmpFile = tempFiles.file("at-conf", ".cfg");
				Files.write(tmpFile, atBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				extracted.add(tmpFile.toAbsolutePath().toString());
			}
		}

		return extracted;
	}

	private static List<Path> getAccessTransformerPaths(FileSystemUtil.Delegate fs, UserdevConfig.AccessTransformerLocation location) throws IOException {
		return location.visitIo(directory -> {
			Path dirPath = fs.getPath(directory);

			try (Stream<Path> paths = Files.list(dirPath)) {
				return paths.toList();
			}
		}, paths -> paths.stream().map(fs::getPath).toList());
	}

	private void remapPatchedJar(ServiceFactory serviceFactory) throws Exception {
		logger.lifecycle(":remapping minecraft (TinyRemapper, srg -> official)");
		Path mcInput = minecraftPatchedIntermediateAtJar;
		Path mcOutput = minecraftPatchedJar;
		Path forgeJar = getForgeJar().toPath();
		Path forgeUserdevJar = getForgeUserdevJar().toPath();
		Files.deleteIfExists(mcOutput);

		TinyRemapper remapper = buildRemapper(serviceFactory, mcInput);

		try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(mcOutput).build()) {
			outputConsumer.addNonClassFiles(mcInput);
			outputConsumer.addNonClassFiles(forgeJar, NonClassCopyMode.FIX_META_INF, remapper);

			InputTag mcTag = remapper.createInputTag();
			InputTag forgeTag = remapper.createInputTag();
			List<CompletableFuture<?>> futures = new ArrayList<>();
			futures.add(remapper.readInputsAsync(mcTag, mcInput));
			futures.add(remapper.readInputsAsync(forgeTag, forgeJar, forgeUserdevJar));
			CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
			remapper.apply(outputConsumer, mcTag);
			remapper.apply(outputConsumer, forgeTag);
		} finally {
			remapper.finish();
		}

		copyUserdevFiles(forgeUserdevJar, mcOutput);
		remapCoreMods(mcOutput, serviceFactory);
		applyLoomPatchVersion(mcOutput);
	}

	private void remapCoreMods(Path patchedJar, ServiceFactory serviceFactory) throws Exception {
		final MappingOption mappingOption = MappingOption.forPlatform(getExtension());
		final TinyMappingsService mappingsService = getExtension().getMappingConfiguration().getMappingsService(project, serviceFactory, mappingOption);
		final MappingTree mappings = mappingsService.getMappingTree();
		CoreModClassRemapper.remapJar(project, getExtension().getPlatform().get(), patchedJar, mappings);
	}

	private void patchJars() throws Exception {
		Stopwatch stopwatch = Stopwatch.createStarted();
		logger.lifecycle(":patching jars");
		patchJars(minecraftIntermediateJar, minecraftPatchedIntermediateJar, type.patches.apply(getExtension().getPatchProvider(), getExtension().getForgeUserdevProvider()));

		copyMissingClasses(minecraftIntermediateJar, minecraftPatchedIntermediateJar);
		deleteParameterNames(minecraftPatchedIntermediateJar);

		if (getExtension().isForgeLikeAndNotOfficial()) {
			fixParameterAnnotation(minecraftPatchedIntermediateJar);
		}

		logger.lifecycle(":patched jars in " + stopwatch.stop());
	}

	private void patchJars(Path clean, Path output, Path patches) {
		ForgeToolValueSource.exec(project, spec -> {
			UserdevConfig.BinaryPatcherConfig config = getExtension().getForgeUserdevProvider().getConfig().binpatcher();
			final FileCollection download = DependencyDownloader.download(project, config.dependency());
			spec.classpath(download);
			spec.getMainClass().set(getMainClass(download));

			for (String arg : config.args()) {
				String actual = switch (arg) {
				case "{clean}" -> clean.toAbsolutePath().toString();
				case "{output}" -> output.toAbsolutePath().toString();
				case "{patch}" -> patches.toAbsolutePath().toString();
				default -> arg;
				};
				spec.args(actual);
			}
		});
	}

	private static String getMainClass(final Iterable<File> files) {
		String mainClass = null;
		IOException ex = null;

		for (File file : files) {
			if (file.getName().endsWith(".jar")) {
				try (FileSystemUtil.Delegate fs = FileSystemUtil.getJarFileSystem(file.toPath())) {
					final Path mfPath = fs.getPath("META-INF/MANIFEST.MF");

					if (Files.exists(mfPath)) {
						try (InputStream in = Files.newInputStream(mfPath)) {
							mainClass = new Manifest(in).getMainAttributes().getValue("Main-Class");
						}
					}
				} catch (final IOException e) {
					if (ex == null) {
						ex = e;
					} else {
						ex.addSuppressed(e);
					}
				}

				if (mainClass != null) {
					break;
				}
			}
		}

		if (mainClass == null) {
			if (ex != null) {
				throw new UncheckedIOException(ex);
			} else {
				throw new RuntimeException("Failed to find main class");
			}
		}

		return mainClass;
	}

	private void walkFileSystems(Path source, Path target, Predicate<Path> filter, Function<FileSystem, Iterable<Path>> toWalk, FsPathConsumer action)
			throws IOException {
		try (FileSystemUtil.Delegate sourceFs = FileSystemUtil.getJarFileSystem(source, false);
				FileSystemUtil.Delegate targetFs = FileSystemUtil.getJarFileSystem(target, false)) {
			for (Path sourceDir : toWalk.apply(sourceFs.get())) {
				Path dir = sourceDir.toAbsolutePath();
				if (!Files.exists(dir)) continue;
				Files.walk(dir)
						.filter(Files::isRegularFile)
						.filter(filter)
						.forEach(it -> {
							boolean root = dir.getParent() == null;

							try {
								Path relativeSource = root ? it : dir.relativize(it);
								Path targetPath = targetFs.get().getPath(relativeSource.toString());
								action.accept(sourceFs.get(), targetFs.get(), it, targetPath);
							} catch (IOException e) {
								throw new UncheckedIOException(e);
							}
						});
			}
		}
	}

	private void walkFileSystems(Path source, Path target, Predicate<Path> filter, FsPathConsumer action) throws IOException {
		walkFileSystems(source, target, filter, FileSystem::getRootDirectories, action);
	}

	private void copyMissingClasses(Path source, Path target) throws IOException {
		walkFileSystems(source, target, it -> it.toString().endsWith(".class"), (sourceFs, targetFs, sourcePath, targetPath) -> {
			if (Files.exists(targetPath)) return;
			Path parent = targetPath.getParent();

			if (parent != null) {
				Files.createDirectories(parent);
			}

			Files.copy(sourcePath, targetPath);
		});
	}

	private void copyNonClassFiles(Path source, Path target) throws IOException {
		Predicate<Path> filter = file -> {
			String s = file.toString();
			return !s.endsWith(".class") && !s.startsWith("/META-INF");
		};

		walkFileSystems(source, target, filter, this::copyReplacing);
	}

	private void copyReplacing(FileSystem sourceFs, FileSystem targetFs, Path sourcePath, Path targetPath) throws IOException {
		Path parent = targetPath.getParent();

		if (parent != null) {
			Files.createDirectories(parent);
		}

		Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
	}

	private void copyUserdevFiles(Path source, Path target) throws IOException {
		// Removes the Forge name mapping service definition so that our own is used.
		// If there are multiple name mapping services with the same "understanding" pair
		// (source -> target namespace pair), modlauncher throws a fit and will crash.
		// To use our YarnNamingService instead of MCPNamingService, we have to remove this file.
		Predicate<Path> filter = file -> !file.toString().endsWith(".class") && !file.toString().equals(NAME_MAPPING_SERVICE_PATH);

		walkFileSystems(source, target, filter, fs -> Collections.singleton(fs.getPath("inject")), (sourceFs, targetFs, sourcePath, targetPath) -> {
			Path parent = targetPath.getParent();

			if (parent != null) {
				Files.createDirectories(parent);
			}

			Files.copy(sourcePath, targetPath);
		});
	}

	public void applyLoomPatchVersion(Path target) throws IOException {
		try (FileSystemUtil.Delegate delegate = FileSystemUtil.getJarFileSystem(target, false)) {
			Path manifestPath = delegate.get().getPath("META-INF/MANIFEST.MF");

			Preconditions.checkArgument(Files.exists(manifestPath), "META-INF/MANIFEST.MF does not exist in patched srg jar!");
			Manifest manifest = new Manifest();

			if (Files.exists(manifestPath)) {
				try (InputStream stream = Files.newInputStream(manifestPath)) {
					manifest.read(stream);
					manifest.getMainAttributes().putValue(LOOM_PATCH_VERSION_KEY, CURRENT_LOOM_PATCH_VERSION);
				}
			}

			try (OutputStream stream = Files.newOutputStream(manifestPath, StandardOpenOption.CREATE)) {
				manifest.write(stream);
			}
		}
	}

	public McpExecutor createMcpExecutor(Path cache) {
		McpConfigProvider provider = getExtension().getMcpConfigProvider();
		return new McpExecutor(project, minecraftProvider, cache, provider, type.mcpId);
	}

	public Path getMinecraftIntermediateJar() {
		return minecraftIntermediateJar;
	}

	public Path getMinecraftPatchedIntermediateJar() {
		return minecraftPatchedIntermediateJar;
	}

	public Path getMinecraftPatchedJar() {
		return minecraftPatchedJar;
	}

	/**
	 * Checks whether the provider's state is dirty (regenerating jars).
	 */
	public boolean isDirty() {
		return dirty;
	}

	public enum Type {
		CLIENT_ONLY("client", "client", (patch, userdev) -> patch.clientPatches),
		SERVER_ONLY("server", "server", (patch, userdev) -> patch.serverPatches),
		MERGED("merged", "joined", (patch, userdev) -> userdev.joinedPatches);

		private final String id;
		private final String mcpId;
		private final BiFunction<PatchProvider, ForgeUserdevProvider, Path> patches;

		Type(String id, String mcpId, BiFunction<PatchProvider, ForgeUserdevProvider, Path> patches) {
			this.id = id;
			this.mcpId = mcpId;
			this.patches = patches;
		}
	}
}
