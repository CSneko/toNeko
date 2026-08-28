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

package net.fabricmc.loom.configuration.providers.forge.mcpconfig.steplogic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import dev.architectury.loom.forge.tool.ForgeToolExecutor;
import org.gradle.api.Action;
import org.gradle.api.logging.Logger;

import net.fabricmc.loom.configuration.providers.forge.ConfigValue;
import net.fabricmc.loom.util.download.DownloadBuilder;
import net.fabricmc.loom.util.function.CollectionUtil;

/**
 * The logic for executing a step. This corresponds to the {@code type} key in the step JSON format.
 */
public interface StepLogic {
	void execute(ExecutionContext context) throws IOException;

	default String getDisplayName(String stepName) {
		return stepName;
	}

	default boolean hasNoContext() {
		return false;
	}

	interface ExecutionContext {
		Logger logger();
		Path setOutput(String fileName) throws IOException;
		Path setOutput(Path output);
		Path cache() throws IOException;
		/** Mappings extracted from {@code data.mappings} in the MCPConfig JSON. */
		Path mappings();
		String resolve(ConfigValue value);
		Path downloadFile(String url) throws IOException;
		Path downloadDependency(String notation);
		DownloadBuilder downloadBuilder(String url);
		void javaexec(Action<? super ForgeToolExecutor.Settings> configurator);
		Set<File> getMinecraftLibraries();

		default List<String> resolve(List<ConfigValue> configValues) {
			return CollectionUtil.map(configValues, this::resolve);
		}
	}

	@FunctionalInterface
	interface Provider {
		Optional<StepLogic> getStepLogic(String name, String type);
	}
}
