package com.crystalneko.tonekofabric.entity;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;

import static com.crystalneko.tonekofabric.ToNekoFabric.NEKO;
import static com.crystalneko.tonekofabric.ToNekoFabric.NEKO_SPAWN_EGG;

public class EntityRegister {
    public static void register() {
        NEKO = Registry.register(
                Registries.ENTITY_TYPE,
                new Identifier("toneko", "neko"),
                FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, nekoEntity::new).dimensions(EntityDimensions.fixed(0.6f, 2.0f)).build()
        );
        NEKO_SPAWN_EGG = new SpawnEggItem(NEKO, 0xc4c4c4, 0xadadad, new Item.Settings());
        //注册实体
        FabricDefaultAttributeRegistry.register(NEKO, nekoEntity.createMobAttributes());

        //设置实体刷新规则
        SpawnRestriction.register(NEKO, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, nekoEntity::canMobSpawn);
        BiomeModifications.addSpawn(BiomeSelectors.all(), SpawnGroup.CREATURE, NEKO, 5, 1, 3);

        Registry.register(Registries.ITEM, new Identifier("toneko", "neko_spawn_egg"), NEKO_SPAWN_EGG);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(content -> {
            content.add(NEKO_SPAWN_EGG);
        });
    }
}
