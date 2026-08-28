package dev.architectury.loom.metadata;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.loom.configuration.ifaceinject.InterfaceInjectionProcessor;
import net.fabricmc.loom.util.ModPlatform;
import net.fabricmc.loom.util.function.CollectionUtil;

/**
 * The metadata file of a mod, such as {@link ArchitecturyCommonJson architectury.common.json} or
 * {@link QuiltModJson quilt.mod.json}.
 *
 * @see net.fabricmc.loom.util.fmj.FabricModJson
 */
public interface ModMetadataFile {
	/**
	 * {@return all mod IDs in this mod metadata file}.
	 */
	Set<String> getIds();

	/**
	 * {@return the mod ID in this mod metadata file, or {@code null} if absent}.
	 */
	default @Nullable String getId() {
		return CollectionUtil.first(getIds()).orElse(null);
	}

	/**
	 * {@return the paths to the access widener file of this mod, or an empty set if absent}.
	 */
	Set<String> getAccessWideners();

	/**
	 * {@return the paths to the access transformer files of this mod, or an empty set if absent}.
	 *
	 * @param platform the platform to run the query on
	 */
	Set<String> getAccessTransformers(ModPlatform platform);

	/**
	 * {@return the injected interface data in this mod metadata file}.
	 *
	 * @param modId the mod ID to use as a fallback if {@link #getId} returns {@code null}
	 * @throws IllegalArgumentException if both {@code modId} and {@link #getId} are {@code null}
	 */
	List<InterfaceInjectionProcessor.InjectedInterface> getInjectedInterfaces(@Nullable String modId);

	/**
	 * {@return the file name of this mod metadata file}.
	 */
	String getFileName();

	/**
	 * {@return a list of the mixin configs declared in this mod metadata file}.
	 */
	List<String> getMixinConfigs();
}
