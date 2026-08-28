/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024 FabricMC
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import com.google.common.base.Stopwatch;
import org.gradle.api.Project;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.configuration.providers.mappings.MappingConfiguration;

public final class ForgeMigratedMappingConfiguration extends MappingConfiguration {
	private final List<MappingsMigrator> migrators = List.of(new FieldMappingsMigrator(), new MethodInheritanceMappingsMigrator());
	private Path hashPath;
	private Path rawTinyMappings;
	private Path rawTinyMappingsWithSrg;
	private Path rawTinyMappingsWithMojang;
	private long hash;

	public ForgeMigratedMappingConfiguration(String mappingsIdentifier, Path mappingsWorkingDir) {
		super(mappingsIdentifier, mappingsWorkingDir);
	}

	@Override
	protected void manipulateMappings(Project project, Path mappingsJar) throws IOException {
		LoomGradleExtension extension = LoomGradleExtension.get(project);
		final Path forgeCache = ForgeProvider.getForgeCache(project);
		Files.createDirectories(forgeCache);

		boolean hasSrg = extension.shouldGenerateSrgTiny();
		boolean hasMojang = extension.isNeoForge();

		this.hashPath = forgeCache.resolve("mappings-migrated.hash");
		this.hash = 1;

		this.rawTinyMappings = this.tinyMappings;
		this.rawTinyMappingsWithSrg = this.tinyMappingsWithSrg;
		this.rawTinyMappingsWithMojang = this.tinyMappingsWithMojang;
		Path rawTinyMappingsWithNs = hasSrg ? this.rawTinyMappingsWithSrg : hasMojang ? this.rawTinyMappingsWithMojang : this.rawTinyMappings;

		this.tinyMappings = mappingsWorkingDir().resolve("mappings-migrated.tiny");
		this.tinyMappingsWithSrg = mappingsWorkingDir().resolve("mappings-srg-migrated.tiny");
		this.tinyMappingsWithMojang = mappingsWorkingDir().resolve("mappings-mojang-migrated.tiny");
		Path tinyMappingsWithNs = hasSrg ? this.tinyMappingsWithSrg : hasMojang ? this.tinyMappingsWithMojang : this.tinyMappings;

		for (MappingsMigrator migrator : this.migrators) {
			hash = hash * 31 + migrator.setup(project, extension.getMinecraftProvider(), forgeCache, rawTinyMappingsWithNs, hasSrg, hasMojang);
		}

		if (!isOutdated(extension, hasSrg, hasMojang)) {
			project.getLogger().info(":manipulated {} mappings are up to date", extension.getPlatform().get().id());
			return;
		}

		Stopwatch stopwatch = Stopwatch.createStarted();
		Files.copy(this.rawTinyMappings, this.tinyMappings, StandardCopyOption.REPLACE_EXISTING);
		Files.copy(rawTinyMappingsWithNs, tinyMappingsWithNs, StandardCopyOption.REPLACE_EXISTING);

		Files.writeString(this.hashPath, Long.toString(this.hash), StandardCharsets.UTF_8);

		for (MappingsMigrator migrator : this.migrators) {
			Path path = Files.createTempFile("mappings-working", ".tiny");
			Path pathWithNs = Files.createTempFile("mappings-working-ns", ".tiny");
			Files.copy(this.tinyMappings, path, StandardCopyOption.REPLACE_EXISTING);
			Files.copy(tinyMappingsWithNs, pathWithNs, StandardCopyOption.REPLACE_EXISTING);

			List<MappingsMigrator.MappingsEntry> entries = List.of(new MappingsMigrator.MappingsEntry(path), new MappingsMigrator.MappingsEntry(pathWithNs));
			migrator.migrate(project, entries);

			Files.copy(path, this.tinyMappings, StandardCopyOption.REPLACE_EXISTING);
			Files.copy(pathWithNs, tinyMappingsWithNs, StandardCopyOption.REPLACE_EXISTING);
			Files.deleteIfExists(path);
			Files.deleteIfExists(pathWithNs);
		}

		project.getLogger().info(":manipulated {} mappings in " + stopwatch.stop(), extension.getPlatform().get().id());
	}

	private boolean isOutdated(LoomGradleExtension extension, boolean hasSrg, boolean hasMojang) throws IOException {
		if (extension.refreshDeps()) return true;
		if (Files.notExists(this.tinyMappings)) return true;
		if (hasSrg && Files.notExists(this.tinyMappingsWithSrg)) return true;
		if (hasMojang && Files.notExists(this.tinyMappingsWithMojang)) return true;
		if (Files.notExists(this.hashPath)) return true;
		String hashStr = Files.readString(hashPath, StandardCharsets.UTF_8);
		return !Long.toString(this.hash).equals(hashStr);
	}
}
