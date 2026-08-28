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

package net.fabricmc.loom.configuration.providers.forge.minecraft;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import net.fabricmc.loom.configuration.ConfigContext;
import net.fabricmc.loom.configuration.providers.forge.MinecraftPatchedProvider;
import net.fabricmc.loom.configuration.providers.minecraft.MergedMinecraftProvider;
import net.fabricmc.loom.configuration.providers.minecraft.MinecraftMetadataProvider;

public final class MergedForgeMinecraftProvider extends MergedMinecraftProvider implements ForgeMinecraftProvider {
	private final MinecraftPatchedProvider patchedProvider;

	public MergedForgeMinecraftProvider(MinecraftMetadataProvider metadataProvider, ConfigContext configContext) {
		super(metadataProvider, configContext);
		this.patchedProvider = new MinecraftPatchedProvider(configContext.project(), this, MinecraftPatchedProvider.Type.MERGED);
	}

	@Override
	protected void mergeJars() throws IOException {
		// Don't merge jars in the superclass
	}

	@Override
	public Path getMergedJar() {
		return patchedProvider.getMinecraftPatchedJar();
	}

	@Override
	public List<Path> getMinecraftJars() {
		return List.of(patchedProvider.getMinecraftPatchedJar());
	}

	@Override
	public MinecraftPatchedProvider getPatchedProvider() {
		return patchedProvider;
	}
}
