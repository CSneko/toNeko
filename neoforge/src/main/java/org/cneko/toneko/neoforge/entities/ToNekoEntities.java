package org.cneko.toneko.neoforge.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.cneko.toneko.common.mod.api.NekoSkinRegistry;
import org.cneko.toneko.common.mod.entities.*;
import org.cneko.toneko.common.mod.entities.boss.mouflet.MoufletNekoBoss;
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
    public static DeferredHolder<EntityType<?>, EntityType<FlySwordEntity>> FLY_SWORD_HOLDER;
    public static DeferredHolder<EntityType<?>, EntityType<SeatEntity>> SEAT_ENTITY_HOLDER;
    public static DeferredHolder<EntityType<?>, EntityType<SpoiledWaterProjectile>> SPOILED_WATER_PROJECTILE_HOLDER;
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
        FLY_SWORD_HOLDER = ToNekoNeoForge.ENTITY_TYPES.register(FLY_SWORD_ENTITY_ID.getPath(),
                org.cneko.toneko.common.mod.entities.ToNekoEntities.getFlySwordEntity()
        );
        SEAT_ENTITY_HOLDER = ToNekoNeoForge.ENTITY_TYPES.register(SEAT_ENTITY_ID.getPath(),
                org.cneko.toneko.common.mod.entities.ToNekoEntities.getSeatEntity()
        );
        SPOILED_WATER_PROJECTILE_HOLDER = ToNekoNeoForge.ENTITY_TYPES.register(SPOILED_WATER_PROJECTILE_ENTITY_ID.getPath(),
                org.cneko.toneko.common.mod.entities.ToNekoEntities.getSpoiledWaterProjectileEntity()
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
        // 生成判定逻辑统一放在 common 的 NekoSpawnRules 中，与 Fabric 侧保持一致
        event.register(ADVENTURER_NEKO_HOLDER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                NekoSpawnRules::canAdventurerSpawn, RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(GHOST_NEKO_HOLDER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                NekoSpawnRules::canGhostSpawn, RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(CRYSTAL_NEKO_HOLDER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                NekoSpawnRules::canCrystalSpawn, RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(FIGHTING_NEKO_HOLDER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                NekoSpawnRules::canFightingSpawn, RegisterSpawnPlacementsEvent.Operation.OR);

        // 诺艾尔猫娘：不注册自然生成规则（无群系生成 + 无生成位置），
        // 只能通过 /summon toneko:noelle_maid_neko 命令召唤


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
        FLY_SWORD_ENTITY = FLY_SWORD_HOLDER.get();
        SEAT_ENTITY = SEAT_ENTITY_HOLDER.get();
        SPOILED_WATER_PROJECTILE_ENTITY = SPOILED_WATER_PROJECTILE_HOLDER.get();
    }
}
