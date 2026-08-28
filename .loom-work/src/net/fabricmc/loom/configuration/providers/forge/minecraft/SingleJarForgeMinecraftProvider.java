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

import java.nio.file.Path;
import java.util.List;

import net.fabricmc.loom.api.mappings.layered.MappingsNamespace;
import net.fabricmc.loom.configuration.ConfigContext;
import net.fabricmc.loom.configuration.providers.BundleMetadata;
import net.fabricmc.loom.configuration.providers.forge.MinecraftPatchedProvider;
import net.fabricmc.loom.configuration.providers.minecraft.MinecraftMetadataProvider;
import net.fabricmc.loom.configuration.providers.minecraft.SingleJarEnvType;
import net.fabricmc.loom.configuration.providers.minecraft.SingleJarMinecraftProvider;

public abstract class SingleJarForgeMinecraftProvider extends SingleJarMinecraftProvider implements ForgeMinecraftProvider {
	private final MinecraftPatchedProvider patchedProvider;

	private SingleJarForgeMinecraftProvider(MinecraftMetadataProvider metadataProvider, ConfigContext configContext) {
		super(metadataProvider, configContext, MappingsNamespace.OFFICIAL);
		this.patchedProvider = new MinecraftPatchedProvider(configContext.project(), this, provideServer() ? MinecraftPatchedProvider.Type.SERVER_ONLY : MinecraftPatchedProvider.Type.CLIENT_ONLY);
	}

	public static SingleJarForgeMinecraftProvider.Server forgeServer(MinecraftMetadataProvider metadataProvider, ConfigContext configContext) {
		return new SingleJarForgeMinecraftProvider.Server(metadataProvider, configContext);
	}

	public static SingleJarForgeMinecraftProvider.Client forgeClient(MinecraftMetadataProvider metadataProvider, ConfigContext configContext) {
		return new SingleJarForgeMinecraftProvider.Client(metadataProvider, configContext);
	}

	@Override
	protected boolean provideClient() {
		// the client jar is needed for client-extra which the Forge userdev launch thing always checks for
		return true;
	}

	@Override
	protected void processJar() throws Exception {
		// don't process the jar, it's created by the patched provider
	}

	@Override
	public MinecraftPatchedProvider getPatchedProvider() {
		return patchedProvider;
	}

	@Override
	public Path getMinecraftEnvOnlyJar() {
		return patchedProvider.getMinecraftPatchedJar();
	}

	@Override
	public List<Path> getMinecraftJars() {
		return List.of(patchedProvider.getMinecraftPatchedJar());
	}

	public static final class Server extends SingleJarForgeMinecraftProvider {
		private Server(MinecraftMetadataProvider metadataProvider, ConfigContext configContext) {
			super(metadataProvider, configContext);
		}

		@Override
		public SingleJarEnvType type() {
			return SingleJarEnvType.SERVER;
		}

		@Override
		public Path getInputJar(SingleJarMinecraftProvider provider) throws Exception {
			BundleMetadata serverBundleMetadata = provider.getServerBundleMetadata();

			if (serverBundleMetadata == null) {
				return provider.getMinecraftServerJar().toPath();
			}

			provider.extractBundledServerJar();
			return provider.getMinecraftExtractedServerJar().toPath();
		}

		@Override
		protected boolean provideServer() {
			return true;
		}

		@Override
		protected boolean provideClient() {
			return false;
		}
	}

	public static final class Client extends SingleJarForgeMinecraftProvider {
		private Client(MinecraftMetadataProvider metadataProvider, ConfigContext configContext) {
			super(metadataProvider, configContext);
		}

		@Override
		public SingleJarEnvType type() {
			return SingleJarEnvType.CLIENT;
		}

		@Override
		public Path getInputJar(SingleJarMinecraftProvider provider) throws Exception {
			return provider.getMinecraftClientJar().toPath();
		}

		@Override
		protected boolean provideServer() {
			return false;
		}

		@Override
		protected boolean provideClient() {
			return true;
		}
	}
}
