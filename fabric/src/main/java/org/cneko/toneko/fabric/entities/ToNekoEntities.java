package org.cneko.toneko.fabric.entities;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import org.cneko.toneko.common.mod.api.NekoNameRegistry;
import org.cneko.toneko.common.mod.api.NekoSkinRegistry;

import java.util.Set;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoEntities {
    private static final TagKey<Biome> IS_MOUNTAIN = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("c","is_mountain"));

    public static final EntityType<AdventurerNeko> ADVENTURER_NEKO = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID,"adventurer_neko"),
            FabricEntityType.Builder.createMob(AdventurerNeko::new, MobCategory.CREATURE, builder -> builder.defaultAttributes(AdventurerNeko::createAdventurerNekoAttributes)
                    .spawnRestriction(SpawnPlacementTypes.ON_GROUND, Heightmap.Types.WORLD_SURFACE, AdventurerNeko::checkMobSpawnRules)).sized(0.5f,1.35f).eyeHeight(1).build()
    );

    public static void init() {
        // 注册皮肤
        NekoSkinRegistry.register(ADVENTURER_NEKO,AdventurerNeko.nekoSkins);
        // 注册名字
        Set<String> names = Set.of(
                "Luna","Mochi","Poppy","Misty","Snowy","Coco","Peaches","Bubbles","Daisy","Cherry",
                "ひなた","もふこ","ちゃちゃまる","ひめにゃん",
                "Felicity","Purrin","Catrina","Fluffy","Meowgical","Felina","Ayame","Cinnamon","Momo"
        );
        NekoNameRegistry.register(names);

        // 设置生成条件
        BiomeModifications.addSpawn(BiomeSelectors.tag(IS_MOUNTAIN), MobCategory.CREATURE, ADVENTURER_NEKO, 5, 1, 1); // 在主世界的高山会生成一只
    }

}
