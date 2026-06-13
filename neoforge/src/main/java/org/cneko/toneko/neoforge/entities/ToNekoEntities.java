package org.cneko.toneko.neoforge.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.cneko.toneko.common.mod.api.NekoSkinRegistry;
import org.cneko.toneko.common.mod.entities.*;
import org.cneko.toneko.common.mod.entities.boss.mouflet.MoufletNekoBoss;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.neoforge.ToNekoNeoForge;

import static org.cneko.toneko.common.mod.entities.ToNekoEntities.*;
public class ToNekoEntities {
    public static DeferredHolder<EntityType<?>, EntityType<CrystalNekoEntity>> CRYSTAL_NEKO_HOLDER;
    public static DeferredHolder<EntityType<?>, EntityType<AdventurerNeko>> ADVENTURER_NEKO_HOLDER;
    public static DeferredHolder<EntityType<?>, EntityType<GhostNekoEntity>> GHOST_NEKO_HOLDER;
    public static DeferredHolder<EntityType<?>, EntityType<FightingNekoEntity>> FIGHTING_NEKO_HOLDER;
    public static DeferredHolder<EntityType<?>, EntityType<AmmunitionEntity>> AMMUNITION_ENTITY_HOLDER;
    public static DeferredHolder<EntityType<?>, EntityType<MoufletNekoBoss>> MOUFLET_NEKO_BOSS_HOLDER;
    public static DeferredHolder<EntityType<?>, EntityType<RavennEntity>> RAVENN_HOLDER;
    public static DeferredHolder<EntityType<?>, EntityType<NoelleMaidNekoEntity>> NOELLE_MAID_NEKO_HOLDER;
    public static void init(){
        CRYSTAL_NEKO_HOLDER = ToNekoNeoForge.ENTITY_TYPES.register(CRYSTAL_NEKO_ID.getPath(),
                org.cneko.toneko.common.mod.entities.ToNekoEntities.getCrystalNeko()
        );
        ADVENTURER_NEKO_HOLDER = ToNekoNeoForge.ENTITY_TYPES.register(ADVENTURER_NEKO_ID.getPath(),
                org.cneko.toneko.common.mod.entities.ToNekoEntities.getAdventurerNeko()
        );
        GHOST_NEKO_HOLDER = ToNekoNeoForge.ENTITY_TYPES.register(GHOST_NEKO_ID.getPath(),
                org.cneko.toneko.common.mod.entities.ToNekoEntities.getGhostNeko()
        );
        FIGHTING_NEKO_HOLDER = ToNekoNeoForge.ENTITY_TYPES.register(FIGHTING_NEKO_ID.getPath(),
                org.cneko.toneko.common.mod.entities.ToNekoEntities.getFightingNeko()
        );
        AMMUNITION_ENTITY_HOLDER = ToNekoNeoForge.ENTITY_TYPES.register(AMMUNITION_ENTITY_ID.getPath(),
                org.cneko.toneko.common.mod.entities.ToNekoEntities.getAmmunitionEntity()
        );
        MOUFLET_NEKO_BOSS_HOLDER = ToNekoNeoForge.ENTITY_TYPES.register(MOUFLET_NEKO_BOSS_ID.getPath(),
                org.cneko.toneko.common.mod.entities.ToNekoEntities.getMoufletNekoBoss()
        );
        RAVENN_HOLDER = ToNekoNeoForge.ENTITY_TYPES.register(RAVENN_ID.getPath(),
                org.cneko.toneko.common.mod.entities.ToNekoEntities.getRavennEntity()
        );
        NOELLE_MAID_NEKO_HOLDER = ToNekoNeoForge.ENTITY_TYPES.register(NOELLE_MAID_NEKO_ID.getPath(),
                org.cneko.toneko.common.mod.entities.ToNekoEntities.getNoelleMaidNeko()
        );

        org.cneko.toneko.common.mod.entities.ToNekoEntities.init();

        // 注册皮肤
        NekoSkinRegistry.register("entity.toneko.adventurer_neko",AdventurerNeko.nekoSkins);
        NekoSkinRegistry.register("entity.toneko.ghost_neko",GhostNekoEntity.nekoSkins);
        NekoSkinRegistry.register("entity.toneko.fighting_neko",FightingNekoEntity.NEKO_SKINS);
        NekoSkinRegistry.register("entity.toneko.mouflet_neko_boss",MoufletNekoBoss.NEKO_SKINS);
        NekoSkinRegistry.register("entity.toneko.noelle_maid_neko",FightingNekoEntity.NEKO_SKINS);


    }

    @SubscribeEvent
    public static void onCreatureSpawn(RegisterSpawnPlacementsEvent event) {
        event.register(ADVENTURER_NEKO_HOLDER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (EntityType<AdventurerNeko> type, ServerLevelAccessor accessor, MobSpawnType spawnType, BlockPos pos, RandomSource random) -> {
            // 樱花林、花海、草甸：95% 超高概率
            if (accessor.getBiome(pos).is(Biomes.CHERRY_GROVE)
                    || accessor.getBiome(pos).is(Biomes.FLOWER_FOREST)
                    || accessor.getBiome(pos).is(Biomes.SUNFLOWER_PLAINS)
                    || accessor.getBiome(pos).is(Biomes.MEADOW)) {
                return random.nextFloat() < 0.95f;
            }
            // 广泛群系：65% 概率生成
            if (accessor.getBiome(pos).is(BiomeTags.IS_MOUNTAIN)
                    || accessor.getBiome(pos).is(BiomeTags.IS_FOREST)
                    || accessor.getBiome(pos).is(BiomeTags.IS_TAIGA)
                    || accessor.getBiome(pos).is(BiomeTags.IS_JUNGLE)
                    || accessor.getBiome(pos).is(BiomeTags.IS_SAVANNA)
                    || accessor.getBiome(pos).is(Biomes.PLAINS)
                    || accessor.getBiome(pos).is(BiomeTags.IS_RIVER)
                    || accessor.getBiome(pos).is(BiomeTags.IS_BEACH)
                    || accessor.getBiome(pos).is(BiomeTags.IS_HILL)) {
                return random.nextFloat() < 0.65f;
            }
            return false;
        }, RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(GHOST_NEKO_HOLDER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (EntityType<GhostNekoEntity> type, ServerLevelAccessor accessor, MobSpawnType spawnType, BlockPos pos, RandomSource random) -> {
            // 樱花林、黑森林、红树林沼泽：75% 高概率
            if (accessor.getBiome(pos).is(Biomes.CHERRY_GROVE)
                    || accessor.getBiome(pos).is(Biomes.DARK_FOREST)
                    || accessor.getBiome(pos).is(Biomes.MANGROVE_SWAMP)) {
                return random.nextFloat() < 0.75f;
            }
            // 神秘地带 + 森林针叶林：50% 概率生成
            if (accessor.getBiome(pos).is(BiomeTags.ALLOWS_SURFACE_SLIME_SPAWNS)
                    || accessor.getBiome(pos).is(BiomeTags.HAS_JUNGLE_TEMPLE)
                    || accessor.getBiome(pos).is(BiomeTags.HAS_NETHER_FOSSIL)
                    || accessor.getBiome(pos).is(BiomeTags.HAS_SWAMP_HUT)
                    || accessor.getBiome(pos).is(BiomeTags.HAS_ANCIENT_CITY)
                    || accessor.getBiome(pos).is(BiomeTags.IS_FOREST)
                    || accessor.getBiome(pos).is(BiomeTags.IS_TAIGA)) {
                return random.nextFloat() < 0.5f;
            }
            return false;
        }, RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(CRYSTAL_NEKO_HOLDER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (EntityType<CrystalNekoEntity> type, ServerLevelAccessor accessor, MobSpawnType spawnType, BlockPos pos, RandomSource random) -> {
            if (ConfigUtil.IS_BIRTHDAY) {
                // 樱花林、花海、草甸：95% 超高概率
                if (accessor.getBiome(pos).is(Biomes.CHERRY_GROVE)
                        || accessor.getBiome(pos).is(Biomes.FLOWER_FOREST)
                        || accessor.getBiome(pos).is(Biomes.MEADOW)) {
                    return random.nextFloat() < 0.95f;
                }
                // 全群系：50% 概率生成
                return random.nextFloat() < 0.5f;
            }
            return false;
        }, RegisterSpawnPlacementsEvent.Operation.OR);

        event.register(FIGHTING_NEKO_HOLDER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (EntityType<FightingNekoEntity> type, ServerLevelAccessor accessor, MobSpawnType spawnType, BlockPos pos, RandomSource random) -> {
            // 地狱、古城、山地：45% 概率生成
             return (accessor.getBiome(pos).is(BiomeTags.IS_NETHER)
                     || accessor.getBiome(pos).is(BiomeTags.HAS_ANCIENT_CITY)
                     || accessor.getBiome(pos).is(BiomeTags.IS_MOUNTAIN))
                     && random.nextFloat() < 0.45f;
        }, RegisterSpawnPlacementsEvent.Operation.OR);

        event.register(NOELLE_MAID_NEKO_HOLDER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (EntityType<NoelleMaidNekoEntity> type, ServerLevelAccessor accessor, MobSpawnType spawnType, BlockPos pos, RandomSource random) -> {
            // 樱花林、花海、草甸、向日葵平原：85% 高概率
            if (accessor.getBiome(pos).is(Biomes.CHERRY_GROVE)
                    || accessor.getBiome(pos).is(Biomes.FLOWER_FOREST)
                    || accessor.getBiome(pos).is(Biomes.MEADOW)
                    || accessor.getBiome(pos).is(Biomes.SUNFLOWER_PLAINS)) {
                return random.nextFloat() < 0.85f;
            }
            // 森林、平原、河流：50% 概率生成
            if (accessor.getBiome(pos).is(BiomeTags.IS_FOREST)
                    || accessor.getBiome(pos).is(Biomes.PLAINS)
                    || accessor.getBiome(pos).is(BiomeTags.IS_RIVER)) {
                return random.nextFloat() < 0.5f;
            }
            return false;
        }, RegisterSpawnPlacementsEvent.Operation.OR);


    }

    public static void reg(){
        CRYSTAL_NEKO = CRYSTAL_NEKO_HOLDER.get();
        ADVENTURER_NEKO = ADVENTURER_NEKO_HOLDER.get();
        GHOST_NEKO = GHOST_NEKO_HOLDER.get();
        FIGHTING_NEKO = FIGHTING_NEKO_HOLDER.get();
        AMMUNITION_ENTITY = AMMUNITION_ENTITY_HOLDER.get();
        MOUFLET_NEKO_BOSS = MOUFLET_NEKO_BOSS_HOLDER.get();
        RAVENN_ENTITY = RAVENN_HOLDER.get();
        NOELLE_MAID_NEKO = NOELLE_MAID_NEKO_HOLDER.get();
    }
}
