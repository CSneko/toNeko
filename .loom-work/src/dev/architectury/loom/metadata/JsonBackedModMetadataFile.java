package dev.architectury.loom.metadata;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

/**
 * A mod metadata file backed by a JSON object.
 */
public interface JsonBackedModMetadataFile extends ModMetadataFile {
	/**
	 * {@return the backing JSON object of this mod metadata file}.
	 */
	JsonObject getJson();

	/**
	 * Gets a custom value from this mod metadata file.
	 *
	 * <p>If the format doesn't support custom values,
	 * returns {@code null}.
	 *
	 * @param key the key of the custom value
	 * @return the custom value, or {@code null} if not found
	 */
	default @Nullable JsonElement getCustomValue(String key) {
		return null;
	}
}
