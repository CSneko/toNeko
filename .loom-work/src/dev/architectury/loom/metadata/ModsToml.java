package dev.architectury.loom.metadata;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.toml.TomlParser;
import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.loom.configuration.ifaceinject.InterfaceInjectionProcessor;
import net.fabricmc.loom.util.ExceptionUtil;
import net.fabricmc.loom.util.ModPlatform;

public final class ModsToml implements ModMetadataFile {
	public static final String FILE_PATH = "META-INF/mods.toml";
	public static final String NEOFORGE_FILE_PATH = "META-INF/neoforge.mods.toml";
	private final Config config;

	private ModsToml(Config config) {
		this.config = Objects.requireNonNull(config);
	}

	public static ModsToml of(byte[] utf8) {
		return of(new String(utf8, StandardCharsets.UTF_8));
	}

	public static ModsToml of(String text) {
		try {
			return new ModsToml(new TomlParser().parse(text));
		} catch (ParsingException e) {
			throw ExceptionUtil.createDescriptiveWrapper(IllegalArgumentException::new, "Could not parse mods.toml", e);
		}
	}

	public static ModsToml of(Path path) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			return new ModsToml(new TomlParser().parse(reader));
		} catch (ParsingException e) {
			throw ExceptionUtil.createDescriptiveWrapper(IllegalArgumentException::new, "Could not parse mods.toml", e);
		}
	}

	public static ModsToml of(File file) throws IOException {
		return of(file.toPath());
	}

	@Override
	public Set<String> getIds() {
		final Optional<List<Config>> mods = config.getOptional("mods");
		if (mods.isEmpty()) return Set.of();

		final ImmutableSet.Builder<String> modIds = ImmutableSet.builder();

		for (final Config mod : mods.get()) {
			final Optional<String> modId = mod.getOptional("modId");
			modId.ifPresent(modIds::add);
		}

		return modIds.build();
	}

	@Override
	public Set<String> getAccessWideners() {
		return Set.of();
	}

	@Override
	public Set<String> getAccessTransformers(ModPlatform platform) {
		if (platform == ModPlatform.NEOFORGE) {
			final List<? extends Config> ats = config.get("accessTransformers");

			if (ats != null) {
				final Set<String> result = new HashSet<>();

				for (Config atEntry : ats) {
					final String file = atEntry.get("file");
					if (file != null) result.add(file);
				}

				return result;
			}
		}

		return Set.of();
	}

	@Override
	public List<InterfaceInjectionProcessor.InjectedInterface> getInjectedInterfaces(@Nullable String modId) {
		return List.of();
	}

	@Override
	public String getFileName() {
		return FILE_PATH;
	}

	@Override
	public List<String> getMixinConfigs() {
		return List.of();
	}

	@Override
	public boolean equals(Object obj) {
		return obj == this || obj instanceof ModsToml modsToml && modsToml.config.equals(config);
	}

	@Override
	public int hashCode() {
		return config.hashCode();
	}
}
