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

import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import net.fabricmc.loom.configuration.providers.forge.mcpconfig.McpConfigFunction;

/**
 * Runs a Forge tool configured by a {@linkplain McpConfigFunction function}.
 */
public final class FunctionLogic implements StepLogic {
	private final McpConfigFunction function;

	public FunctionLogic(McpConfigFunction function) {
		this.function = function;
	}

	@Override
	public void execute(ExecutionContext context) throws IOException {
		// These are almost always jars, and it's expected by some tools such as ForgeFlower.
		// The other tools seem to work with the name containing .jar anyway.
		// Technically, FG supports an "outputExtension" config value for steps, but it's not used in practice.
		context.setOutput("output.jar");
		Path jar = function.download(context);
		String mainClass;

		try (JarFile jarFile = new JarFile(jar.toFile())) {
			mainClass = jarFile.getManifest().getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
		} catch (IOException e) {
			throw new IOException("Could not determine main class for " + jar.toAbsolutePath(), e);
		}

		context.javaexec(spec -> {
			spec.classpath(jar);
			spec.getMainClass().set(mainClass);
			spec.args(context.resolve(function.args()));
			spec.jvmArgs(context.resolve(function.jvmArgs()));
		});
	}

	@Override
	public String getDisplayName(String stepName) {
		return stepName + " with " + function.version();
	}
}
