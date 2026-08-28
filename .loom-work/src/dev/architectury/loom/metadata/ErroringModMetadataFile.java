package dev.architectury.loom.metadata;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import net.fabricmc.loom.configuration.ifaceinject.InterfaceInjectionProcessor;
import net.fabricmc.loom.util.ModPlatform;

/**
 * A fallback mod metadata file that represents a non-fatal format error
 * in another file type. An example of such file type is mods.toml,
 * which doesn't necessarily need to be parsed &ndash; it primarily needs to just exist.
 */
@VisibleForTesting
public final class ErroringModMetadataFile implements ModMetadataFile {
	private final String fileName;

	ErroringModMetadataFile(String fileName) {
		this.fileName = fileName;
	}

	@Override
	public Set<String> getIds() {
		return Set.of();
	}

	@Override
	public Set<String> getAccessWideners() {
		return Set.of();
	}

	@Override
	public Set<String> getAccessTransformers(ModPlatform platform) {
		return Set.of();
	}

	@Override
	public List<InterfaceInjectionProcessor.InjectedInterface> getInjectedInterfaces(@Nullable String modId) {
		return List.of();
	}

	@Override
	public String getFileName() {
		return fileName + " [erroring]";
	}

	@Override
	public List<String> getMixinConfigs() {
		return List.of();
	}
}
