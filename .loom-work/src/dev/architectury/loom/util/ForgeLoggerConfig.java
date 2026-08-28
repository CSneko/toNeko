package dev.architectury.loom.util;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.StringJoiner;

import org.gradle.api.Project;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.util.FileSystemUtil;

public final class ForgeLoggerConfig {
	private static final List<ArtifactCoordinates> LOGGER_CONFIG_ARTIFACTS = List.of(
			// 1.17 -
			new ArtifactCoordinates("net.minecraftforge", "fmlloader", null),
			// 1.14 - 1.16
			new ArtifactCoordinates("net.minecraftforge", "forge", "launcher")
	);

	public static @Nullable File getForgeLoggerConfigSource(Project project) {
		final List<String> libraries = LoomGradleExtension.get(project)
				.getForgeUserdevProvider()
				.getConfig()
				.libraries();

		for (String library : libraries) {
			if (LOGGER_CONFIG_ARTIFACTS.stream().anyMatch(artifact -> artifact.matches(library))) {
				return project.getConfigurations()
						.detachedConfiguration(project.getDependencies().create(library))
						.setTransitive(false)
						.getSingleFile();
			}
		}

		return null;
	}

	public static void copyToPath(Path libraryFile, Path outputFile) {
		try (FileSystemUtil.Delegate fs = FileSystemUtil.getJarFileSystem(libraryFile, false)) {
			final Path configPath = fs.getPath("log4j2.xml");
			Files.copy(configPath, outputFile, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Contract("-> fail")
	public static void throwNotFound() {
		StringBuilder sb = new StringBuilder("Could not find Forge dependency with logger config. Tried to find:");

		for (ArtifactCoordinates artifact : LOGGER_CONFIG_ARTIFACTS) {
			sb.append('\n').append(" - ").append(artifact);
		}

		throw new RuntimeException(sb.toString());
	}

	private record ArtifactCoordinates(String group, String name, @Nullable String classifier) {
		boolean matches(String notation) {
			final String[] parts = notation.split(":");
			return group.equals(parts[0]) && name.equals(parts[1])
					&& (classifier == null || (parts.length >= 4 && classifier.equals(parts[3])));
		}

		@Override
		public String toString() {
			final StringJoiner joiner = new StringJoiner(":");
			joiner.add(group);
			joiner.add(name);

			if (classifier != null) {
				joiner.add(classifier);
			}

			return joiner.toString();
		}
	}
}
