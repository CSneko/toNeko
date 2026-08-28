package dev.architectury.loom.metadata;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.loom.LoomGradlePlugin;
import net.fabricmc.loom.configuration.ifaceinject.InterfaceInjectionProcessor;
import net.fabricmc.loom.util.ModPlatform;
import net.fabricmc.loom.util.function.CollectionUtil;

public final class QuiltModJson implements JsonBackedModMetadataFile, SingleIdModMetadataFile {
	public static final String FILE_NAME = "quilt.mod.json";
	private static final Logger LOGGER = LoggerFactory.getLogger(QuiltModJson.class);
	private static final String ACCESS_WIDENER_KEY = "access_widener";
	private static final String MIXIN_KEY = "mixin";

	private final JsonObject json;

	private QuiltModJson(JsonObject json) {
		this.json = Objects.requireNonNull(json, "json");
	}

	public static QuiltModJson of(byte[] utf8) {
		return of(new String(utf8, StandardCharsets.UTF_8));
	}

	public static QuiltModJson of(String text) {
		return of(LoomGradlePlugin.GSON.fromJson(text, JsonObject.class));
	}

	public static QuiltModJson of(Path path) throws IOException {
		return of(Files.readString(path, StandardCharsets.UTF_8));
	}

	public static QuiltModJson of(File file) throws IOException {
		return of(file.toPath());
	}

	public static QuiltModJson of(JsonObject json) {
		return new QuiltModJson(json);
	}

	@Override
	public JsonObject getJson() {
		return json;
	}

	@Override
	public @Nullable String getId() {
		JsonObject quiltLoader = json.getAsJsonObject("quilt_loader");

		if (quiltLoader != null && quiltLoader.has("id")) {
			return quiltLoader.get("id").getAsString();
		}

		return null;
	}

	@Override
	public Set<String> getAccessWideners() {
		if (json.has(ACCESS_WIDENER_KEY)) {
			if (json.get(ACCESS_WIDENER_KEY).isJsonArray()) {
				JsonArray array = json.get(ACCESS_WIDENER_KEY).getAsJsonArray();
				return CollectionUtil.mapTo(array, new LinkedHashSet<>(), JsonElement::getAsString);
			} else {
				return Set.of(json.get(ACCESS_WIDENER_KEY).getAsString());
			}
		} else {
			return Set.of();
		}
	}

	@Override
	public Set<String> getAccessTransformers(ModPlatform platform) {
		return Set.of();
	}

	@Override
	public List<InterfaceInjectionProcessor.InjectedInterface> getInjectedInterfaces(@Nullable String modId) {
		try {
			modId = Objects.requireNonNullElse(getId(), modId);
		} catch (NullPointerException e) {
			throw new IllegalArgumentException("Could not determine mod ID for Quilt mod and no fallback provided", e);
		}

		// Quilt injected interfaces have the same format as architectury.common.json
		if (json.has("quilt_loom")) {
			JsonElement quiltLoom = json.get("quilt_loom");

			if (quiltLoom.isJsonObject()) {
				return ArchitecturyCommonJson.getInjectedInterfaces(json.getAsJsonObject("quilt_loom"), modId);
			} else {
				LOGGER.warn("Unexpected type for 'quilt_loom' in quilt.mod.json: {}", quiltLoom.getClass());
			}
		}

		return List.of();
	}

	@Override
	public List<String> getMixinConfigs() {
		// RFC 0002: The `mixin` field:
		//   Type: Array/String
		//   Required: False

		if (json.has(MIXIN_KEY)) {
			JsonElement mixin = json.get(MIXIN_KEY);

			if (mixin.isJsonPrimitive()) {
				return List.of(mixin.getAsString());
			} else if (mixin.isJsonArray()) {
				List<String> mixinConfigs = new ArrayList<>();

				for (JsonElement child : mixin.getAsJsonArray()) {
					mixinConfigs.add(child.getAsString());
				}

				return mixinConfigs;
			} else {
				LOGGER.warn("'mixin' key in quilt.mod.json is of unexpected type {}", mixin.getClass());
			}
		}

		return List.of();
	}

	/**
	 * {@return {@value #FILE_NAME}}.
	 */
	@Override
	public String getFileName() {
		return FILE_NAME;
	}

	@Override
	public @Nullable JsonElement getCustomValue(String key) {
		// Quilt Loader allows reading any field from the quilt.mod.json as
		// custom values (under the name "loader values").
		// See https://github.com/QuiltMC/quilt-loader/blob/7da975c7/src/main/java/org/quiltmc/loader/api/ModMetadata.java#L150-L152
		return json.get(key);
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || obj instanceof QuiltModJson qmj && qmj.json.equals(json);
	}

	@Override
	public int hashCode() {
		return json.hashCode();
	}
}
