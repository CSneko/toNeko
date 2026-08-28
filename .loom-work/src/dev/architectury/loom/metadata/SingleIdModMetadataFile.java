package dev.architectury.loom.metadata;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

interface SingleIdModMetadataFile extends ModMetadataFile {
	@Override
	default Set<String> getIds() {
		final String id = getId();
		return id != null ? Set.of(id) : Set.of();
	}

	@Override
	@Nullable String getId();
}
