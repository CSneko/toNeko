package org.cneko.toneko.common.mod.worldgen;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;

public final class CoffeeWorldgen {
    private CoffeeWorldgen() {}
    public static void register() {
        BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_JUNGLE).or(BiomeSelectors.tag(BiomeTags.IS_FOREST)),
                GenerationStep.Decoration.VEGETAL_DECORATION,
                ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath("toneko", "patch_wild_coffee")));
    }
}
