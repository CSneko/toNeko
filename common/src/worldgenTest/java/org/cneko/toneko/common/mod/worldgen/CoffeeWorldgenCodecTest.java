package org.cneko.toneko.common.mod.worldgen;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.SharedConstants;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Decode the real resources with the target game's codecs, without starting a world or mod loader. */
public final class CoffeeWorldgenCodecTest {
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Path resources = Path.of(args[0]);
        JsonElement configuredJson = read(resources.resolve("configured_feature/patch_wild_coffee.json"));
        JsonElement placedJson = read(resources.resolve("placed_feature/patch_wild_coffee.json"));
        // Vanilla registries are frozen here. A vanilla fungus substitutes for the loader-registered
        // coffee block; feature types, placement codecs and the configured-feature reference stay real.
        replaceCoffeeBlock(configuredJson);
        replaceCoffeeBlock(placedJson);
        ConfiguredFeature<?, ?> configured = ConfiguredFeature.DIRECT_CODEC.parse(JsonOps.INSTANCE, configuredJson).getOrThrow();
        MappedRegistry<ConfiguredFeature<?, ?>> registry = new MappedRegistry<>(Registries.CONFIGURED_FEATURE, Lifecycle.stable());
        var key = ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.fromNamespaceAndPath("toneko", "patch_wild_coffee"));
        registry.register(key, configured, RegistrationInfo.BUILT_IN);
        RegistryAccess access = new RegistryAccess.ImmutableRegistryAccess(List.of(registry));
        PlacedFeature placed = PlacedFeature.DIRECT_CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, access), placedJson).getOrThrow();
        registry.freeze();
        if (placed.feature().value() != configured) throw new AssertionError("Coffee feature reference did not bind");
        System.out.println("Coffee worldgen: Minecraft configured/placed feature codecs and registry binding passed.");
    }

    private static JsonElement read(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path));
    }

    private static void replaceCoffeeBlock(JsonElement json) {
        if (json.isJsonObject()) {
            var object = json.getAsJsonObject();
            if (object.has("Name") && object.get("Name").getAsString().equals("toneko:wild_coffee")) {
                object.addProperty("Name", "minecraft:brown_mushroom");
            }
            object.entrySet().forEach(entry -> replaceCoffeeBlock(entry.getValue()));
        } else if (json.isJsonArray()) {
            json.getAsJsonArray().forEach(CoffeeWorldgenCodecTest::replaceCoffeeBlock);
        }
    }
}
