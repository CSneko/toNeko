package org.cneko.toneko.fabric.entities;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import org.cneko.toneko.common.mod.api.NekoNameRegistry;
import org.cneko.toneko.common.mod.api.NekoSkinRegistry;
import org.cneko.toneko.common.mod.entities.*;
import org.cneko.toneko.common.mod.entities.boss.mouflet.MoufletNekoBoss;
import org.cneko.toneko.common.util.ConfigUtil;

import java.util.Set;

import static org.cneko.toneko.common.Bootstrap.MODID;
import static org.cneko.toneko.common.mod.entities.ToNekoEntities.*;

public class ToNekoEntities {
    private static ResourceKey<net.minecraft.world.entity.EntityType<?>> key(String path) {
        return ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MODID, path));
    }

    public static void init(){
        ADVENTURER_NEKO = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                ADVENTURER_NEKO_ID,
                EntityType.Builder.of(AdventurerNeko::new, MobCategory.CREATURE)
                        .sized(0.5f,1.7f).eyeHeight(1.6f).clientTrackingRange(8)
                        .build(key("adventurer_neko"))
        );
        FabricDefaultAttributeRegistry.register(ADVENTURER_NEKO, AdventurerNeko.createAdventurerNekoAttributes());
        CRYSTAL_NEKO = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                CRYSTAL_NEKO_ID,
                EntityType.Builder.of(CrystalNekoEntity::new, MobCategory.CREATURE)
                        .sized(0.5f,1.7f).eyeHeight(1.6f).clientTrackingRange(8)
                        .build(key("crystal_neko"))
        );
        FabricDefaultAttributeRegistry.register(CRYSTAL_NEKO, CrystalNekoEntity.createNekoAttributes());
        GHOST_NEKO = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                GHOST_NEKO_ID,
                EntityType.Builder.of(GhostNekoEntity::new, MobCategory.CREATURE)
                        .sized(0.4f,1.2f).eyeHeight(1.5f).clientTrackingRange(8)
                        .build(key("ghost_neko"))
        );
        FabricDefaultAttributeRegistry.register(GHOST_NEKO, GhostNekoEntity.createGhostNekoAttributes());
        FIGHTING_NEKO = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                FIGHTING_NEKO_ID,
                EntityType.Builder.of(FightingNekoEntity::new, MobCategory.CREATURE)
                        .sized(0.5f,1.7f).eyeHeight(1.6f).clientTrackingRange(8)
                        .build(key("fighting_neko"))
        );
        FabricDefaultAttributeRegistry.register(FIGHTING_NEKO, FightingNekoEntity.createFightingNekoAttributes());
        MOUFLET_NEKO_BOSS = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                MOUFLET_NEKO_BOSS_ID,
                EntityType.Builder.of(MoufletNekoBoss::new, MobCategory.MONSTER)
                        .sized(0.5f,1.7f).clientTrackingRange(8)
                        .build(key("mouflet_neko_boss"))
        );
        FabricDefaultAttributeRegistry.register(MOUFLET_NEKO_BOSS, MoufletNekoBoss.createMoufletNekoAttributes());
        RAVENN_ENTITY = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                RAVENN_ID,
                EntityType.Builder.of(RavennEntity::new, MobCategory.MONSTER)
                        .sized(0.5f,1.7f).clientTrackingRange(8)
                        .build(key("ravenn"))
        );
        FabricDefaultAttributeRegistry.register(RAVENN_ENTITY, RavennEntity.createRavennAttributes());
        NOELLE_MAID_NEKO = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                NOELLE_MAID_NEKO_ID,
                EntityType.Builder.of(NoelleMaidNekoEntity::new, MobCategory.CREATURE)
                        .sized(0.5f,1.7f).eyeHeight(1.6f).clientTrackingRange(8)
                        .build(key("noelle_maid_neko"))
        );
        FabricDefaultAttributeRegistry.register(NOELLE_MAID_NEKO, NoelleMaidNekoEntity.createNoelleAttributes());

        AMMUNITION_ENTITY = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                AMMUNITION_ENTITY_ID,
                EntityType.Builder.of(AmmunitionEntity::new, MobCategory.MISC)
                        .sized(0.25f,0.25f).build(key("ammunition_entity"))
        );
        FLY_SWORD_ENTITY = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                FLY_SWORD_ENTITY_ID,
                EntityType.Builder.of(FlySwordEntity::new, MobCategory.MISC)
                        .sized(0.6f, 0.6f).clientTrackingRange(10).build(key("fly_sword_entity"))
        );
        SEAT_ENTITY = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                SEAT_ENTITY_ID,
                EntityType.Builder.of(SeatEntity::new, MobCategory.MISC)
                        .sized(0.5f, 0.5f).clientTrackingRange(4).updateInterval(20).build(key("seat_entity"))
        );
        SPOILED_WATER_PROJECTILE_ENTITY = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                SPOILED_WATER_PROJECTILE_ENTITY_ID,
                EntityType.Builder.<SpoiledWaterProjectile>of((type, level) -> new SpoiledWaterProjectile(type, level), MobCategory.MISC)
                        .sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(20).build(key("spoiled_water_projectile_entity"))
        );

        org.cneko.toneko.common.mod.entities.ToNekoEntities.init();

        // 注册皮肤
        NekoSkinRegistry.register(ADVENTURER_NEKO,AdventurerNeko.nekoSkins);
        NekoSkinRegistry.register(GHOST_NEKO,GhostNekoEntity.nekoSkins);
        NekoSkinRegistry.register(FIGHTING_NEKO, FightingNekoEntity.NEKO_SKINS);
        NekoSkinRegistry.register(MOUFLET_NEKO_BOSS, MoufletNekoBoss.NEKO_SKINS);
        NekoSkinRegistry.register(NOELLE_MAID_NEKO, FightingNekoEntity.NEKO_SKINS);

        // 注册群系生成（委托 common 方法）
        registerBiomeSpawns(ADVENTURER_NEKO, GHOST_NEKO, CRYSTAL_NEKO, FIGHTING_NEKO);

        // 注册生成位置规则：必须站在实心方块上生成。
        // Fabric 没有像 NeoForge 那样的 RegisterSpawnPlacementsEvent，只能直接调用原版注册方法
        // （由 toneko.accesswidener 放开访问权限）。
        // 若不注册，SpawnPlacements.getPlacementType 会退化为 NO_RESTRICTIONS（任何位置都合法），
        // 自然生成时就会在下界这类空旷维度直接刷在半空中。
        registerSpawnPlacements();
    }

    /**
     * 注册猫娘的自然生成位置与判定规则（与 NeoForge 侧 {@code onCreatureSpawn} 保持一致）。
     */
    private static void registerSpawnPlacements() {
        SpawnPlacements.register(ADVENTURER_NEKO, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, NekoSpawnRules::canAdventurerSpawn);
        SpawnPlacements.register(GHOST_NEKO, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, NekoSpawnRules::canGhostSpawn);
        SpawnPlacements.register(CRYSTAL_NEKO, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, NekoSpawnRules::canCrystalSpawn);
        SpawnPlacements.register(FIGHTING_NEKO, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, NekoSpawnRules::canFightingSpawn);
        // 诺艾尔猫娘不注册：只能通过 /summon 召唤
    }
}
