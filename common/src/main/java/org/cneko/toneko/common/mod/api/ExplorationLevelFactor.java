package org.cneko.toneko.common.mod.api;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public class ExplorationLevelFactor implements NekoLevelFactor {
    @Override
    public String getId() {
        return "exploration";
    }

    @Override
    public double getLevel(double rawValue) {
        double C = 200.0;
        return (Math.sqrt(1 + 8 * rawValue / C) - 1) / 2;
    }

    /**
     * 根据群系返回探索经验值。
     * 优先精确匹配原版群系，模组群系则通过 BiomeTags 智能分类。
     */
    public static double getBiomeXp(Holder<Biome> biomeHolder) {
        return biomeHolder.unwrapKey()
                .map(key -> getBiomeXp(key, biomeHolder))
                .orElse(100.0); // 无 key 的群系（极少见）兜底
    }

    /**
     * 先精确匹配原版群系 key，匹配不到则用 Tag 分类（适用于模组群系）。
     */
    private static double getBiomeXp(ResourceKey<Biome> key, Holder<Biome> biome) {
        // ==================== 末地 / 特殊 ====================
        if (key == Biomes.THE_END || key == Biomes.END_HIGHLANDS
                || key == Biomes.END_MIDLANDS || key == Biomes.SMALL_END_ISLANDS
                || key == Biomes.END_BARRENS) {
            return 1000.0;
        }
        if (key == Biomes.DEEP_DARK) {
            return 1000.0;
        }

        // ==================== 稀有 Overworld ====================
        if (key == Biomes.BADLANDS || key == Biomes.ERODED_BADLANDS
                || key == Biomes.WOODED_BADLANDS || key == Biomes.ICE_SPIKES
                || key == Biomes.MUSHROOM_FIELDS || key == Biomes.BAMBOO_JUNGLE
                || key == Biomes.FLOWER_FOREST || key == Biomes.OLD_GROWTH_BIRCH_FOREST
                || key == Biomes.OLD_GROWTH_PINE_TAIGA || key == Biomes.OLD_GROWTH_SPRUCE_TAIGA
                || key == Biomes.SUNFLOWER_PLAINS) {
            return 400.0;
        }

        // ==================== 下界 ====================
        if (key == Biomes.NETHER_WASTES || key == Biomes.CRIMSON_FOREST
                || key == Biomes.WARPED_FOREST || key == Biomes.SOUL_SAND_VALLEY
                || key == Biomes.BASALT_DELTAS) {
            return 600.0;
        }

        // ==================== 少见 Overworld ====================
        if (key == Biomes.DARK_FOREST || key == Biomes.BIRCH_FOREST
                || key == Biomes.SNOWY_PLAINS || key == Biomes.DESERT
                || key == Biomes.SAVANNA || key == Biomes.SAVANNA_PLATEAU
                || key == Biomes.WINDSWEPT_SAVANNA || key == Biomes.JUNGLE
                || key == Biomes.SPARSE_JUNGLE || key == Biomes.WINDSWEPT_HILLS
                || key == Biomes.WINDSWEPT_GRAVELLY_HILLS || key == Biomes.WINDSWEPT_FOREST
                || key == Biomes.STONY_SHORE || key == Biomes.CHERRY_GROVE
                || key == Biomes.SNOWY_TAIGA || key == Biomes.SNOWY_BEACH
                || key == Biomes.FROZEN_RIVER || key == Biomes.FROZEN_OCEAN
                || key == Biomes.DEEP_FROZEN_OCEAN || key == Biomes.COLD_OCEAN
                || key == Biomes.DEEP_COLD_OCEAN || key == Biomes.LUKEWARM_OCEAN
                || key == Biomes.DEEP_LUKEWARM_OCEAN || key == Biomes.WARM_OCEAN
                || key == Biomes.TAIGA || key == Biomes.GROVE
                || key == Biomes.JAGGED_PEAKS || key == Biomes.FROZEN_PEAKS
                || key == Biomes.STONY_PEAKS || key == Biomes.MEADOW
                || key == Biomes.DRIPSTONE_CAVES || key == Biomes.LUSH_CAVES
                || key == Biomes.MANGROVE_SWAMP) {
            return 200.0;
        }

        // ==================== 模组群系：Tag 分类 ====================
        // 以上都没匹配到 → 不是原版群系，按 BiomeTags 智能分类
        return getXpByTags(biome);
    }

    /**
     * 通过 BiomeTags 判断模组群系的稀有度。
     * 模组群系只要正确继承了原版的 biome tag，就能获得相应经验。
     */
    private static double getXpByTags(Holder<Biome> biome) {
        if (biome.is(BiomeTags.IS_END)) return 1000.0;
        if (biome.is(BiomeTags.IS_NETHER)) return 600.0;
        if (biome.is(BiomeTags.IS_BADLANDS)) return 400.0;
        if (biome.is(BiomeTags.IS_JUNGLE)) return 200.0;
        if (biome.is(BiomeTags.IS_MOUNTAIN)) return 200.0;
        // 其余：平原、森林、海洋、河流、海滩、沼泽等常见群系
        return 100.0;
    }
}
