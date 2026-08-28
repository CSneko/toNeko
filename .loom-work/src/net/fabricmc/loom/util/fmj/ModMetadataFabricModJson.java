/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2023 FabricMC
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

package net.fabricmc.loom.util.fmj;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.architectury.loom.metadata.JsonBackedModMetadataFile;
import dev.architectury.loom.metadata.ModMetadataFile;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.loom.util.gradle.SourceSetHelper;

public final class ModMetadataFabricModJson extends FabricModJson {
	private static final int FABRIC_SCHEMA_VERSION = -1;
	private final ModMetadataFile modMetadata;
	private final FabricModJsonSource source;

	ModMetadataFabricModJson(ModMetadataFile modMetadata, FabricModJsonSource source) {
		super(getJsonForModMetadata(modMetadata), source);
		this.modMetadata = modMetadata;
		this.source = source;
	}

	private static JsonObject getJsonForModMetadata(ModMetadataFile modMetadata) {
		if (modMetadata instanceof JsonBackedModMetadataFile jsonBacked) {
			return jsonBacked.getJson();
		}

		return new JsonObject();
	}

	public ModMetadataFile getModMetadata() {
		return modMetadata;
	}

	@Override
	public String getId() {
		return Optional.ofNullable(modMetadata.getId())
				.or(this::getIdFromSource)
				.orElseGet(super::getId);
	}

	private Optional<String> getIdFromSource() {
		if (source instanceof FabricModJsonSource.ZipSource zip) {
			return Optional.of(zip.zipPath().getFileName().toString());
		} else if (source instanceof FabricModJsonSource.DirectorySource directory) {
			return Optional.of(directory.directoryPath().toAbsolutePath().toString());
		} else if (source instanceof FabricModJsonSource.SourceSetSource sourceSets) {
			final StringJoiner joiner = new StringJoiner("+");

			for (SourceSet sourceSet : sourceSets.sourceSets()) {
				final Project project = SourceSetHelper.getSourceSetProject(sourceSet);
				final String path = project.getPath();
				final StringBuilder sb = new StringBuilder(path);

				if (!path.endsWith(":")) {
					sb.append(':');
				}

				sb.append(sourceSet.getName());
				joiner.add(sb.toString());
			}

			return Optional.of(joiner.toString());
		}

		return Optional.empty();
	}

	@Override
	public int getVersion() {
		// Technically not correct since we're not a real fabric.mod.json,
		// but this is needed for computing the hash code.
		return FABRIC_SCHEMA_VERSION;
	}

	@Override
	public @Nullable JsonElement getCustom(String key) {
		if (modMetadata instanceof JsonBackedModMetadataFile jsonBacked) {
			return jsonBacked.getCustomValue(key);
		}

		return null;
	}

	@Override
	public List<String> getMixinConfigurations() {
		return modMetadata.getMixinConfigs();
	}

	@Override
	public Map<String, ModEnvironment> getClassTweakers() {
		return modMetadata.getAccessWideners()
				.stream()
				.collect(Collectors.toMap(Function.identity(), path -> ModEnvironment.UNIVERSAL));
	}
}
