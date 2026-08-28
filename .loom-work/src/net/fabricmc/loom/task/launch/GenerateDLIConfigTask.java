/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021 FabricMC
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

package net.fabricmc.loom.task.launch;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.configuration.ConsoleOutput;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.ApiStatus;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.LoomGradlePlugin;
import net.fabricmc.loom.build.IntermediaryNamespaces;
import net.fabricmc.loom.configuration.providers.forge.ConfigValue;
import net.fabricmc.loom.configuration.providers.forge.ForgeRunTemplate;
import net.fabricmc.loom.configuration.providers.forge.ForgeRunsProvider;
import net.fabricmc.loom.configuration.providers.minecraft.MinecraftVersionMeta;
import net.fabricmc.loom.configuration.providers.minecraft.mapped.MappedMinecraftProvider;
import net.fabricmc.loom.task.AbstractLoomTask;
import net.fabricmc.loom.util.Constants;
import net.fabricmc.loom.util.ModPlatform;
import net.fabricmc.loom.util.gradle.SourceSetHelper;

public abstract class GenerateDLIConfigTask extends AbstractLoomTask {
	@Input
	protected abstract Property<String> getVersionInfoJson();

	@Input
	protected abstract Property<String> getMinecraftVersion();

	@Input
	protected abstract Property<Boolean> getSplitSourceSets();

	@Input
	protected abstract Property<Boolean> getPlainConsole();

	@Input
	protected abstract Property<Boolean> getANSISupportedIDE();

	@Input
	@Optional
	protected abstract Property<String> getClassPathGroups();

	@Input
	protected abstract Property<String> getLog4jConfigPaths();

	@Input
	@Optional
	protected abstract Property<String> getClientGameJarPath();

	@Input
	@Optional
	protected abstract Property<String> getCommonGameJarPath();

	@Input
	protected abstract Property<String> getAssetsDirectoryPath();

	@Input
	protected abstract Property<String> getNativesDirectoryPath();

	@InputFile
	public abstract RegularFileProperty getRemapClasspathFile();

	@OutputFile
	protected abstract RegularFileProperty getDevLauncherConfig();

	@ApiStatus.Internal
	@Input
	@Optional
	protected abstract Property<ForgeInputs> getForgeInputs();

	@ApiStatus.Internal
	@InputFile
	protected abstract RegularFileProperty getPlatformMappingFile();

	@ApiStatus.Internal
	@InputFiles
	protected abstract ConfigurableFileCollection getMappingJars();

	@ApiStatus.Internal
	@Input
	protected abstract SetProperty<ForgeRunTemplate.Resolved> getRunTemplates();

	public GenerateDLIConfigTask() {
		getVersionInfoJson().set(LoomGradlePlugin.GSON.toJson(getExtension().getMinecraftProvider().getVersionInfo()));
		getMinecraftVersion().set(getExtension().getMinecraftProvider().minecraftVersion());
		getSplitSourceSets().set(getExtension().areEnvironmentSourceSetsSplit());
		getANSISupportedIDE().set(ansiSupportedIde(getProject()));
		getPlainConsole().set(getProject().getGradle().getStartParameter().getConsoleOutput() == ConsoleOutput.Plain);

		if (!getExtension().getMods().isEmpty()) {
			getClassPathGroups().set(buildClassPathGroups(getProject()));
		}

		getLog4jConfigPaths().set(getAllLog4JConfigFiles(getProject()));

		if (getSplitSourceSets().get()) {
			getClientGameJarPath().set(getGameJarPath("client"));
			getCommonGameJarPath().set(getGameJarPath("common"));
		}

		getAssetsDirectoryPath().set(new File(getExtension().getFiles().getUserCache(), "assets").getAbsolutePath());
		getNativesDirectoryPath().set(getExtension().getFiles().getNativesDirectory(getProject()).getAbsolutePath());
		getDevLauncherConfig().set(getExtension().getFiles().getDevLauncherConfig());

		getPlatformMappingFile().set(getProject().getLayout().file(getProject().provider(() -> getExtension().getPlatformMappingFile().toFile())));
		getPlatformMappingFile().finalizeValue();
		getMappingJars().from(getProject().getConfigurations().getByName(Constants.Configurations.MAPPINGS_FINAL));

		if (getExtension().isForgeLike()) {
			getRunTemplates().addAll(getProject().provider(() -> {
				final ForgeRunsProvider forgeRunsProvider = getExtension().getForgeRunsProvider();
				final ConfigValue.Resolver configResolver = forgeRunsProvider.getResolver(null);
				return forgeRunsProvider.getTemplates()
						.stream()
						.map(template -> template.resolve(configResolver))
						.toList();
			}));

			if (getExtension().isForge()) {
				getForgeInputs().set(getProject().provider(() -> new ForgeInputs(getProject(), getExtension())));
			}
		} else {
			getRunTemplates().empty();
		}
	}

	@TaskAction
	public void run() throws IOException {
		final MinecraftVersionMeta versionInfo = LoomGradlePlugin.GSON.fromJson(getVersionInfoJson().get(), MinecraftVersionMeta.class);
		File assetsDirectory = new File(getAssetsDirectoryPath().get());

		if (versionInfo.assets().equals("legacy")) {
			assetsDirectory = new File(assetsDirectory, "/legacy/" + versionInfo.id());
		}

		final ModPlatform platform = getModPlatform().get();
		boolean quilt = platform == ModPlatform.QUILT;
		final LaunchConfig launchConfig = new LaunchConfig()
				.property(!quilt ? "fabric.development" : "loader.development", "true")
				.property(!quilt ? "fabric.remapClasspathFile" : "loader.remapClasspathFile", getRemapClasspathFile().get().getAsFile().getAbsolutePath())
				.property("log4j.configurationFile", getLog4jConfigPaths().get())
				.property("log4j2.formatMsgNoLookups", "true");

		if (versionInfo.hasNativesToExtract()) {
			String nativesPath = getNativesDirectoryPath().get();

			launchConfig
					.property("client", "java.library.path", nativesPath)
					.property("client", "org.lwjgl.librarypath", nativesPath);
		}

		if (!platform.isForgeLike()) {
			launchConfig
					.argument("client", "--assetIndex")
					.argument("client", versionInfo.assetIndex().fabricId(getMinecraftVersion().get()))
					.argument("client", "--assetsDir")
					.argument("client", assetsDirectory.getAbsolutePath());

			if (getSplitSourceSets().get()) {
				launchConfig.property("client", !quilt ? "fabric.gameJarPath.client" : "loader.gameJarPath.client", getClientGameJarPath().get());
				launchConfig.property(!quilt ? "fabric.gameJarPath" : "loader.gameJarPath", getCommonGameJarPath().get());
			}

			if (getClassPathGroups().isPresent()) {
				launchConfig.property(!quilt ? "fabric.classPathGroups" : "loader.classPathGroups", getClassPathGroups().get());
			}
		}

		if (quilt) {
			launchConfig
					.argument("client", "--version")
					.argument("client", "Architectury Loom");
		}

		if (platform.isForgeLike()) {
			// Find the mapping files for Unprotect to use for figuring out
			// which classes are from Minecraft.
			String unprotectMappings = getMappingJars()
					.getFiles()
					.stream()
					.map(File::getAbsolutePath)
					.collect(Collectors.joining(File.pathSeparator));

			final String intermediateNs = IntermediaryNamespaces.intermediaryNamespace(platform).toString();
			final String mappingsPath = getPlatformMappingFile().get().getAsFile().getAbsolutePath();

			launchConfig
					.property("unprotect.mappings", unprotectMappings)
					// See ArchitecturyNamingService in forge-runtime
					.property("architectury.naming.sourceNamespace", intermediateNs)
					.property("architectury.naming.mappingsPath", mappingsPath);

			if (platform == ModPlatform.FORGE) {
				final ForgeInputs forgeInputs = Objects.requireNonNull(getForgeInputs().getOrNull());
				final List<String> dataGenMods = forgeInputs.dataGenMods();

				// Only apply the hardcoded data arguments if the deprecated data generator API is being used.
				if (!dataGenMods.isEmpty()) {
					launchConfig
							.argument("data", "--all")
							.argument("data", "--mod")
							.argument("data", String.join(",", dataGenMods))
							.argument("data", "--output")
							.argument("data", forgeInputs.legacyDataGenDir());
				}

				launchConfig.property("mixin.env.remapRefMap", "true");

				if (forgeInputs.useCustomMixin()) {
					// See mixin remapper service in forge-runtime
					launchConfig
							.property("architectury.mixinRemapper.sourceNamespace", intermediateNs)
							.property("architectury.mixinRemapper.mappingsPath", mappingsPath);
				} else {
					launchConfig.property("net.minecraftforge.gradle.GradleStart.srg.srg-mcp", forgeInputs.srgToNamedSrg());
				}

				Set<String> mixinConfigs = forgeInputs.mixinConfigs();

				if (!mixinConfigs.isEmpty()) {
					for (String config : mixinConfigs) {
						launchConfig.argument("-mixin.config");
						launchConfig.argument(config);
					}
				}
			}

			for (ForgeRunTemplate.Resolved template : getRunTemplates().get()) {
				// Note: lowercase to match RunConfig which lowercases all user input for
				// RunConfigSettings.environment
				var env = template.name().toLowerCase(Locale.ROOT);

				for (String argument : template.args()) {
					launchConfig.argument(env, argument);
				}

				for (Map.Entry<String, String> property : template.props().entrySet()) {
					launchConfig.property(env, property.getKey(), property.getValue());
				}
			}
		}

		//Enable ansi by default for idea and vscode when gradle is not ran with plain console.
		if (getANSISupportedIDE().get() && !getPlainConsole().get()) {
			launchConfig.property("fabric.log.disableAnsi", "false");
		}

		FileUtils.writeStringToFile(getDevLauncherConfig().getAsFile().get(), launchConfig.asString(), StandardCharsets.UTF_8);
	}

	private static String getAllLog4JConfigFiles(Project project) {
		return LoomGradleExtension.get(project).getLog4jConfigs().getFiles().stream()
				.map(File::getAbsolutePath)
				.collect(Collectors.joining(","));
	}

	private String getGameJarPath(String env) {
		MappedMinecraftProvider.Split split = (MappedMinecraftProvider.Split) getExtension().getNamedMinecraftProvider();

		return switch (env) {
		case "client" -> split.getClientOnlyJar().getPath().toAbsolutePath().toString();
		case "common" -> split.getCommonJar().getPath().toAbsolutePath().toString();
		default -> throw new UnsupportedOperationException();
		};
	}

	/**
	 * See: https://github.com/FabricMC/fabric-loader/pull/585.
	 */
	private static String buildClassPathGroups(Project project) {
		return LoomGradleExtension.get(project).getMods().stream()
				.map(modSettings ->
						SourceSetHelper.getClasspath(modSettings, project).stream()
							.map(File::getAbsolutePath)
							.collect(Collectors.joining(File.pathSeparator))
				)
				.collect(Collectors.joining(File.pathSeparator+File.pathSeparator));
	}

	private static boolean ansiSupportedIde(Project project) {
		File rootDir = project.getRootDir();
		return new File(rootDir, ".vscode").exists()
				|| new File(rootDir, ".idea").exists()
				|| new File(rootDir, ".project").exists()
				|| (Arrays.stream(rootDir.listFiles()).anyMatch(file -> file.getName().endsWith(".iws")));
	}

	public static class LaunchConfig {
		private final Map<String, List<String>> values = new HashMap<>();

		public LaunchConfig property(String key, String value) {
			return property("common", key, value);
		}

		public LaunchConfig property(String side, String key, String value) {
			values.computeIfAbsent(side + "Properties", (s -> new ArrayList<>()))
					.add(String.format("%s=%s", key, value));
			return this;
		}

		public LaunchConfig argument(String value) {
			return argument("common", value);
		}

		public LaunchConfig argument(String side, String value) {
			values.computeIfAbsent(side + "Args", (s -> new ArrayList<>()))
					.add(value);
			return this;
		}

		public String asString() {
			StringJoiner stringJoiner = new StringJoiner("\n");

			for (Map.Entry<String, List<String>> entry : values.entrySet()) {
				stringJoiner.add(entry.getKey());

				for (String s : entry.getValue()) {
					stringJoiner.add("\t" + s);
				}
			}

			return stringJoiner.toString();
		}
	}

	@ApiStatus.Internal
	public record ForgeInputs(
			List<String> dataGenMods,
			String legacyDataGenDir,
			Set<String> mixinConfigs,
			boolean useCustomMixin,
			String srgToNamedSrg
	) implements Serializable {
		public ForgeInputs(Project project, LoomGradleExtension extension) {
			this(
					extension.getForge().getDataGenMods(),
					project.file("src/generated/resources").getAbsolutePath(),
					extension.getForge().getMixinConfigs().get(),
					extension.getForge().getUseCustomMixin().get(),
					extension.getMappingConfiguration().srgToNamedSrg.toAbsolutePath().toString()
			);
		}
	}
}
