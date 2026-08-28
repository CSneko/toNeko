/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2020-2023 FabricMC
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

package net.fabricmc.loom.util.srg;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

import net.fabricmc.loom.build.IntermediaryNamespaces;
import net.fabricmc.loom.util.FileSystemUtil;
import net.fabricmc.loom.util.ModPlatform;
import net.fabricmc.loom.util.function.CollectionUtil;
import net.fabricmc.mappingio.tree.MappingTree;

/**
 * Remaps coremod class names from SRG to Yarn.
 *
 * @author Juuz
 */
public final class CoreModClassRemapper {
	private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("^(.*')((?:com\\.mojang\\.|net\\.minecraft\\.)[A-Za-z0-9.-_$]+)('.*)$");
	private static final Pattern REDIRECT_FIELD_TO_METHOD_PATTERN = Pattern.compile("^(.*\\w+\\s*\\.\\s*redirectFieldToMethod\\s*\\(\\s*\\w+\\s*,\\s*')(\\w*)('\\s*,(?:\\s*'(\\w+)'\\s*|.*)\\).*)$");

	public static void remapJar(Project project, ModPlatform platform, Path jar, MappingTree mappings) throws IOException {
		final Logger logger = project.getLogger();
		final String sourceNamespace = IntermediaryNamespaces.runtimeIntermediary(project);

		try (FileSystemUtil.Delegate fs = FileSystemUtil.getJarFileSystem(jar, false)) {
			Path coremodsJsonPath = fs.getPath("META-INF", "coremods.json");

			if (Files.notExists(coremodsJsonPath)) {
				logger.info(":no coremods in " + jar.getFileName());
				return;
			}

			JsonObject coremodsJson;

			try (Reader reader = Files.newBufferedReader(coremodsJsonPath)) {
				coremodsJson = new Gson().fromJson(reader, JsonObject.class);
			}

			for (Map.Entry<String, JsonElement> nameFileEntry : coremodsJson.entrySet()) {
				String file = nameFileEntry.getValue().getAsString();
				Path js = fs.getPath(file);

				if (Files.exists(js)) {
					logger.info(":remapping coremod '" + file + "'");
					remap(js, platform, mappings, sourceNamespace);
				} else {
					logger.warn("Coremod '" + file + "' listed in coremods.json but not found");
				}
			}
		}
	}

	public static void remap(Path js, ModPlatform platform, MappingTree mappings, String sourceNamespace) throws IOException {
		List<String> lines = Files.readAllLines(js);
		List<String> output = new ArrayList<>(lines);
		String lastClassName = null;

		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			Matcher matcher = CLASS_NAME_PATTERN.matcher(line);

			if (matcher.matches()) {
				String className = matcher.group(2).replace('.', '/');
				String remapped = CollectionUtil.find(mappings.getClasses(), def -> def.getName(sourceNamespace).equals(className))
						.map(def -> def.getName("named"))
						.orElse(className);
				lastClassName = remapped;

				if (!className.equals(remapped)) {
					output.set(i, matcher.group(1) + remapped.replace('/', '.') + matcher.group(3));
				}
			} else if (platform == ModPlatform.NEOFORGE && lastClassName != null) {
				matcher = REDIRECT_FIELD_TO_METHOD_PATTERN.matcher(line);

				if (matcher.matches()) {
					String fieldName = matcher.group(2);
					String remapped = Optional.ofNullable(mappings.getClass(lastClassName, mappings.getNamespaceId("named")))
							.flatMap(clazz -> Optional.ofNullable(clazz.getField(fieldName, null, mappings.getNamespaceId(sourceNamespace))))
							.map(field -> field.getName("named"))
							.orElse(fieldName);

					if (!fieldName.equals(remapped)) {
						String optionalMethod = matcher.group(4);
						String remappedMethod = optionalMethod == null ? null
								: Optional.ofNullable(mappings.getClass(lastClassName, mappings.getNamespaceId("named")))
								.flatMap(clazz -> Optional.ofNullable(clazz.getMethod(optionalMethod, null, mappings.getNamespaceId(sourceNamespace))))
								.map(method -> method.getName("named"))
								.orElse(null);

						if (remappedMethod != null) {
							output.set(i, matcher.group(1) + remapped + matcher.group(3).replace("'" + optionalMethod + "'", "'" + remappedMethod + "'"));
						} else {
							output.set(i, matcher.group(1) + remapped + matcher.group(3));
						}
					}
				}
			}
		}

		if (!lines.equals(output)) {
			try (Writer writer = Files.newBufferedWriter(js, StandardCharsets.UTF_8, StandardOpenOption.WRITE)) {
				writer.write(String.join("\n", output));
			}
		}
	}
}
