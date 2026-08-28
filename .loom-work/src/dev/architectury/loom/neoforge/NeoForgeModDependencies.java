package dev.architectury.loom.neoforge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import dev.architectury.at.AccessTransformSet;
import dev.architectury.at.io.AccessTransformFormats;
import dev.architectury.loom.metadata.ModMetadataFile;
import dev.architectury.loom.metadata.ModMetadataFiles;

import net.fabricmc.loom.util.Constants;
import net.fabricmc.loom.util.FileSystemUtil;
import net.fabricmc.loom.util.ModPlatform;
import net.fabricmc.mappingio.tree.MappingTreeView;

public final class NeoForgeModDependencies {
	public static void remapAts(Path jar, MappingTreeView mappings, String from, String to) throws IOException {
		final ModMetadataFile modMetadata = ModMetadataFiles.fromJar(jar);
		Set<String> atPaths = Set.of(Constants.Forge.ACCESS_TRANSFORMER_PATH);

		if (modMetadata != null) {
			final Set<String> modsTomlAts = modMetadata.getAccessTransformers(ModPlatform.NEOFORGE);

			if (!modsTomlAts.isEmpty()) {
				atPaths = modsTomlAts;
			}
		}

		try (FileSystemUtil.Delegate fs = FileSystemUtil.getJarFileSystem(jar)) {
			for (String atPathStr : atPaths) {
				final Path atPath = fs.getPath(atPathStr);

				if (Files.exists(atPath)) {
					AccessTransformSet ats = AccessTransformFormats.FML.read(atPath);
					ats = ats.remap(mappings, from, to);
					AccessTransformFormats.FML.write(atPath, ats);
				}
			}
		}
	}
}
