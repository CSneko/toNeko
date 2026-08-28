package dev.architectury.loom.extensions;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import dev.architectury.at.AccessTransformSet;
import dev.architectury.at.io.AccessTransformFormats;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.loom.task.service.MappingsService;
import net.fabricmc.loom.util.Constants;
import net.fabricmc.loom.util.FileSystemUtil;
import net.fabricmc.loom.util.LfWriter;
import net.fabricmc.loom.util.aw2at.Aw2At;
import net.fabricmc.loom.util.service.ServiceFactory;

public final class ModBuildExtensions {
	public static Set<String> readMixinConfigsFromManifest(File jarFile) {
		try (JarFile jar = new JarFile(jarFile)) {
			@Nullable Manifest manifest = jar.getManifest();

			if (manifest != null) {
				Attributes attributes = manifest.getMainAttributes();
				String mixinConfigs = attributes.getValue(Constants.Forge.MIXIN_CONFIGS_MANIFEST_KEY);

				if (mixinConfigs != null) {
					return Set.of(mixinConfigs.split(","));
				}
			}

			return Set.of();
		} catch (IOException e) {
			throw new UncheckedIOException("Could not read mixin configs from jar " + jarFile.getAbsolutePath(), e);
		}
	}

	public static void convertAwToAt(ServiceFactory serviceFactory, Set<String> atAccessWideners, Path outputFile, Provider<MappingsService.Options> options) throws IOException {
		if (atAccessWideners.isEmpty()) {
			return;
		}

		AccessTransformSet at = AccessTransformSet.create();

		try (FileSystemUtil.Delegate fileSystem = FileSystemUtil.getJarFileSystem(outputFile, false)) {
			FileSystem fs = fileSystem.get();
			Path atPath = fs.getPath(Constants.Forge.ACCESS_TRANSFORMER_PATH);

			if (Files.exists(atPath)) {
				throw new FileAlreadyExistsException("Jar " + outputFile + " already contains an access transformer - cannot convert AWs!");
			}

			for (String aw : atAccessWideners) {
				Path awPath = fs.getPath(aw);

				if (Files.notExists(awPath)) {
					throw new NoSuchFileException("Could not find AW '" + aw + "' to convert into AT!");
				}

				try (BufferedReader reader = Files.newBufferedReader(awPath, StandardCharsets.UTF_8)) {
					at.merge(Aw2At.toAccessTransformSet(reader));
				}

				Files.delete(awPath);
			}

			MappingsService service = serviceFactory.get(options);
			at = at.remap(service.getMemoryMappingTree(), service.getFrom(), service.getTo());

			try (Writer writer = new LfWriter(Files.newBufferedWriter(atPath))) {
				AccessTransformFormats.FML.write(writer, at);
			}
		}
	}
}
