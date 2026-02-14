package org.cneko.toneko.fabric.items;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import org.cneko.toneko.common.mod.blocks.ToNekoBlocks;
import org.cneko.toneko.common.mod.items.*;
import org.cneko.toneko.common.mod.entities.ToNekoEntities;
import org.cneko.toneko.common.mod.items.ammo.ExplosiveBombItem;
import org.cneko.toneko.common.mod.items.ammo.LightningBombItem;
import org.cneko.toneko.common.mod.misc.ToNekoSongs;
import org.cneko.toneko.common.util.ConfigUtil;

import java.util.List;
import java.util.Optional;

import static org.cneko.toneko.common.mod.items.ToNekoItems.*;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;


public class ToNekoItems {
    public static NekoCollectorItem NEKO_COLLECTOR;
    public static ResourceKey<CreativeModeTab> TONEKO_ITEM_GROUP_KEY;
    public static CreativeModeTab TONEKO_ITEM_GROUP;
    public static final SpawnEggItem ADVENTURER_NEKO_SPAWN_EGG = new SpawnEggItem(ToNekoEntities.ADVENTURER_NEKO, 0x7e7e7e, 0xffffff,new Item.Properties());
    public static final SpawnEggItem GHOST_NEKO_SPAWN_EGG = new SpawnEggItem(ToNekoEntities.GHOST_NEKO, 0x7e7e7e, 0xffffff,new Item.Properties());
    public static final SpawnEggItem FIGHTING_NEKO_SPAWN_EGG = new SpawnEggItem(ToNekoEntities.FIGHTING_NEKO, 0x7e7e7e, 0xffffff,new Item.Properties());
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
        PLOT_SCROLL = new PlotScrollItem(new Item.Properties());
        LIGHTNING_BOMB = new LightningBombItem(new Item.Properties());
        EXPLOSIVE_BOMB  = new ExplosiveBombItem(new Item.Properties());
        CONTRACT = new ContractItem(new Item.Properties());
        NEKO_AGGREGATOR_ITEM = new ItemNameBlockItem(ToNekoBlocks.NEKO_AGGREGATOR, new Item.Properties());
        NEKO_INGOT = new Item(new Item.Properties());
        NEKO_BLOCK = new BlockItem(ToNekoBlocks.NEKO_BLOCK, new Item.Properties());
        NEKO_DIAMOND = new Item(new Item.Properties());
        NEKO_DIAMOND_BLOCK = new BlockItem(ToNekoBlocks.NEKO_DIAMOND_BLOCK, new Item.Properties());
        NEKO_CRYSTAL = new Item(new Item.Properties());
        NEKO_ENERGY_STORAGE_SMALL = new NekoEnergyStorageItem(150,false);
        NEKO_ENERGY_STORAGE_SMALL_CHARGED = new NekoEnergyStorageItem(150,true);
        NEKO_ENERGY_STORAGE_MEDIUM = new NekoEnergyStorageItem(400,false);
        NEKO_ENERGY_STORAGE_MEDIUM_CHARGED = new NekoEnergyStorageItem(400,true);
        NEKO_ENERGY_STORAGE_LARGE = new NekoEnergyStorageItem(1000,false);
        NEKO_ENERGY_STORAGE_LARGE_CHARGED = new NekoEnergyStorageItem(1000,true);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(NekoPotionItem.ID), NEKO_POTION);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(NekoCollectorItem.ID), NEKO_COLLECTOR);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(FurryBoheItem.ID), FURRY_BOHE);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("adventurer_neko_spawn_egg"), ADVENTURER_NEKO_SPAWN_EGG);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("ghost_neko_spawn_egg"),GHOST_NEKO_SPAWN_EGG);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("fighting_neko_spawn_egg"), FIGHTING_NEKO_SPAWN_EGG);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("catnip"), CATNIP);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("catnip_sandwich"), CATNIP_SANDWICH);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("catnip_seed"), CATNIP_SEED);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("music_disc_kawaii"), MUSIC_DISC_KAWAII);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("music_disc_never_gonna_give_you_up"), MUSIC_DISC_NEVER_GONNA_GIVE_YOU_UP);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(BazookaItem.ID), BAZOOKA);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("plot_scroll"), PLOT_SCROLL);
        Registry .register(BuiltInRegistries.ITEM, toNekoLoc("lightning_bomb"), LIGHTNING_BOMB);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("explosive_bomb"), EXPLOSIVE_BOMB);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("contract"), CONTRACT);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("neko_aggregator"), NEKO_AGGREGATOR_ITEM);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("neko_ingot"), NEKO_INGOT);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("neko_block"), NEKO_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("neko_diamond"), NEKO_DIAMOND);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("neko_diamond_block"), NEKO_DIAMOND_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("neko_crystal"), NEKO_CRYSTAL);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("neko_energy_storage_small"), NEKO_ENERGY_STORAGE_SMALL);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("neko_energy_storage_small_charged"), NEKO_ENERGY_STORAGE_SMALL_CHARGED);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("neko_energy_storage_medium"), NEKO_ENERGY_STORAGE_MEDIUM);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("neko_energy_storage_medium_charged"), NEKO_ENERGY_STORAGE_MEDIUM_CHARGED);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("neko_energy_storage_large"), NEKO_ENERGY_STORAGE_LARGE);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("neko_energy_storage_large_charged"), NEKO_ENERGY_STORAGE_LARGE_CHARGED);

        // 如果安装了trinkets，则注册为TrinketItem
        if (isTrinketsInstalled){
            NekoArmorTrinkets.init();
        }else {
            NEKO_EARS = new NekoArmor.NekoEarsItem(ToNekoArmorMaterials.NEKO);
            NEKO_TAIL = new NekoArmor.NekoTailItem(ToNekoArmorMaterials.NEKO);
            NEKO_PAWS = new NekoArmor.NekoPawsItem(ToNekoArmorMaterials.NEKO);
        }
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(NekoArmor.NekoEarsItem.ID), NEKO_EARS);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(NekoArmor.NekoTailItem.ID), NEKO_TAIL);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(NekoArmor.NekoPawsItem.ID), NEKO_PAWS);
        TONEKO_ITEM_GROUP = FabricItemGroup.builder()
                .icon(() -> new ItemStack(NEKO_EARS))
                .title(Component.translatable("itemGroup.toneko"))
                .build();
        // 注册物品组
        TONEKO_ITEM_GROUP_KEY = ResourceKey.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(), toNekoLoc("item_group"));
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
            content.accept(FIGHTING_NEKO_SPAWN_EGG);
            content.accept(MUSIC_DISC_KAWAII);
            if (ConfigUtil.IS_FOOL_DAY){
                content.accept(MUSIC_DISC_NEVER_GONNA_GIVE_YOU_UP);
            }
            content.accept(BAZOOKA);
            // content.accept(PLOT_SCROLL);
            content.accept(LIGHTNING_BOMB);
            content.accept(EXPLOSIVE_BOMB);
            content.accept(CONTRACT);
            content.accept(NEKO_AGGREGATOR_ITEM);
            content.accept(NEKO_INGOT);
            content.accept(NEKO_BLOCK);
            content.accept(NEKO_DIAMOND);
            content.accept(NEKO_DIAMOND_BLOCK);
            content.accept(NEKO_CRYSTAL);
            content.accept(NEKO_ENERGY_STORAGE_SMALL);
            content.accept(NEKO_ENERGY_STORAGE_SMALL_CHARGED);
            content.accept(NEKO_ENERGY_STORAGE_MEDIUM);
            content.accept(NEKO_ENERGY_STORAGE_MEDIUM_CHARGED);
            content.accept(NEKO_ENERGY_STORAGE_LARGE);
            content.accept(NEKO_ENERGY_STORAGE_LARGE_CHARGED);
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
