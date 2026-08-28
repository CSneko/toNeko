/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022-2024 FabricMC
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.common.hash.Hashing;
import dev.architectury.loom.forge.ModDirTransformerDiscovererPatch;
import dev.architectury.loom.neoforge.LaunchHandlerPatcher;
import dev.architectury.loom.util.ClassVisitorUtil;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.api.mappings.layered.MappingContext;
import net.fabricmc.loom.api.mappings.layered.MappingsNamespace;
import net.fabricmc.loom.configuration.mods.ModConfigurationRemapper;
import net.fabricmc.loom.configuration.mods.dependency.LocalMavenHelper;
import net.fabricmc.loom.configuration.providers.mappings.GradleMappingContext;
import net.fabricmc.loom.configuration.providers.mappings.MappingConfiguration;
import net.fabricmc.loom.util.Constants;
import net.fabricmc.loom.util.ExceptionUtil;
import net.fabricmc.loom.util.FileSystemUtil;
import net.fabricmc.loom.util.PropertyUtil;
import net.fabricmc.loom.util.srg.RemapObjectHolderVisitor;
import net.fabricmc.loom.util.srg.ForgeMappingsMerger;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public class ForgeLibrariesProvider {
	private static final String FML_LOADER_GROUP = "net.minecraftforge";
	private static final String FML_LOADER_NAME = "fmlloader";
	private static final String FANCYML_LOADER_GROUP = "net.neoforged.fancymodloader";
	private static final String FANCYML_LOADER_NAME = "loader";

	private static final String FORGE_OBJECT_HOLDER_FILE = "net/minecraftforge/fml/common/asm/ObjectHolderDefinalize.class";
	private static final String FORGE_MOD_DIR_TRANSFORMER_DISCOVERER_FILE = "net/minecraftforge/fml/loading/ModDirTransformerDiscoverer.class";
	private static final String NEOFORGE_OBJECT_HOLDER_FILE = "net/neoforged/fml/common/asm/ObjectHolderDefinalize.class";
	private static final String NEOFORGE_LAUNCH_HANDLER_FILE = "net/neoforged/fml/loading/targets/CommonUserdevLaunchHandler.class";

	public static void provide(MappingConfiguration mappingConfiguration, Project project) throws Exception {
		LoomGradleExtension extension = LoomGradleExtension.get(project);
		final List<Dependency> dependencies = new ArrayList<>();

		// Collect all dependencies with possible relocations, such as Mixin.
		for (String lib : extension.getForgeUserdevProvider().getConfig().libraries()) {
			String dep = null;

			if (lib.startsWith("org.spongepowered:mixin:")) {
				// Don't apply custom mixin on NeoForge.
				if (extension.isForge() && PropertyUtil.getAndFinalize(extension.getForge().getUseCustomMixin())) {
					if (lib.contains("0.8.2")) {
						dep = "net.fabricmc:sponge-mixin:0.8.2+build.24";
					} else {
						String version = lib.substring(lib.lastIndexOf(":"));
						// Used for the file extension, for example @jar
						int atIndex = version.indexOf('@');

						if (atIndex >= 0) {
							// Strip the file extension away
							version = version.substring(0, atIndex);
						}

						dep = "dev.architectury:mixin-patched" + version + ".+";
					}
				}
			}

			if (lib.startsWith("net.minecraftforge:bootstrap:")) {
				if (extension.isForge() && extension.getForgeProvider().getVersion().getMajorVersion() >= Constants.Forge.MIN_BOOTSTRAP_DEV_VERSION) {
					String version = lib.substring(lib.lastIndexOf(":"));
					dependencies.add(project.getDependencies().create("net.minecraftforge:bootstrap-dev" + version));
				}
			}

			if (dep == null) {
				dep = lib;
			}

			dependencies.add(project.getDependencies().create(dep));
		}

		// Resolve all files. We just add the dependencies manually unless it's FML.
		// We're transforming the files manually instead of using Gradle's mechanism because
		// we can target the individual files to be transformed instead of creating new copies of all the libraries.
		final ResolvedConfiguration config = project.getConfigurations()
				.detachedConfiguration(dependencies.toArray(new Dependency[0]))
				.getResolvedConfiguration();

		for (ResolvedArtifact artifact : config.getResolvedArtifacts()) {
			final ModuleVersionIdentifier id = artifact.getModuleVersion().getId();
			final Object dep;
			final boolean isFML = FML_LOADER_GROUP.equals(id.getGroup()) && FML_LOADER_NAME.equals(id.getName());
			final boolean isFancyML = FANCYML_LOADER_GROUP.equals(id.getGroup()) && FANCYML_LOADER_NAME.equals(id.getName());

			if (isFML || isFancyML) {
				// If FML, remap it.
				try {
					if (isFML) {
						project.getLogger().info(":remapping FML loader");
					} else if (isFancyML) {
						project.getLogger().info(":remapping FancyML loader");
					}

					dep = remapFmlLoader(project, artifact, mappingConfiguration);
				} catch (IOException e) {
					throw ExceptionUtil.createDescriptiveWrapper(RuntimeException::new, "Could not remap FML", e);
				}
			} else {
				dep = project.getDependencies().create(getDependencyNotation(artifact));

				if (dep instanceof ModuleDependency md) {
					// We've already resolved the transitive deps, and we don't want both a transformed one
					// and an untransformed one on the classpath.
					md.setTransitive(false);
				}
			}

			DependencyProvider.addDependency(project, dep, Constants.Configurations.FORGE_DEPENDENCIES);
		}
	}

	// Returns a Gradle dependency notation.
	private static Object remapFmlLoader(Project project, ResolvedArtifact artifact, MappingConfiguration mappingConfiguration) throws IOException {
		final LoomGradleExtension extension = LoomGradleExtension.get(project);

		// A hash of the current mapping configuration. The transformations only need to be done once per mapping set.
		// While the mappings ID is definitely valid in file names, splitting MC versions parts into nested directories
		// isn't good.
		final String mappingHash = Hashing.sha256()
				.hashString(mappingConfiguration.mappingsIdentifier(), StandardCharsets.UTF_8)
				.toString();

		// Resolve the inputs and outputs.
		final ModuleVersionIdentifier id = artifact.getModuleVersion().getId();
		final LocalMavenHelper mavenHelper = new LocalMavenHelper(
				id.getGroup() + "." + mappingHash,
				id.getName(),
				id.getVersion(),
				artifact.getClassifier(),
				extension.getFiles().getForgeDependencyRepo().toPath()
		);
		final Path inputJar = artifact.getFile().toPath();
		final Path outputJar = mavenHelper.getOutputFile(null);

		// Modify jar.
		if (!Files.exists(outputJar) || extension.refreshDeps()) {
			mavenHelper.copyToMaven(inputJar, null);

			try (FileSystemUtil.Delegate fs = FileSystemUtil.getJarFileSystem(outputJar, false)) {
				Path path = fs.get().getPath("META-INF/services/cpw.mods.modlauncher.api.INameMappingService");
				Files.deleteIfExists(path);

				if (Files.exists(fs.get().getPath(FORGE_OBJECT_HOLDER_FILE))) {
					remapObjectHolder(project, outputJar, mappingConfiguration);
				}

				if (Files.exists(fs.getPath(FORGE_MOD_DIR_TRANSFORMER_DISCOVERER_FILE))) {
					ClassVisitorUtil.rewriteClassFile(fs.getPath(FORGE_MOD_DIR_TRANSFORMER_DISCOVERER_FILE), ModDirTransformerDiscovererPatch::new);
				}

				if (Files.exists(fs.getPath(NEOFORGE_OBJECT_HOLDER_FILE))) {
					remapNeoForgeObjectHolder(project, outputJar, mappingConfiguration);
				}

				if (Files.exists(fs.getPath(NEOFORGE_LAUNCH_HANDLER_FILE))) {
					ClassVisitorUtil.rewriteClassFile(fs.getPath(NEOFORGE_LAUNCH_HANDLER_FILE), LaunchHandlerPatcher::new);
				}
			}

			// Copy sources when not running under CI.
			if (!ModConfigurationRemapper.isCIBuild()) {
				final Path sourcesJar = ModConfigurationRemapper.findSources(project, artifact);

				if (sourcesJar != null) {
					mavenHelper.copyToMaven(sourcesJar, "sources");
				}
			}
		}

		return mavenHelper.getNotation();
	}

	private static void remapObjectHolder(Project project, Path outputJar, MappingConfiguration mappingConfiguration) throws IOException {
		try {
			// Merge SRG mappings. The real SRG mapping file hasn't been created yet since the usual SRG merging
			// process occurs after all Forge libraries have been provided.
			// Forge libs are needed for MC, which is needed for the mappings.
			final ForgeMappingsMerger.ExtraMappings extraMappings = ForgeMappingsMerger.ExtraMappings.ofMojmapTsrg(MappingConfiguration.getMojmapSrgFileIfPossible(project));
			final MemoryMappingTree mappings = ForgeMappingsMerger.mergeSrg(MappingConfiguration.getRawSrgFile(project), mappingConfiguration.tinyMappings, extraMappings, true);

			// Remap the object holders.
			RemapObjectHolderVisitor.remapObjectHolder(
					outputJar, "net.minecraftforge.fml.common.asm.ObjectHolderDefinalize", mappings,
					MappingsNamespace.SRG.toString(), MappingsNamespace.NAMED.toString()
			);
		} catch (IOException e) {
			throw new IOException("Could not remap object holders in " + outputJar, e);
		}
	}

	private static void remapNeoForgeObjectHolder(Project project, Path outputJar, MappingConfiguration mappingConfiguration) throws IOException {
		try {
			// Merge Mojang mappings. The real Mojang mapping file hasn't been created yet since the usual Mojang merging
			// process occurs after all Forge libraries have been provided.
			// Forge libs are needed for MC, which is needed for the mappings.
			final MappingContext context = new GradleMappingContext(project, "tmp-neoforge-libs");
			final MemoryMappingTree mappings = ForgeMappingsMerger.mergeMojang(context, mappingConfiguration.tinyMappings, null, true);

			// Remap the object holders.
			RemapObjectHolderVisitor.remapObjectHolder(
					outputJar, "net.neoforged.fml.common.asm.ObjectHolderDefinalize", mappings,
					MappingsNamespace.MOJANG.toString(), MappingsNamespace.NAMED.toString()
			);
		} catch (IOException e) {
			throw new IOException("Could not remap object holders in " + outputJar, e);
		}
	}

	/**
	 * Reconstructs the dependency notation of a resolved artifact.
	 * @param artifact the artifact
	 * @return the notation
	 */
	private static String getDependencyNotation(ResolvedArtifact artifact) {
		final ModuleVersionIdentifier id = artifact.getModuleVersion().getId();
		String notation = "%s:%s:%s".formatted(id.getGroup(), id.getName(), id.getVersion());

		if (artifact.getClassifier() != null) {
			notation += ":" + artifact.getClassifier();
		}

		return notation;
	}
}
