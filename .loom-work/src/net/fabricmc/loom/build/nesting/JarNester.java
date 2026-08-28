/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2018-2021 FabricMC
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

package net.fabricmc.loom.build.nesting;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.gradle.api.UncheckedIOException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import net.fabricmc.loom.LoomGradlePlugin;
import net.fabricmc.loom.util.ModPlatform;
import net.fabricmc.loom.util.Pair;
import net.fabricmc.loom.util.ZipUtils;
import net.fabricmc.loom.util.fmj.FabricModJsonFactory;

public class JarNester {
	public static void nestJars(Collection<File> jars, File modJar, ModPlatform platform, Logger logger) {
		if (jars.isEmpty()) {
			logger.debug("Nothing to nest into " + modJar.getName());
			return;
		}

		Preconditions.checkArgument(FabricModJsonFactory.isNestableModJar(modJar, platform), "Cannot nest jars into none mod jar " + modJar.getName());

		// Ensure deterministic ordering of entries in fabric.mod.json
		Collection<File> sortedJars = jars.stream().sorted(Comparator.comparing(File::getName)).toList();

		try {
			ZipUtils.add(modJar.toPath(), sortedJars.stream().map(file -> {
				try {
					return new Pair<>("META-INF/jars/" + file.getName(), Files.readAllBytes(file.toPath()));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}).collect(Collectors.toList()));

			if (platform.isForgeLike()) {
				handleForgeJarJar(jars, modJar, logger);
				return;
			}

			int count = ZipUtils.transformJson(JsonObject.class, modJar.toPath(), Stream.of(platform == ModPlatform.FABRIC ? new Pair<>("fabric.mod.json", json -> {
				JsonArray nestedJars = json.getAsJsonArray("jars");

				if (nestedJars == null || !json.has("jars")) {
					nestedJars = new JsonArray();
				}

				for (File file : sortedJars) {
					String nestedJarPath = "META-INF/jars/" + file.getName();
					Preconditions.checkArgument(FabricModJsonFactory.isNestableModJar(file, platform), "Cannot nest none mod jar: " + file.getName());

					for (JsonElement nestedJar : nestedJars) {
						JsonObject jsonObject = nestedJar.getAsJsonObject();

						if (jsonObject.has("file") && jsonObject.get("file").getAsString().equals(nestedJarPath)) {
							throw new IllegalStateException("Cannot nest 2 jars at the same path: " + nestedJarPath);
						}
					}

					JsonObject jsonObject = new JsonObject();
					jsonObject.addProperty("file", nestedJarPath);
					nestedJars.add(jsonObject);

					logger.debug("Nested " + nestedJarPath + " into " + modJar.getName());
				}

				json.add("jars", nestedJars);

				return json;
			}) : platform == ModPlatform.QUILT ? new Pair<>("quilt.mod.json", json -> {
				JsonObject loader;

				if (json.has("quilt_loader")) {
					loader = json.getAsJsonObject("quilt_loader");
				} else {
					json.add("quilt_loader", loader = new JsonObject());
				}

				JsonArray nestedJars = loader.getAsJsonArray("jars");

				if (nestedJars == null || !loader.has("jars")) {
					nestedJars = new JsonArray();
				}

				for (File file : jars) {
					String nestedJarPath = "META-INF/jars/" + file.getName();
					Preconditions.checkArgument(FabricModJsonFactory.isNestableModJar(file, platform), "Cannot nest none mod jar: " + file.getName());

					for (JsonElement nestedJar : nestedJars) {
						String nestedJarString = nestedJar.getAsString();

						if (nestedJarPath.equals(nestedJarString)) {
							throw new IllegalStateException("Cannot nest 2 jars at the same path: " + nestedJarString);
						}
					}

					nestedJars.add(nestedJarPath);

					logger.debug("Nested " + nestedJarPath + " into " + modJar.getName());
				}

				loader.add("jars", nestedJars);

				return json;
			}) : null));

			Preconditions.checkState(count > 0, "Failed to transform fabric.mod.json");
		} catch (IOException e) {
			throw new java.io.UncheckedIOException("Failed to nest jars into " + modJar.getName(), e);
		}
	}

	private static @Nullable NestableJarGenerationTask.Metadata readNestedFile(File file, Logger logger) {
		try {
			return ZipUtils.unpackGsonNullable(file.toPath(), NestableJarGenerationTask.NESTING_METADATA_PATH, NestableJarGenerationTask.Metadata.class);
		} catch (IOException e) {
			logger.error("Could not read {}", file.getAbsolutePath(), e);
			return null;
		}
	}

	private static void handleForgeJarJar(Collection<File> jars, File modJar, Logger logger) throws IOException {
		JsonObject json = new JsonObject();
		JsonArray nestedJars = new JsonArray();

		for (File file : jars) {
			NestableJarGenerationTask.Metadata metadata = readNestedFile(file, logger);

			if (metadata == null) {
				logger.error("Jar {} does not contain Loom nesting metadata", file.getAbsolutePath());
				continue;
			}

			String nestedJarPath = "META-INF/jars/" + file.getName();

			for (JsonElement nestedJar : nestedJars) {
				JsonObject jsonObject = nestedJar.getAsJsonObject();

				if (jsonObject.has("path") && jsonObject.get("path").getAsString().equals(nestedJarPath)) {
					throw new IllegalStateException("Cannot nest 2 jars at the same path: " + nestedJarPath);
				}
			}

			JsonObject jsonObject = new JsonObject();
			JsonObject identifierObject = new JsonObject();
			JsonObject versionObject = new JsonObject();
			identifierObject.addProperty("group", metadata.group());
			identifierObject.addProperty("artifact", metadata.name());
			versionObject.addProperty("range", "[" + metadata.version() + ",)");
			versionObject.addProperty("artifactVersion", metadata.version());
			jsonObject.add("identifier", identifierObject);
			jsonObject.add("version", versionObject);
			jsonObject.addProperty("path", nestedJarPath);
			nestedJars.add(jsonObject);

			logger.debug("Nested " + nestedJarPath + " into " + modJar.getName());
		}

		json.add("jars", nestedJars);

		ZipUtils.add(modJar.toPath(), "META-INF/jarjar/metadata.json", LoomGradlePlugin.GSON.toJson(json));
	}
}
