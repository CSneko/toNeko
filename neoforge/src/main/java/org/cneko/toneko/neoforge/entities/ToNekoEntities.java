package org.cneko.toneko.neoforge.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.cneko.toneko.common.mod.api.NekoSkinRegistry;
import org.cneko.toneko.common.mod.entities.AdventurerNeko;
import org.cneko.toneko.common.mod.entities.CrystalNekoEntity;
import org.cneko.toneko.common.mod.entities.GhostNekoEntity;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.neoforge.ToNekoNeoForge;

import static org.cneko.toneko.common.mod.entities.ToNekoEntities.*;
public class ToNekoEntities {
    public static DeferredHolder<EntityType<?>, EntityType<CrystalNekoEntity>> CRYSTAL_NEKO_HOLDER;
    public static DeferredHolder<EntityType<?>, EntityType<AdventurerNeko>> ADVENTURER_NEKO_HOLDER;
    public static DeferredHolder<EntityType<?>, EntityType<GhostNekoEntity>> GHOST_NEKO_HOLDER;
    public static void init(){
        CRYSTAL_NEKO_HOLDER = ToNekoNeoForge.ENTITY_TYPES.register("crystal_neko",
                org.cneko.toneko.common.mod.entities.ToNekoEntities.getCrystalNeko()
        );
        ADVENTURER_NEKO_HOLDER = ToNekoNeoForge.ENTITY_TYPES.register("adventurer_neko",
                org.cneko.toneko.common.mod.entities.ToNekoEntities.getAdventurerNeko()
        );
        GHOST_NEKO_HOLDER = ToNekoNeoForge.ENTITY_TYPES.register("ghost_neko",
                org.cneko.toneko.common.mod.entities.ToNekoEntities.getGhostNeko()
        );

        org.cneko.toneko.common.mod.entities.ToNekoEntities.init();

        // 注册皮肤
        NekoSkinRegistry.register("entity.toneko.adventurer_neko",AdventurerNeko.nekoSkins);
        NekoSkinRegistry.register("entity.toneko.ghost_neko",GhostNekoEntity.nekoSkins);



    }

    @SubscribeEvent
    public static void onCreatureSpawn(RegisterSpawnPlacementsEvent event) {
        event.register(ADVENTURER_NEKO_HOLDER.get(), (EntityType<AdventurerNeko> type, ServerLevelAccessor accessor, MobSpawnType spawnType, BlockPos pos, RandomSource random) -> {
            // 检查是否是在高山生物群系
            if (accessor.getBiome(pos).is(BiomeTags.IS_MOUNTAIN)) {
                // 控制生成概率，例如有20%的概率生成
                return random.nextFloat() < 0.2f;
            }
            return false; // 不在高山时不生成
        });
        event.register(GHOST_NEKO_HOLDER.get(), (EntityType<GhostNekoEntity> type, ServerLevelAccessor accessor, MobSpawnType spawnType, BlockPos pos, RandomSource random) -> {
            // 检查生物群系是否符合条件：允许史莱姆生成，或有丛林神庙
            if (accessor.getBiome(pos).is(BiomeTags.ALLOWS_SURFACE_SLIME_SPAWNS) || accessor.getBiome(pos).is(BiomeTags.HAS_JUNGLE_TEMPLE)) {
                // 控制生成概率，例如有15%的概率生成
                return random.nextFloat() < 0.15f;
            }
            return false; // 不符合条件时不生成
        });
        event.register(CRYSTAL_NEKO_HOLDER.get(), (EntityType<CrystalNekoEntity> type, ServerLevelAccessor accessor, MobSpawnType spawnType, BlockPos pos, RandomSource random) -> {
            // 如果是生日，则在所有生物群系中随机生成
            if (ConfigUtil.IS_BIRTHDAY) {
                // 控制生成概率，例如有10%的概率生成
                return random.nextFloat() < 0.1f;
            }
            return false; // 不是生日时不生成
        });


    }

    public static void reg(){
        CRYSTAL_NEKO = CRYSTAL_NEKO_HOLDER.get();
        ADVENTURER_NEKO = ADVENTURER_NEKO_HOLDER.get();
        GHOST_NEKO = GHOST_NEKO_HOLDER.get();
    }
}
