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

package net.fabricmc.loom.configuration.providers.forge;

import com.mojang.serialization.Codec;

/**
 * A string or a variable in a Forge configuration file, or an MCPConfig step or function.
 */
public sealed interface ConfigValue {
	/**
	 * The variable that refers to the current MCP step's output path.
	 */
	String OUTPUT = "output";
	/**
	 * A suffix that is appended to the name of an MCP step to get its output path.
	 */
	String PREVIOUS_OUTPUT_SUFFIX = "Output";
	/**
	 * The variable that refers to a log file for the MCP executor.
	 */
	String LOG = "log";

	Codec<ConfigValue> CODEC = Codec.STRING.xmap(ConfigValue::of, configValue -> {
		if (configValue instanceof Constant constant) {
			return constant.value();
		} else if (configValue instanceof Variable variable) {
			return "{" + variable.name() + "}";
		}

		throw new IllegalArgumentException("Unmatched config value");
	});

	String resolve(Resolver variableResolver);

	static ConfigValue of(String str) {
		if (str.startsWith("{") && str.endsWith("}")) {
			return new Variable(str.substring(1, str.length() - 1));
		}

		return new Constant(str);
	}

	@FunctionalInterface
	interface Resolver {
		String resolve(Variable variable);
	}

	record Constant(String value) implements ConfigValue {
		@Override
		public String resolve(Resolver variableResolver) {
			return value;
		}
	}

	record Variable(String name) implements ConfigValue {
		@Override
		public String resolve(Resolver variableResolver) {
			return variableResolver.resolve(this);
		}
	}
}
