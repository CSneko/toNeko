package org.cneko.toneko.common.mod.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biomes;
import org.cneko.toneko.common.util.ConfigUtil;

/**
 * 猫娘的自然生成判定规则。
 *
 * <p>Fabric 与 NeoForge 两个平台注册 SpawnPlacements 时都使用这里的方法，
 * 保证两端生成概率一致。
 *
 * <p><b>重要：</b>这些规则只负责群系/概率判定，落地检测依赖平台侧注册的
 * {@code SpawnPlacementTypes.ON_GROUND} + {@code Heightmap.Types.MOTION_BLOCKING_NO_LEAVES}。
 * 若某个猫娘实体没有注册生成位置，{@code SpawnPlacements.getPlacementType} 会退化为
 * {@code NO_RESTRICTIONS}（任何位置都算合法），自然生成时就会在下界这类空旷维度
 * 直接刷在半空中，因此各平台必须注册 ON_GROUND 位置类型。
 */
public final class NekoSpawnRules {

    private NekoSpawnRules() {
    }

    /** 冒险猫娘：樱花林/花海/草甸 95% 超高概率，其余常见温和群系 65%。 */
    public static boolean canAdventurerSpawn(EntityType<AdventurerNeko> type, ServerLevelAccessor level,
                                             EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        // 樱花林、花海、草甸：95% 超高概率
        if (level.getBiome(pos).is(Biomes.CHERRY_GROVE)
                || level.getBiome(pos).is(Biomes.FLOWER_FOREST)
                || level.getBiome(pos).is(Biomes.SUNFLOWER_PLAINS)
                || level.getBiome(pos).is(Biomes.MEADOW)) {
            return random.nextFloat() < 0.95f;
        }
        // 广泛群系：65% 概率生成
        if (level.getBiome(pos).is(BiomeTags.IS_MOUNTAIN)
                || level.getBiome(pos).is(BiomeTags.IS_FOREST)
                || level.getBiome(pos).is(BiomeTags.IS_TAIGA)
                || level.getBiome(pos).is(BiomeTags.IS_JUNGLE)
                || level.getBiome(pos).is(BiomeTags.IS_SAVANNA)
                || level.getBiome(pos).is(Biomes.PLAINS)
                || level.getBiome(pos).is(BiomeTags.IS_RIVER)
                || level.getBiome(pos).is(BiomeTags.IS_BEACH)
                || level.getBiome(pos).is(BiomeTags.IS_HILL)) {
            return random.nextFloat() < 0.65f;
        }
        return false;
    }

    /** 幽灵猫娘：樱花林/黑森林/红树林沼泽 75%，其余神秘地带与森林针叶林 50%。 */
    public static boolean canGhostSpawn(EntityType<GhostNekoEntity> type, ServerLevelAccessor level,
                                        EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        // 樱花林、黑森林、红树林沼泽：75% 高概率
        if (level.getBiome(pos).is(Biomes.CHERRY_GROVE)
                || level.getBiome(pos).is(Biomes.DARK_FOREST)
                || level.getBiome(pos).is(Biomes.MANGROVE_SWAMP)) {
            return random.nextFloat() < 0.75f;
        }
        // 神秘地带 + 森林针叶林：50% 概率生成
        if (level.getBiome(pos).is(BiomeTags.ALLOWS_SURFACE_SLIME_SPAWNS)
                || level.getBiome(pos).is(BiomeTags.HAS_JUNGLE_TEMPLE)
                || level.getBiome(pos).is(BiomeTags.HAS_NETHER_FOSSIL)
                || level.getBiome(pos).is(BiomeTags.HAS_SWAMP_HUT)
                || level.getBiome(pos).is(BiomeTags.HAS_ANCIENT_CITY)
                || level.getBiome(pos).is(BiomeTags.IS_FOREST)
                || level.getBiome(pos).is(BiomeTags.IS_TAIGA)) {
            return random.nextFloat() < 0.5f;
        }
        return false;
    }

    /** 水晶猫娘：仅生日限定期间生成（花海 95%，其余群系 50%）。 */
    public static boolean canCrystalSpawn(EntityType<CrystalNekoEntity> type, ServerLevelAccessor level,
                                          EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (ConfigUtil.IS_BIRTHDAY) {
            // 樱花林、花海、草甸：95% 超高概率
            if (level.getBiome(pos).is(Biomes.CHERRY_GROVE)
                    || level.getBiome(pos).is(Biomes.FLOWER_FOREST)
                    || level.getBiome(pos).is(Biomes.MEADOW)) {
                return random.nextFloat() < 0.95f;
            }
            // 全群系：50% 概率生成
            return random.nextFloat() < 0.5f;
        }
        return false;
    }

    /** 战斗猫娘：地狱/古城/山地 45%。 */
    public static boolean canFightingSpawn(EntityType<FightingNekoEntity> type, ServerLevelAccessor level,
                                           EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        // 地狱、古城、山地：45% 概率生成
        return (level.getBiome(pos).is(BiomeTags.IS_NETHER)
                || level.getBiome(pos).is(BiomeTags.HAS_ANCIENT_CITY)
                || level.getBiome(pos).is(BiomeTags.IS_MOUNTAIN))
                && random.nextFloat() < 0.45f;
    }
}
