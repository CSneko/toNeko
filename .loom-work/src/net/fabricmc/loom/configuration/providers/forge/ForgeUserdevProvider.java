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

import java.io.File;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.architectury.loom.forge.UserdevConfig;
import org.gradle.api.Project;

import net.fabricmc.loom.configuration.DependencyInfo;
import net.fabricmc.loom.util.Constants;
import net.fabricmc.loom.util.ZipUtils;

public class ForgeUserdevProvider extends DependencyProvider {
	private File userdevJar;
	private JsonObject json;
	private UserdevConfig config;
	Path joinedPatches;

	public ForgeUserdevProvider(Project project) {
		super(project);
	}

	@Override
	public void provide(DependencyInfo dependency) throws Exception {
		userdevJar = new File(getExtension().getForgeProvider().getGlobalCache(), "forge-userdev.jar");
		joinedPatches = getExtension().getForgeProvider().getGlobalCache().toPath().resolve("patches-joined.lzma");
		Path configJson = getExtension().getForgeProvider().getGlobalCache().toPath().resolve("forge-config.json");

		if (!userdevJar.exists() || Files.notExists(configJson) || refreshDeps()) {
			File resolved = dependency.resolveFile().orElseThrow(() -> new RuntimeException("Could not resolve Forge userdev"));
			Files.copy(resolved.toPath(), userdevJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
			Files.write(configJson, ZipUtils.unpack(resolved.toPath(), "config.json"));
		}

		try (Reader reader = Files.newBufferedReader(configJson)) {
			json = new Gson().fromJson(reader, JsonObject.class);
			config = UserdevConfig.CODEC.parse(JsonOps.INSTANCE, json)
					.getOrThrow(false, msg -> getProject().getLogger().error("Couldn't read userdev config, {}", msg));
		}

		addDependency(config.mcp(), Constants.Configurations.MCP_CONFIG);

		if (!getExtension().isNeoForge()) {
			addDependency(config.mcp(), Constants.Configurations.SRG);
		}

		addDependency(config.universal(), Constants.Configurations.FORGE_UNIVERSAL);

		if (Files.notExists(joinedPatches)) {
			Files.write(joinedPatches, ZipUtils.unpack(userdevJar.toPath(), config.binpatches()));
		}
	}

	public File getUserdevJar() {
		return userdevJar;
	}

	@Override
	public String getTargetConfig() {
		return Constants.Configurations.FORGE_USERDEV;
	}

	public JsonObject getJson() {
		return json;
	}

	public UserdevConfig getConfig() {
		return config;
	}
}
