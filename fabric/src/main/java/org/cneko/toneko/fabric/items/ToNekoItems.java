package org.cneko.toneko.fabric.items;

import org.cneko.toneko.common.mod.util.NekoIds;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.Consumables;
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
import org.cneko.toneko.common.mod.items.ammo.NekoEnergyBombItem;
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
    public static final SpawnEggItem ADVENTURER_NEKO_SPAWN_EGG = new SpawnEggItem(NekoIds.itemProps("adventurer_neko_spawn_egg").spawnEgg(org.cneko.toneko.common.mod.entities.ToNekoEntities.ADVENTURER_NEKO));
    public static final SpawnEggItem GHOST_NEKO_SPAWN_EGG = new SpawnEggItem(NekoIds.itemProps("ghost_neko_spawn_egg").spawnEgg(org.cneko.toneko.common.mod.entities.ToNekoEntities.GHOST_NEKO));
    public static final SpawnEggItem FIGHTING_NEKO_SPAWN_EGG = new SpawnEggItem(NekoIds.itemProps("fighting_neko_spawn_egg").spawnEgg(org.cneko.toneko.common.mod.entities.ToNekoEntities.FIGHTING_NEKO));
    // 26.x 迁移说明：Trinkets 尚无 26.x（去混淆版本）发布，集成类已暂时移除，恒走原版路径。
    public static boolean isTrinketsInstalled = false;
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
        CATNIP = new CatnipItem(NekoIds.itemProps("catnip").component(DataComponents.FOOD,
                new FoodProperties(2, 1.0f, true)).component(DataComponents.CONSUMABLE, Consumables.DEFAULT_FOOD));
        INFINITE_CATNIP = new CatnipItem.InfiniteCatnipItem(NekoIds.itemProps("infinite_catnip").component(DataComponents.FOOD,new FoodProperties(2, 1.0f, false)).component(DataComponents.CONSUMABLE, Consumables.DEFAULT_FOOD).rarity(Rarity.UNCOMMON));
        CATNIP_SANDWICH = new CatnipItem(NekoIds.itemProps("catnip_sandwich").component(DataComponents.FOOD,new FoodProperties(10, 12f, false)).component(DataComponents.CONSUMABLE, Consumables.DEFAULT_FOOD));
        CATNIP_SEED = new BlockItem(ToNekoBlocks.CATNIP, NekoIds.itemProps("catnip_seed"));
        WILD_CATNIP = new BlockItem(ToNekoBlocks.WILD_CATNIP, NekoIds.itemProps("wild_catnip"));
        MUSIC_DISC_KAWAII = new Item(NekoIds.itemProps("music_disc_kawaii").stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ToNekoSongs.KAWAII));
        MUSIC_DISC_NEVER_GONNA_GIVE_YOU_UP = new Item(NekoIds.itemProps("music_disc_never_gonna_give_you_up").stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ToNekoSongs.NEVER_GONNA_GIVE_YOU_UP));
        BAZOOKA = new BazookaItem(NekoIds.itemProps(BazookaItem.ID));
        PLOT_SCROLL = new PlotScrollItem(NekoIds.itemProps("plot_scroll"));
        FLY_SWORD = new FlySwordItem(NekoIds.itemProps("fly_sword").stacksTo(1));
        LIGHTNING_BOMB = new LightningBombItem(NekoIds.itemProps("lightning_bomb"));
        EXPLOSIVE_BOMB  = new ExplosiveBombItem(NekoIds.itemProps("explosive_bomb"));
        ENERGY_BOMB = new NekoEnergyBombItem("energy_bomb");
        CONTRACT = new ContractItem(NekoIds.itemProps("contract"));
        NEKO_AGGREGATOR_ITEM = new BlockItem(ToNekoBlocks.NEKO_AGGREGATOR, NekoIds.itemProps("neko_aggregator"));
        NEKO_INGOT = new Item(NekoIds.itemProps("neko_ingot"));
        NEKO_BLOCK = new BlockItem(ToNekoBlocks.NEKO_BLOCK, NekoIds.itemProps("neko_block"));
        NEKO_DIAMOND = new Item(NekoIds.itemProps("neko_diamond"));
        NEKO_DIAMOND_BLOCK = new BlockItem(ToNekoBlocks.NEKO_DIAMOND_BLOCK, NekoIds.itemProps("neko_diamond_block"));
        NEKO_CRYSTAL = new Item(NekoIds.itemProps("neko_crystal"));
        NEKO_ENERGY_STORAGE_SMALL = new NekoEnergyStorageItem("neko_energy_storage_small",150,false);
        NEKO_ENERGY_STORAGE_SMALL_CHARGED = new NekoEnergyStorageItem("neko_energy_storage_small_charged",150,true);
        NEKO_ENERGY_STORAGE_MEDIUM = new NekoEnergyStorageItem("neko_energy_storage_medium",400,false);
        NEKO_ENERGY_STORAGE_MEDIUM_CHARGED = new NekoEnergyStorageItem("neko_energy_storage_medium_charged",400,true);
        NEKO_ENERGY_STORAGE_LARGE = new NekoEnergyStorageItem("neko_energy_storage_large",1000,false);
        NEKO_ENERGY_STORAGE_LARGE_CHARGED = new NekoEnergyStorageItem("neko_energy_storage_large_charged",1000,true);
        NEKO_ENERGY_BURST = new NekoEnergyBurstItem("neko_energy_burst",2f,3f,50f);
        EVIL_NEKO_ENERGY_BURST = new EvilNekoEnergyBurstItem("evil_neko_energy_burst",2f,3f,50f);
        GENE_EDITOR = new GeneEditorItem(NekoIds.itemProps("gene_editor").stacksTo(1).rarity(Rarity.EPIC));
        GROWTH_TREAT = new GrowthTreatItem(NekoIds.itemProps(GrowthTreatItem.ID).component(DataComponents.FOOD,
                new FoodProperties(4, 2.0f, false)).component(DataComponents.CONSUMABLE, Consumables.DEFAULT_FOOD).rarity(Rarity.UNCOMMON));
        DEAGE_TREAT = new DeageTreatItem(NekoIds.itemProps(DeageTreatItem.ID).component(DataComponents.FOOD,
                new FoodProperties(4, 2.0f, false)).component(DataComponents.CONSUMABLE, Consumables.DEFAULT_FOOD).rarity(Rarity.UNCOMMON));
        NINE_LIVES_CHARM = new NineLivesCharmItem();
        NEKO_MULTI_TOOL = new NekoMultiToolItem();
        NEKO_BELL = new NekoBellItem();
        NEKO_ENERGY_BATTERY = new NekoEnergyBatteryItem(NekoEnergyBatteryItem.ID, 2000, 10, 5);
        NEKO_ENERGY_BATTERY_LARGE = new NekoEnergyBatteryItem(NekoEnergyBatteryItem.ID_LARGE, 10000, 100, 50);
        SHENG_DENG_ITEM = new ShengDengItem(ToNekoBlocks.SHENG_DENG, NekoIds.itemProps(ShengDengItem.ID));
        LEGWEAR_WORKBENCH_ITEM = new BlockItem(ToNekoBlocks.LEGWEAR_WORKBENCH, NekoIds.itemProps("legwear_workbench"));
        CLOTHESLINE_ITEM = new BlockItem(ToNekoBlocks.CLOTHESLINE, NekoIds.itemProps("clothesline"));
        SPOILED_WATER_BUCKET = new SpoiledWaterBucketItem();
        SPOILED_WATER_BOTTLE = new SpoiledWaterBottleItem();
        SPOILED_WATER_SPLASH = new SpoiledWaterThrowableItem(SpoiledWaterThrowableItem.SPLASH_ID, false);
        SPOILED_WATER_LINGERING = new SpoiledWaterThrowableItem(SpoiledWaterThrowableItem.LINGERING_ID, true);
        SCENT_PERFUME = new ScentPerfumeItem();
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(NekoPotionItem.ID), NEKO_POTION);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(NekoCollectorItem.ID), NEKO_COLLECTOR);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(FurryBoheItem.ID), FURRY_BOHE);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("adventurer_neko_spawn_egg"), ADVENTURER_NEKO_SPAWN_EGG);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("ghost_neko_spawn_egg"),GHOST_NEKO_SPAWN_EGG);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("fighting_neko_spawn_egg"), FIGHTING_NEKO_SPAWN_EGG);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("catnip"), CATNIP);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("infinite_catnip"), INFINITE_CATNIP);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("catnip_sandwich"), CATNIP_SANDWICH);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("catnip_seed"), CATNIP_SEED);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("wild_catnip"), WILD_CATNIP);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("music_disc_kawaii"), MUSIC_DISC_KAWAII);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("music_disc_never_gonna_give_you_up"), MUSIC_DISC_NEVER_GONNA_GIVE_YOU_UP);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(BazookaItem.ID), BAZOOKA);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("plot_scroll"), PLOT_SCROLL);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("fly_sword"), FLY_SWORD);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("lightning_bomb"), LIGHTNING_BOMB);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("explosive_bomb"), EXPLOSIVE_BOMB);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("energy_bomb"), ENERGY_BOMB);
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
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("neko_energy_burst"), NEKO_ENERGY_BURST);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("evil_neko_energy_burst"), EVIL_NEKO_ENERGY_BURST);
            Registry.register(BuiltInRegistries.ITEM, toNekoLoc("gene_editor"), GENE_EDITOR);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(GrowthTreatItem.ID), GROWTH_TREAT);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(DeageTreatItem.ID), DEAGE_TREAT);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(NineLivesCharmItem.ID), NINE_LIVES_CHARM);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(NekoMultiToolItem.ID), NEKO_MULTI_TOOL);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(NekoBellItem.ID), NEKO_BELL);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(NekoEnergyBatteryItem.ID), NEKO_ENERGY_BATTERY);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(NekoEnergyBatteryItem.ID_LARGE), NEKO_ENERGY_BATTERY_LARGE);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("sheng_deng"), SHENG_DENG_ITEM);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("legwear_workbench"), LEGWEAR_WORKBENCH_ITEM);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc("clothesline"), CLOTHESLINE_ITEM);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(SpoiledWaterBucketItem.ID), SPOILED_WATER_BUCKET);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(SpoiledWaterBottleItem.ID), SPOILED_WATER_BOTTLE);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(SpoiledWaterThrowableItem.SPLASH_ID), SPOILED_WATER_SPLASH);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(SpoiledWaterThrowableItem.LINGERING_ID), SPOILED_WATER_LINGERING);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(ScentPerfumeItem.ID), SCENT_PERFUME);

        // 26.x 迁移说明：Trinkets 尚无 26.x 版本，暂直接注册原版盔甲/丝袜物品
        NEKO_EARS = new NekoArmor.NekoEarsItem(ToNekoArmorMaterials.NEKO);
        NEKO_TAIL = new NekoArmor.NekoTailItem(ToNekoArmorMaterials.NEKO);
        NEKO_PAWS = new NekoArmor.NekoPawsItem(ToNekoArmorMaterials.NEKO);
        LEGWEAR_PANTYHOSE_40D = new LegwearItem.Pantyhose40DItem(ToNekoArmorMaterials.LEGWEAR);
        LEGWEAR_PANTYHOSE_20D = new LegwearItem.Pantyhose20DItem(ToNekoArmorMaterials.LEGWEAR);
        LEGWEAR_PANTYHOSE_5D = new LegwearItem.Pantyhose5DItem(ToNekoArmorMaterials.LEGWEAR);
        LEGWEAR_OVER_KNEE = new LegwearItem.OverKneeSockItem(ToNekoArmorMaterials.LEGWEAR);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(NekoArmor.NekoEarsItem.ID), NEKO_EARS);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(NekoArmor.NekoTailItem.ID), NEKO_TAIL);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(NekoArmor.NekoPawsItem.ID), NEKO_PAWS);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(LegwearItem.Pantyhose40DItem.ID), LEGWEAR_PANTYHOSE_40D);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(LegwearItem.Pantyhose20DItem.ID), LEGWEAR_PANTYHOSE_20D);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(LegwearItem.Pantyhose5DItem.ID), LEGWEAR_PANTYHOSE_5D);
        Registry.register(BuiltInRegistries.ITEM, toNekoLoc(LegwearItem.OverKneeSockItem.ID), LEGWEAR_OVER_KNEE);
        // 26.x：Fabric itemgroup.v1 更名为 creativetab.v1
        TONEKO_ITEM_GROUP = FabricCreativeModeTab.builder()
                .icon(() -> new ItemStack(NEKO_EARS))
                .title(Component.translatable("itemGroup.toneko"))
                .build();
        // 注册物品组
        TONEKO_ITEM_GROUP_KEY = ResourceKey.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(), toNekoLoc("item_group"));
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TONEKO_ITEM_GROUP_KEY,TONEKO_ITEM_GROUP);

        CreativeModeTabEvents.modifyOutputEvent(TONEKO_ITEM_GROUP_KEY).register(content -> {
            content.accept(NEKO_POTION);
            content.accept(NEKO_COLLECTOR);
            content.accept(NEKO_EARS);
            content.accept(NEKO_TAIL);
            content.accept(LEGWEAR_PANTYHOSE_40D);
            content.accept(LEGWEAR_PANTYHOSE_20D);
            content.accept(LEGWEAR_PANTYHOSE_5D);
            content.accept(LEGWEAR_OVER_KNEE);
            content.accept(CATNIP);
            content.accept(INFINITE_CATNIP);
            content.accept(CATNIP_SANDWICH);
            content.accept(CATNIP_SEED);
            content.accept(WILD_CATNIP);
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
            content.accept(ENERGY_BOMB);
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
            content.accept(NEKO_ENERGY_BURST);
            content.accept(EVIL_NEKO_ENERGY_BURST);
            content.accept(GENE_EDITOR);
            content.accept(GROWTH_TREAT);
            content.accept(DEAGE_TREAT);
            content.accept(NINE_LIVES_CHARM);
            content.accept(NEKO_MULTI_TOOL);
            content.accept(NEKO_BELL);
            content.accept(NEKO_ENERGY_BATTERY);
            content.accept(NEKO_ENERGY_BATTERY_LARGE);
            content.accept(SHENG_DENG_ITEM);
            content.accept(LEGWEAR_WORKBENCH_ITEM);
            content.accept(CLOTHESLINE_ITEM);
            content.accept(SPOILED_WATER_BUCKET);
            content.accept(SPOILED_WATER_BOTTLE);
            content.accept(SPOILED_WATER_SPLASH);
            content.accept(SPOILED_WATER_LINGERING);
            content.accept(SCENT_PERFUME);
            // 猫猫手册（Patchouli 指南书）
            ItemStack guideBook = GuideBookItem.createGuideBookStack();
            if (!guideBook.isEmpty()) {
                content.accept(guideBook);
            }
        });
    }
}
