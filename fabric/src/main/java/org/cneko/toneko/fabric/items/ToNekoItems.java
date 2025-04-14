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
import org.cneko.toneko.common.mod.blocks.ToNekoBlocks;
import org.cneko.toneko.common.mod.items.*;
import org.cneko.toneko.common.mod.entities.ToNekoEntities;
import org.cneko.toneko.common.mod.misc.ToNekoSongs;
import org.cneko.toneko.common.util.ConfigUtil;

import java.util.List;
import java.util.Optional;

import static org.cneko.toneko.common.mod.items.ToNekoItems.*;

import static org.cneko.toneko.common.Bootstrap.MODID;


public class ToNekoItems {
    public static NekoCollectorItem NEKO_COLLECTOR;
    public static ResourceKey<CreativeModeTab> TONEKO_ITEM_GROUP_KEY;
    public static CreativeModeTab TONEKO_ITEM_GROUP;
    public static final SpawnEggItem ADVENTURER_NEKO_SPAWN_EGG = new SpawnEggItem(ToNekoEntities.ADVENTURER_NEKO, 0x7e7e7e, 0xffffff,new Item.Properties());
    public static final SpawnEggItem GHOST_NEKO_SPAWN_EGG = new SpawnEggItem(ToNekoEntities.GHOST_NEKO, 0x7e7e7e, 0xffffff,new Item.Properties());
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
                new FoodProperties(2,1.0f,true,1.6f, Optional.empty(),
                        List.of()
                )));
        CATNIP_SANDWICH = new CatnipItem(new Item.Properties().component(DataComponents.FOOD,new FoodProperties(10,12f,false,1.6f, Optional.empty(),List.of())));
        CATNIP_SEED = new ItemNameBlockItem(ToNekoBlocks.CATNIP, new Item.Properties());
        MUSIC_DISC_KAWAII = new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ToNekoSongs.KAWAII));
        MUSIC_DISC_NEVER_GONNA_GIVE_YOU_UP = new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ToNekoSongs.NEVER_GONNA_GIVE_YOU_UP));
        BAZOOKA = new BazookaItem(new Item.Properties());
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, NekoPotionItem.ID), NEKO_POTION);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, NekoCollectorItem.ID), NEKO_COLLECTOR);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, FurryBoheItem.ID), FURRY_BOHE);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "adventurer_neko_spawn_egg"), ADVENTURER_NEKO_SPAWN_EGG);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "ghost_neko_spawn_egg"),GHOST_NEKO_SPAWN_EGG);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "catnip"), CATNIP);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "catnip_sandwich"), CATNIP_SANDWICH);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "catnip_seed"), CATNIP_SEED);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "music_disc_kawaii"), MUSIC_DISC_KAWAII);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "music_disc_never_gonna_give_you_up"), MUSIC_DISC_NEVER_GONNA_GIVE_YOU_UP);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, BazookaItem.ID), BAZOOKA);

        // 如果安装了trinkets，则注册为TrinketItem
        if (isTrinketsInstalled){
            NekoArmorTrinkets.init();
        }else {
            NEKO_EARS = new NekoArmor.NekoEarsItem(ToNekoArmorMaterials.NEKO);
            NEKO_TAIL = new NekoArmor.NekoTailItem(ToNekoArmorMaterials.NEKO);
            NEKO_PAWS = new NekoArmor.NekoPawsItem(ToNekoArmorMaterials.NEKO);
        }
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, NekoArmor.NekoEarsItem.ID), NEKO_EARS);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, NekoArmor.NekoTailItem.ID), NEKO_TAIL);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, NekoArmor.NekoPawsItem.ID), NEKO_PAWS);
        TONEKO_ITEM_GROUP = FabricItemGroup.builder()
                .icon(() -> new ItemStack(NEKO_EARS))
                .title(Component.translatable("itemGroup.toneko"))
                .build();
        // 注册物品组
        TONEKO_ITEM_GROUP_KEY = ResourceKey.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(), ResourceLocation.fromNamespaceAndPath(MODID, "item_group"));
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TONEKO_ITEM_GROUP_KEY,TONEKO_ITEM_GROUP);

        ItemGroupEvents.modifyEntriesEvent(TONEKO_ITEM_GROUP_KEY).register(content -> {
            content.accept(NEKO_POTION);
            content.accept(NEKO_COLLECTOR);
            content.accept(NEKO_EARS);
            content.accept(NEKO_TAIL);
            content.accept(FURRY_BOHE);
            content.accept(CATNIP);
            content.accept(CATNIP_SANDWICH);
            content.accept(CATNIP_SEED);
            content.accept(ADVENTURER_NEKO_SPAWN_EGG);
            content.accept(GHOST_NEKO_SPAWN_EGG);
            content.accept(MUSIC_DISC_KAWAII);
            if (ConfigUtil.IS_FOOL_DAY){
                content.accept(MUSIC_DISC_NEVER_GONNA_GIVE_YOU_UP);
            }
            content.accept(BAZOOKA);
        });
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
