/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022-2023 FabricMC
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

package net.fabricmc.loom.configuration.providers.forge.mcpconfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.loom.configuration.providers.forge.ConfigValue;
import net.fabricmc.loom.configuration.providers.forge.mcpconfig.steplogic.StepLogic;
import net.fabricmc.loom.util.function.CollectionUtil;

/**
 * An executable program for {@linkplain McpConfigStep steps}.
 *
 * @param version the Gradle-style dependency string of the program
 * @param args    the command-line arguments
 * @param jvmArgs the JVM arguments
 * @param repo    the Maven repository to download the dependency from, or {@code null} if not specified
 */
public record McpConfigFunction(String version, List<ConfigValue> args, List<ConfigValue> jvmArgs, @Nullable String repo) {
	private static final String VERSION_KEY = "version";
	private static final String ARGS_KEY = "args";
	private static final String JVM_ARGS_KEY = "jvmargs";
	private static final String REPO_KEY = "repo";

	public Path download(StepLogic.ExecutionContext executionContext) throws IOException {
		if (repo != null) {
			return executionContext.downloadFile(getDownloadUrl());
		} else {
			return executionContext.downloadDependency(version);
		}
	}

	private String getDownloadUrl() {
		String[] parts = version.split(":");
		StringBuilder builder = new StringBuilder();
		builder.append(repo);
		// Group:
		builder.append(parts[0].replace('.', '/')).append('/');
		// Name:
		builder.append(parts[1]).append('/');
		// Version:
		builder.append(parts[2]).append('/');
		// Artifact:
		builder.append(parts[1]).append('-').append(parts[2]);

		// Classifier:
		if (parts.length >= 4) {
			builder.append('-').append(parts[3]);
		}

		builder.append(".jar");
		return builder.toString();
	}

	public static McpConfigFunction fromJson(JsonObject json) {
		String version = json.get(VERSION_KEY).getAsString();
		List<ConfigValue> args = json.has(ARGS_KEY) ? configValuesFromJson(json.getAsJsonArray(ARGS_KEY)) : List.of();
		List<ConfigValue> jvmArgs = json.has(JVM_ARGS_KEY) ? configValuesFromJson(json.getAsJsonArray(JVM_ARGS_KEY)) : List.of();
		JsonElement repoJson = json.get(REPO_KEY);
		@Nullable String repo = repoJson.isJsonPrimitive() ? repoJson.getAsString() : null;
		return new McpConfigFunction(version, args, jvmArgs, repo);
	}

	private static List<ConfigValue> configValuesFromJson(JsonArray json) {
		return CollectionUtil.map(json, child -> ConfigValue.of(child.getAsString()));
	}
}
