/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022 FabricMC
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

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Data extracted from the MCPConfig JSON file.
 *
 * @param version      the Minecraft version - the value of the {@code version} property
 * @param data         the value of the {@code data} property
 * @param mappingsPath the path to srg mappings inside the MCP zip
 * @param official     the value of the {@code official} property
 * @param steps        the MCP step definitions by environment type
 * @param functions    the MCP function definitions by name
 */
public record McpConfigData(
		String version,
		JsonObject data,
		String mappingsPath,
		boolean official,
		Map<String, List<McpConfigStep>> steps,
		Map<String, McpConfigFunction> functions
) {
	public static McpConfigData fromJson(JsonObject json) {
		String version = json.get("version").getAsString();
		JsonObject data = json.getAsJsonObject("data");
		String mappingsPath = data.get("mappings").getAsString();
		boolean official = json.has("official") && json.getAsJsonPrimitive("official").getAsBoolean();

		JsonObject stepsJson = json.getAsJsonObject("steps");
		ImmutableMap.Builder<String, List<McpConfigStep>> stepsBuilder = ImmutableMap.builder();

		for (String key : stepsJson.keySet()) {
			ImmutableList.Builder<McpConfigStep> stepListBuilder = ImmutableList.builder();

			for (JsonElement child : stepsJson.getAsJsonArray(key)) {
				stepListBuilder.add(McpConfigStep.fromJson(child.getAsJsonObject()));
			}

			stepsBuilder.put(key, stepListBuilder.build());
		}

		JsonObject functionsJson = json.getAsJsonObject("functions");
		ImmutableMap.Builder<String, McpConfigFunction> functionsBuilder = ImmutableMap.builder();

		for (String key : functionsJson.keySet()) {
			functionsBuilder.put(key, McpConfigFunction.fromJson(functionsJson.getAsJsonObject(key)));
		}

		return new McpConfigData(version, data, mappingsPath, official, stepsBuilder.build(), functionsBuilder.build());
	}
}
