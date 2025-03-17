package org.cneko.toneko.fabric.items;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import org.cneko.toneko.common.mod.blocks.ToNekoBlocks;
import org.cneko.toneko.common.mod.items.*;
import org.cneko.toneko.common.mod.entities.ToNekoEntities;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.cneko.toneko.common.mod.items.ToNekoItems.*;

import static org.cneko.toneko.common.Bootstrap.MODID;


public class ToNekoItems {
    public static NekoCollectorItem NEKO_COLLECTOR;
    public static ResourceKey<CreativeModeTab> TONEKO_ITEM_GROUP_KEY;
    public static CreativeModeTab TONEKO_ITEM_GROUP;
    public static final SpawnEggItem ADVENTURER_NEKO_SPAWN_EGG = new SpawnEggItem(ToNekoEntities.ADVENTURER_NEKO, new Item.Properties());
    public static final SpawnEggItem GHOST_NEKO_SPAWN_EGG = new SpawnEggItem(ToNekoEntities.GHOST_NEKO, new Item.Properties());
    public static boolean isTrinketsInstalled = tryClass("dev.emi.trinkets.api.Trinket");
    public static void init() {
        registerWithOutConfig();
    }

    /**
     * 强制注册物品，无论配置文件如何设置
     */
    public static void registerWithOutConfig() {
        NEKO_POTION = new NekoPotionItem();
        NEKO_COLLECTOR = new NekoCollectorItem();
        FURRY_BOHE = new FurryBoheItem();
        CATNIP = new CatnipItem(new Item.Properties().component(DataComponents.FOOD,
                new FoodProperties(2,1.0f,true)));
        CATNIP_SANDWICH = new CatnipItem(new Item.Properties().component(DataComponents.FOOD,new FoodProperties(10,12f,false)));
        CATNIP_SEED = createBlockItemWithCustomItemName(ToNekoBlocks.CATNIP).apply(new Item.Properties());
        MUSIC_DISC_KAWAII = new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ToNekoSongs.KAWAII));
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, NekoPotionItem.ID), NEKO_POTION);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, NekoCollectorItem.ID), NEKO_COLLECTOR);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, FurryBoheItem.ID), FURRY_BOHE);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "adventurer_neko_spawn_egg"), ADVENTURER_NEKO_SPAWN_EGG);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "ghost_neko_spawn_egg"),GHOST_NEKO_SPAWN_EGG);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "catnip"), CATNIP);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "catnip_sandwich"), CATNIP_SANDWICH);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "catnip_seed"), CATNIP_SEED);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "music_disc_kawaii"), MUSIC_DISC_KAWAII);

        // 注册物品组
        TONEKO_ITEM_GROUP_KEY = ResourceKey.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(), ResourceLocation.fromNamespaceAndPath(MODID, "item_group"));
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TONEKO_ITEM_GROUP_KEY,TONEKO_ITEM_GROUP);

        ItemGroupEvents.modifyEntriesEvent(TONEKO_ITEM_GROUP_KEY).register(content -> {
            content.accept(NEKO_POTION);
            content.accept(NEKO_COLLECTOR);
            content.accept(FURRY_BOHE);
            content.accept(CATNIP);
            content.accept(CATNIP_SANDWICH);
            content.accept(CATNIP_SEED);
            content.accept(ADVENTURER_NEKO_SPAWN_EGG);
            content.accept(GHOST_NEKO_SPAWN_EGG);
            content.accept(MUSIC_DISC_KAWAII);
        });
    }

    private static Function<Item.Properties, Item> createBlockItemWithCustomItemName(Block block) {
        return (properties) -> {
            return new BlockItem(block, properties.useItemDescriptionPrefix());
        };
    }

    public static boolean tryClass(String clazz){
        try {
            Class.forName(clazz);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}
