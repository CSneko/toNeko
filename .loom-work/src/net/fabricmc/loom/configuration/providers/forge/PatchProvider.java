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

package net.fabricmc.loom.configuration.providers.forge;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.gradle.api.Project;

import net.fabricmc.loom.configuration.DependencyInfo;
import net.fabricmc.loom.util.Constants;
import net.fabricmc.loom.util.FileSystemUtil;

public class PatchProvider extends DependencyProvider {
	public Path clientPatches;
	public Path serverPatches;

	public PatchProvider(Project project) {
		super(project);
	}

	@Override
	public void provide(DependencyInfo dependency) throws Exception {
		init();

		if (Files.notExists(clientPatches) || Files.notExists(serverPatches) || refreshDeps()) {
			getProject().getLogger().info(":extracting forge patches");

			Path installerJar = dependency.resolveFile().orElseThrow(() -> new RuntimeException("Could not resolve Forge installer")).toPath();

			try (FileSystemUtil.Delegate fs = FileSystemUtil.getJarFileSystem(installerJar, false)) {
				Files.copy(fs.getPath("data", "client.lzma"), clientPatches, StandardCopyOption.REPLACE_EXISTING);
				Files.copy(fs.getPath("data", "server.lzma"), serverPatches, StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}

	private void init() {
		final Path projectCacheFolder = ForgeProvider.getForgeCache(getProject());
		clientPatches = projectCacheFolder.resolve("patches-client.lzma");
		serverPatches = projectCacheFolder.resolve("patches-server.lzma");

		try {
			Files.createDirectories(projectCacheFolder);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public String getTargetConfig() {
		return Constants.Configurations.FORGE_INSTALLER;
	}
}
