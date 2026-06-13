package org.cneko.toneko.fabric.entities;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biomes;
import org.cneko.toneko.common.mod.api.NekoNameRegistry;
import org.cneko.toneko.common.mod.api.NekoSkinRegistry;
import org.cneko.toneko.common.mod.entities.*;
import org.cneko.toneko.common.mod.entities.boss.mouflet.MoufletNekoBoss;
import org.cneko.toneko.common.util.ConfigUtil;

import java.util.Set;

import static org.cneko.toneko.common.Bootstrap.MODID;
import static org.cneko.toneko.common.mod.entities.ToNekoEntities.*;

public class ToNekoEntities {
    public static void init(){
        ADVENTURER_NEKO = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                ADVENTURER_NEKO_ID,
                FabricEntityType.Builder.createMob(AdventurerNeko::new, MobCategory.CREATURE, builder -> builder.defaultAttributes(AdventurerNeko::createAdventurerNekoAttributes)
                        ).
                        sized(0.5f,1.7f).eyeHeight(1.6f).build()
        );
        CRYSTAL_NEKO = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                CRYSTAL_NEKO_ID,
                FabricEntityType.Builder.createMob(CrystalNekoEntity::new, MobCategory.CREATURE, builder -> builder.defaultAttributes(CrystalNekoEntity::createNekoAttributes)
                        )
                        .sized(0.5f,1.7f).eyeHeight(1.6f).clientTrackingRange(8).build()
        );
        GHOST_NEKO = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                GHOST_NEKO_ID,
                FabricEntityType.Builder.createMob(GhostNekoEntity::new, MobCategory.CREATURE, builder -> builder.defaultAttributes(GhostNekoEntity::createGhostNekoAttributes)
                )
                        .sized(0.4f,1.2f).eyeHeight(1.5f).clientTrackingRange(8).build()
        );
        FIGHTING_NEKO  = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                FIGHTING_NEKO_ID,
                FabricEntityType.Builder.createMob(FightingNekoEntity::new, MobCategory.CREATURE, builder -> builder.defaultAttributes(FightingNekoEntity::createFightingNekoAttributes)
                        )
                        .sized(0.5f,1.7f).eyeHeight(1.6f).build()
        );
        MOUFLET_NEKO_BOSS = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                MOUFLET_NEKO_BOSS_ID,
                FabricEntityType.Builder.createMob(MoufletNekoBoss::new, MobCategory.MONSTER,builder -> builder.defaultAttributes(MoufletNekoBoss::createMoufletNekoAttributes))
                        .sized(0.5f,1.7f).clientTrackingRange(8).build()
        );
        RAVENN_ENTITY = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                RAVENN_ID,
                FabricEntityType.Builder.createMob(RavennEntity::new, MobCategory.MONSTER, builder -> builder.defaultAttributes(RavennEntity::createRavennAttributes))
                        .sized(0.5f,1.7f).clientTrackingRange(8).build()
        );
        NOELLE_MAID_NEKO = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                NOELLE_MAID_NEKO_ID,
                FabricEntityType.Builder.createMob(NoelleMaidNekoEntity::new, MobCategory.CREATURE, builder -> builder.defaultAttributes(NoelleMaidNekoEntity::createNoelleAttributes)
                        )
                        .sized(0.5f,1.7f).eyeHeight(1.6f).build()
        );

        AMMUNITION_ENTITY = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                AMMUNITION_ENTITY_ID,
                EntityType.Builder.of(AmmunitionEntity::new, MobCategory.MISC)
                        .sized(0.25f,0.25f).build()
        );

        org.cneko.toneko.common.mod.entities.ToNekoEntities.init();

        // 注册皮肤
        NekoSkinRegistry.register(ADVENTURER_NEKO,AdventurerNeko.nekoSkins);
        NekoSkinRegistry.register(GHOST_NEKO,GhostNekoEntity.nekoSkins);
        NekoSkinRegistry.register(FIGHTING_NEKO, FightingNekoEntity.NEKO_SKINS);
        NekoSkinRegistry.register(MOUFLET_NEKO_BOSS, MoufletNekoBoss.NEKO_SKINS);
        NekoSkinRegistry.register(NOELLE_MAID_NEKO, FightingNekoEntity.NEKO_SKINS);

        // 注册群系生成（委托 common 方法）
        registerBiomeSpawns(ADVENTURER_NEKO, GHOST_NEKO, CRYSTAL_NEKO, FIGHTING_NEKO, NOELLE_MAID_NEKO);
    }
}
