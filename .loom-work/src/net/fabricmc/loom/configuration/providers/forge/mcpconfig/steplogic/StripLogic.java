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

package net.fabricmc.loom.configuration.providers.forge.mcpconfig.steplogic;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.stream.Collectors;

import net.fabricmc.loom.configuration.providers.forge.ConfigValue;
import net.fabricmc.loom.util.FileSystemUtil;
import net.fabricmc.loom.util.ThreadingUtils;

/**
 * Strips certain classes from the jar.
 */
public final class StripLogic implements StepLogic {
	@Override
	public void execute(ExecutionContext context) throws IOException {
		Set<String> filter = Files.readAllLines(context.mappings(), StandardCharsets.UTF_8).stream()
				.filter(s -> !s.startsWith("\t"))
				.map(s -> s.split(" ")[0] + ".class")
				.collect(Collectors.toSet());

		Path input = Path.of(context.resolve(new ConfigValue.Variable("input")));

		try (FileSystemUtil.Delegate output = FileSystemUtil.getJarFileSystem(context.setOutput("stripped.jar"), true)) {
			try (FileSystemUtil.Delegate fs = FileSystemUtil.getJarFileSystem(input, false)) {
				ThreadingUtils.TaskCompleter completer = ThreadingUtils.taskCompleter();

				for (Path path : (Iterable<? extends Path>) Files.walk(fs.get().getPath("/"))::iterator) {
					String trimLeadingSlash = trimLeadingSlash(path.toString());
					if (!trimLeadingSlash.endsWith(".class")) continue;
					boolean has = filter.contains(trimLeadingSlash);
					String s = trimLeadingSlash;

					while (s.contains("$") && !has) {
						s = s.substring(0, s.lastIndexOf("$")) + ".class";
						has = filter.contains(s);
					}

					if (!has) continue;
					Path to = output.get().getPath(trimLeadingSlash);
					Path parent = to.getParent();
					if (parent != null) Files.createDirectories(parent);

					completer.add(() -> {
						Files.copy(path, to, StandardCopyOption.COPY_ATTRIBUTES);
					});
				}

				completer.complete();
			}
		}
	}

	private static String trimLeadingSlash(String string) {
		if (string.startsWith(File.separator)) {
			return string.substring(File.separator.length());
		} else if (string.startsWith("/")) {
			return string.substring(1);
		}

		return string;
	}
}
