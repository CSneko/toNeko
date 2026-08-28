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
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.stream.Stream;

import net.fabricmc.loom.configuration.providers.forge.ConfigValue;
import net.fabricmc.loom.util.FileSystemUtil;

public final class InjectLogic implements StepLogic {
	@Override
	public void execute(ExecutionContext context) throws IOException {
		Path injectedFiles = Path.of(context.resolve(new ConfigValue.Variable("inject")));
		Path input = Path.of(context.resolve(new ConfigValue.Variable("input")));
		Path output = context.setOutput("output.jar");
		Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING);

		try (FileSystemUtil.Delegate targetFs = FileSystemUtil.getJarFileSystem(output, false)) {
			FileSystem fs = targetFs.get();

			try (Stream<Path> paths = Files.walk(injectedFiles)) {
				Iterator<Path> iter = paths.filter(Files::isRegularFile).iterator();

				while (iter.hasNext()) {
					Path from = iter.next();
					Path relative = injectedFiles.relativize(from);
					Path to = fs.getPath(relative.toString().replace(relative.getFileSystem().getSeparator(), "/"));

					if (to.getParent() != null) {
						Files.createDirectories(to.getParent());
					}

					Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
	}
}
