package org.cneko.toneko.neoforge.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.cneko.toneko.common.mod.api.NekoSkinRegistry;
import org.cneko.toneko.common.mod.entities.*;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.neoforge.ToNekoNeoForge;

import static org.cneko.toneko.common.mod.entities.ToNekoEntities.*;
public class ToNekoEntities {
    public static DeferredHolder<EntityType<?>, EntityType<CrystalNekoEntity>> CRYSTAL_NEKO_HOLDER;
    public static DeferredHolder<EntityType<?>, EntityType<AdventurerNeko>> ADVENTURER_NEKO_HOLDER;
    public static DeferredHolder<EntityType<?>, EntityType<GhostNekoEntity>> GHOST_NEKO_HOLDER;
    public static DeferredHolder<EntityType<?>, EntityType<FightingNekoEntity>> FIGHTING_NEKO_HOLDER;
    public static DeferredHolder<EntityType<?>, EntityType<AmmunitionEntity>> AMMUNITION_ENTITY_HOLDER;
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

        org.cneko.toneko.common.mod.entities.ToNekoEntities.init();

        // 注册皮肤
        NekoSkinRegistry.register("entity.toneko.adventurer_neko",AdventurerNeko.nekoSkins);
        NekoSkinRegistry.register("entity.toneko.ghost_neko",GhostNekoEntity.nekoSkins);
        NekoSkinRegistry.register("entity.toneko.fighting_neko",FightingNekoEntity.NEKO_SKINS);


    }

    @SubscribeEvent
    public static void onCreatureSpawn(RegisterSpawnPlacementsEvent event) {
        event.register(ADVENTURER_NEKO_HOLDER.get(), (EntityType<AdventurerNeko> type, ServerLevelAccessor accessor, MobSpawnType spawnType, BlockPos pos, RandomSource random) -> {
            // 检查是否是在高山生物群系
            if (accessor.getBiome(pos).is(BiomeTags.IS_MOUNTAIN)|| accessor.getBiome(pos).is(BiomeTags.IS_FOREST)) {
                // 控制生成概率，有20%的概率生成
                return random.nextFloat() < 0.2f;
            }
            return false; // 不在高山时不生成
        });
        event.register(GHOST_NEKO_HOLDER.get(), (EntityType<GhostNekoEntity> type, ServerLevelAccessor accessor, MobSpawnType spawnType, BlockPos pos, RandomSource random) -> {
            // 检查生物群系是否符合条件：允许史莱姆生成，或有丛林神庙
            if (accessor.getBiome(pos).is(BiomeTags.ALLOWS_SURFACE_SLIME_SPAWNS) || accessor.getBiome(pos).is(BiomeTags.HAS_JUNGLE_TEMPLE) || accessor.getBiome(pos).is(BiomeTags.HAS_NETHER_FOSSIL)) {
                // 控制生成概率，有15%的概率生成
                return random.nextFloat() < 0.15f;
            }
            return false; // 不符合条件时不生成
        });
        event.register(CRYSTAL_NEKO_HOLDER.get(), (EntityType<CrystalNekoEntity> type, ServerLevelAccessor accessor, MobSpawnType spawnType, BlockPos pos, RandomSource random) -> {
            // 如果是生日，则在所有生物群系中随机生成
            if (ConfigUtil.IS_BIRTHDAY) {
                // 控制生成概率，有10%的概率生成
                return random.nextFloat() < 0.1f;
            }
            return false; // 不是生日时不生成
        });

        event.register(FIGHTING_NEKO_HOLDER.get(), (EntityType<FightingNekoEntity> type, ServerLevelAccessor accessor, MobSpawnType spawnType, BlockPos pos, RandomSource random) -> {
            // 在地狱和古城5%生产
             return (accessor.getBiome(pos).is(BiomeTags.IS_NETHER) || accessor.getBiome(pos).is(BiomeTags.IS_END))&& random.nextFloat() < 0.05f;
        });


    }

    public static void reg(){
        CRYSTAL_NEKO = CRYSTAL_NEKO_HOLDER.get();
        ADVENTURER_NEKO = ADVENTURER_NEKO_HOLDER.get();
        GHOST_NEKO = GHOST_NEKO_HOLDER.get();
        FIGHTING_NEKO = FIGHTING_NEKO_HOLDER.get();
        AMMUNITION_ENTITY = AMMUNITION_ENTITY_HOLDER.get();
    }
}
