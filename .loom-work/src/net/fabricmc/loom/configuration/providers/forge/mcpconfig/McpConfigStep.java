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

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import net.fabricmc.loom.configuration.providers.forge.ConfigValue;

public record McpConfigStep(String type, String name, Map<String, ConfigValue> config) {
	private static final String TYPE_KEY = "type";
	private static final String NAME_KEY = "name";

	public static McpConfigStep fromJson(JsonObject json) {
		String type = json.get(TYPE_KEY).getAsString();
		String name = json.has(NAME_KEY) ? json.get(NAME_KEY).getAsString() : type;
		ImmutableMap.Builder<String, ConfigValue> config = ImmutableMap.builder();

		for (String key : json.keySet()) {
			if (key.equals(TYPE_KEY) || key.equals(NAME_KEY)) continue;

			config.put(key, ConfigValue.of(json.get(key).getAsString()));
		}

		return new McpConfigStep(type, name, config.build());
	}
}
