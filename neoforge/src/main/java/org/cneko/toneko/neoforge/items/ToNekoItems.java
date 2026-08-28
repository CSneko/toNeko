package org.cneko.toneko.neoforge.items;

import org.cneko.toneko.common.mod.util.NekoIds;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.cneko.toneko.common.mod.items.*;
import org.cneko.toneko.common.mod.items.ammo.ExplosiveBombItem;
import org.cneko.toneko.common.mod.items.ammo.LightningBombItem;
import org.cneko.toneko.common.mod.items.ammo.NekoEnergyBombItem;
import org.cneko.toneko.common.mod.misc.ToNekoSongs;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.neoforge.ToNekoNeoForge;
import org.cneko.toneko.neoforge.entities.ToNekoEntities;
import org.cneko.toneko.neoforge.msic.ToNekoCriteriaNeoForge;
import org.cneko.toneko.neoforge.msic.ToNekoEffectNeoForge;
import org.cneko.toneko.neoforge.msic.ToNekoMenuTypesNeo;
import org.cneko.toneko.neoforge.msic.ToNekoRecipesNeo;

import java.util.List;
import java.util.Optional;

import static org.cneko.toneko.common.mod.items.ToNekoItems.*;
import static org.cneko.toneko.neoforge.ToNekoNeoForge.ITEMS;


public class ToNekoItems {

    public static DeferredHolder<Item,SpawnEggItem> ADVENTURER_NEKO_SPAWN_EGG_HOLDER;
    public static DeferredHolder<Item,SpawnEggItem> GHOST_NEKO_SPAWN_EGG_HOLDER;
    public static DeferredHolder<Item,SpawnEggItem> FIGHTING_NEKO_SPAWN_EGG_HOLDER;
    public static DeferredHolder<Item,NekoPotionItem> NEKO_POTION_HOLDER;
    public static DeferredHolder<Item,NekoCollectorItem> NEKO_COLLECTOR_HOLDER;
    public static DeferredHolder<Item,FurryBoheItem> FURRY_BOHE_HOLDER;
    public static DeferredHolder<Item,NekoArmor.NekoEarsItem> NEKO_EARS_HOLDER;
    public static DeferredHolder<Item,NekoArmor.NekoTailItem> NEKO_TAIL_HOLDER;
    public static DeferredHolder<Item,LegwearItem.Pantyhose40DItem> LEGWEAR_PANTYHOSE_40D_HOLDER;
    public static DeferredHolder<Item,LegwearItem.Pantyhose20DItem> LEGWEAR_PANTYHOSE_20D_HOLDER;
    public static DeferredHolder<Item,LegwearItem.Pantyhose5DItem> LEGWEAR_PANTYHOSE_5D_HOLDER;
    public static DeferredHolder<Item,LegwearItem.OverKneeSockItem> LEGWEAR_OVER_KNEE_HOLDER;
    public static DeferredHolder<Item, CatnipItem> CATNIP_HOLDER;
    public static DeferredHolder<Item, CatnipItem> CATNIP_SANDWICH_HOLDER;
    public static DeferredHolder<Item,Item> CATNIP_SEED_HOLDER;
    public static DeferredHolder<Item, BlockItem> WILD_CATNIP_HOLDER;
    public static DeferredHolder<CreativeModeTab,CreativeModeTab> TONEKO_ITEM_GROUP_HOLDER;
    public static DeferredHolder<Item,Item> MUSIC_DISC_KAWAII_HOLDER;
    public static DeferredHolder<Item,Item> MUSIC_DISC_NEVER_GONNA_GIVE_YOU_UP_HOLDER;
    public static DeferredHolder<Item,Item> BAZOOKA_HOLDER;
    public static DeferredHolder<Item,Item> PLOT_SCROLL_HOLDER;
    public static DeferredHolder<Item,Item> FLY_SWORD_HOLDER;
    public static DeferredHolder<Item,Item> LIGHTNING_BOMB_HOLDER;
    public static DeferredHolder<Item,Item> EXPLOSIVE_BOMB_HOLDER;
    public static DeferredHolder<Item,Item> CONTRACT_HOLDER;
    public static DeferredHolder<Item,Item> NEKO_AGGREGATOR_ITEM_HOLDER;
    public static DeferredHolder<Item,Item> NEKO_INGOT_HOLDER;
    public static DeferredHolder<Item,GrowthTreatItem> GROWTH_TREAT_HOLDER;
    public static DeferredHolder<Item,DeageTreatItem> DEAGE_TREAT_HOLDER;
    public static DeferredHolder<Item,CatnipItem> INFINITE_CATNIP_HOLDER;
    public static DeferredHolder<Item,NekoEnergyBombItem> NEKO_ENERGY_BOMB_HOLDER;
    public static DeferredHolder<Item,BlockItem> NEKO_BLOCK_HOLDER;
    public static DeferredHolder<Item,Item> NEKO_DIAMOND_HOLDER;
    public static DeferredHolder<Item,BlockItem> NEKO_DIAMOND_BLOCK_HOLDER;
    public static DeferredHolder<Item,Item> NEKO_CRYSTAL_HOLDER;
    public static DeferredHolder<Item,NekoEnergyStorageItem> NEKO_ENERGY_STORAGE_SMALL_HOLDER;
    public static DeferredHolder<Item,NekoEnergyStorageItem> NEKO_ENERGY_STORAGE_SMALL_CHARGED_HOLDER;
    public static DeferredHolder<Item,NekoEnergyStorageItem> NEKO_ENERGY_STORAGE_MEDIUM_HOLDER;
    public static DeferredHolder<Item,NekoEnergyStorageItem> NEKO_ENERGY_STORAGE_MEDIUM_CHARGED_HOLDER;
    public static DeferredHolder<Item,NekoEnergyStorageItem> NEKO_ENERGY_STORAGE_LARGE_HOLDER;
    public static DeferredHolder<Item,NekoEnergyStorageItem> NEKO_ENERGY_STORAGE_LARGE_CHARGED_HOLDER;
    public static DeferredHolder<Item,NekoEnergyBurstItem> NEKO_ENERGY_BURST_HOLDER;
    public static DeferredHolder<Item,EvilNekoEnergyBurstItem> EVIL_NEKO_ENERGY_BURST_HOLDER;
    public static DeferredHolder<Item,GeneEditorItem> GENE_EDITOR_HOLDER;
    public static DeferredHolder<Item,NekoArmor.NekoPawsItem> NEKO_PAWS_HOLDER;
    public static DeferredHolder<Item,NineLivesCharmItem> NINE_LIVES_CHARM_HOLDER;
    public static DeferredHolder<Item,NekoMultiToolItem> NEKO_MULTI_TOOL_HOLDER;
    public static DeferredHolder<Item,NekoBellItem> NEKO_BELL_HOLDER;
    public static DeferredHolder<Item,NekoEnergyBatteryItem> NEKO_ENERGY_BATTERY_HOLDER;
    public static DeferredHolder<Item,NekoEnergyBatteryItem> NEKO_ENERGY_BATTERY_LARGE_HOLDER;
    public static DeferredHolder<Item,BlockItem> SHENG_DENG_ITEM_HOLDER;
    public static DeferredHolder<Item,BlockItem> LEGWEAR_WORKBENCH_ITEM_HOLDER;
    public static DeferredHolder<Item,BlockItem> CLOTHESLINE_ITEM_HOLDER;
    public static DeferredHolder<Item,SpoiledWaterBucketItem> SPOILED_WATER_BUCKET_HOLDER;
    public static DeferredHolder<Item,SpoiledWaterBottleItem> SPOILED_WATER_BOTTLE_HOLDER;
    public static DeferredHolder<Item,SpoiledWaterThrowableItem> SPOILED_WATER_SPLASH_HOLDER;
    public static DeferredHolder<Item,SpoiledWaterThrowableItem> SPOILED_WATER_LINGERING_HOLDER;
    public static DeferredHolder<Item,ScentPerfumeItem> SCENT_PERFUME_HOLDER;

    public static void init() {
        registerWithOutConfig();
    }

    /**
     * 强制注册物品，无论配置文件如何设置
     */
    public static void registerWithOutConfig() {
        NEKO_POTION_HOLDER = ITEMS.register(NekoPotionItem.ID, NekoPotionItem::new);

        NEKO_COLLECTOR_HOLDER = ITEMS.register(NekoCollectorItem.ID, NekoCollectorItem::new);

        FURRY_BOHE_HOLDER = ITEMS.register(FurryBoheItem.ID, FurryBoheItem::new);

        NEKO_EARS_HOLDER = ITEMS.register(NekoArmor.NekoEarsItem.ID, ()->new NekoArmor.NekoEarsItem(ToNekoArmorMaterials.NEKO));

        NEKO_TAIL_HOLDER = ITEMS.register(NekoArmor.NekoTailItem.ID, ()->new NekoArmor.NekoTailItem(ToNekoArmorMaterials.NEKO));

        ADVENTURER_NEKO_SPAWN_EGG_HOLDER = ITEMS.register("adventurer_neko_spawn_egg",()->new SpawnEggItem(NekoIds.itemProps("adventurer_neko_spawn_egg").spawnEgg(org.cneko.toneko.neoforge.entities.ToNekoEntities.ADVENTURER_NEKO_HOLDER.get())));
        GHOST_NEKO_SPAWN_EGG_HOLDER = ITEMS.register("ghost_neko_spawn_egg",()->new SpawnEggItem(NekoIds.itemProps("ghost_neko_spawn_egg").spawnEgg(org.cneko.toneko.neoforge.entities.ToNekoEntities.GHOST_NEKO_HOLDER.get())));
        FIGHTING_NEKO_SPAWN_EGG_HOLDER = ITEMS.register("fighting_neko_spawn_egg",()->new SpawnEggItem(NekoIds.itemProps("fighting_neko_spawn_egg").spawnEgg(org.cneko.toneko.neoforge.entities.ToNekoEntities.FIGHTING_NEKO_HOLDER.get())));
        // 诺艾尔猫娘没有刷怪蛋：只能通过 /summon toneko:noelle_maid_neko 命令召唤

        CATNIP_HOLDER = ITEMS.register("catnip", ()->new CatnipItem(NekoIds.itemProps("catnip").component(DataComponents.FOOD,
                new FoodProperties(2, 1.0f, true)).component(DataComponents.CONSUMABLE, Consumables.DEFAULT_FOOD)));

        CATNIP_SANDWICH_HOLDER = ITEMS.register("catnip_sandwich", ()->new CatnipItem(NekoIds.itemProps("catnip_sandwich").component(DataComponents.FOOD,
                new FoodProperties(6, 3.0f, false)).component(DataComponents.CONSUMABLE, Consumables.DEFAULT_FOOD)));

        CATNIP_SEED_HOLDER = ITEMS.register("catnip_seed",()->new BlockItem(ToNekoBlocks.CATNIP_HOLDER.get(), NekoIds.itemProps("catnip_seed")));

        WILD_CATNIP_HOLDER = ITEMS.register("wild_catnip",()->new BlockItem(ToNekoBlocks.WILD_CATNIP_HOLDER.get(), NekoIds.itemProps("wild_catnip")));

        MUSIC_DISC_KAWAII_HOLDER = ITEMS.register("music_disc_kawaii",()->new Item(NekoIds.itemProps("music_disc_kawaii").stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ToNekoSongs.KAWAII)));
        MUSIC_DISC_NEVER_GONNA_GIVE_YOU_UP_HOLDER = ITEMS.register("music_disc_never_gonna_give_you_up",()->new Item(NekoIds.itemProps("music_disc_never_gonna_give_you_up").stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ToNekoSongs.NEVER_GONNA_GIVE_YOU_UP)));

        BAZOOKA_HOLDER = ITEMS.register("bazooka",()->new BazookaItem(NekoIds.itemProps("bazooka").stacksTo(1).rarity(Rarity.RARE)));

        PLOT_SCROLL_HOLDER = ITEMS.register("plot_scroll", ()->new PlotScrollItem(NekoIds.itemProps("plot_scroll")));
        FLY_SWORD_HOLDER = ITEMS.register("fly_sword",()->new FlySwordItem(NekoIds.itemProps("fly_sword").stacksTo(1)));

        LIGHTNING_BOMB_HOLDER = ITEMS.register("lightning_bomb", ()->new LightningBombItem(NekoIds.itemProps("lightning_bomb")));

        EXPLOSIVE_BOMB_HOLDER = ITEMS.register("explosive_bomb", ()->new ExplosiveBombItem(NekoIds.itemProps("explosive_bomb")));

        CONTRACT_HOLDER = ITEMS.register("contract", ()->new ContractItem(NekoIds.itemProps("contract")));

        NEKO_AGGREGATOR_ITEM_HOLDER = ITEMS.register("neko_aggregator",()->new BlockItem(ToNekoBlocks.NEKO_AGGREGATOR_BLOCK_HOLDER.get(), NekoIds.itemProps("neko_aggregator")));

        NEKO_INGOT_HOLDER = ITEMS.register("neko_ingot", ()->new Item(NekoIds.itemProps("neko_ingot")));

        GROWTH_TREAT_HOLDER = ITEMS.register("growth_treat", ()->new GrowthTreatItem(NekoIds.itemProps("growth_treat").component(DataComponents.FOOD,
                new FoodProperties(4, 2.0f, false)).component(DataComponents.CONSUMABLE, Consumables.DEFAULT_FOOD).rarity(Rarity.UNCOMMON)));

        DEAGE_TREAT_HOLDER = ITEMS.register("deage_treat", ()->new DeageTreatItem(NekoIds.itemProps("deage_treat").component(DataComponents.FOOD,
                new FoodProperties(4, 2.0f, false)).component(DataComponents.CONSUMABLE, Consumables.DEFAULT_FOOD).rarity(Rarity.UNCOMMON)));

        NEKO_PAWS_HOLDER = ITEMS.register(NekoArmor.NekoPawsItem.ID, ()->new NekoArmor.NekoPawsItem(ToNekoArmorMaterials.NEKO));

        LEGWEAR_PANTYHOSE_40D_HOLDER = ITEMS.register(LegwearItem.Pantyhose40DItem.ID, ()->new LegwearItem.Pantyhose40DItem(ToNekoArmorMaterials.LEGWEAR));
        LEGWEAR_PANTYHOSE_20D_HOLDER = ITEMS.register(LegwearItem.Pantyhose20DItem.ID, ()->new LegwearItem.Pantyhose20DItem(ToNekoArmorMaterials.LEGWEAR));
        LEGWEAR_PANTYHOSE_5D_HOLDER = ITEMS.register(LegwearItem.Pantyhose5DItem.ID, ()->new LegwearItem.Pantyhose5DItem(ToNekoArmorMaterials.LEGWEAR));
        LEGWEAR_OVER_KNEE_HOLDER = ITEMS.register(LegwearItem.OverKneeSockItem.ID, ()->new LegwearItem.OverKneeSockItem(ToNekoArmorMaterials.LEGWEAR));

        INFINITE_CATNIP_HOLDER = ITEMS.register("infinite_catnip", ()->new CatnipItem.InfiniteCatnipItem(NekoIds.itemProps("infinite_catnip").component(DataComponents.FOOD,new FoodProperties(2, 1.0f, false)).component(DataComponents.CONSUMABLE, Consumables.DEFAULT_FOOD).rarity(Rarity.UNCOMMON)));

        NEKO_ENERGY_BOMB_HOLDER = ITEMS.register("energy_bomb", () -> new NekoEnergyBombItem("energy_bomb"));

        NEKO_BLOCK_HOLDER = ITEMS.register("neko_block", ()->new BlockItem(ToNekoBlocks.NEKO_BLOCK_HOLDER.get(), NekoIds.itemProps("neko_block")));

        NEKO_DIAMOND_HOLDER = ITEMS.register("neko_diamond", ()->new Item(NekoIds.itemProps("neko_diamond")));

        NEKO_DIAMOND_BLOCK_HOLDER = ITEMS.register("neko_diamond_block", ()->new BlockItem(ToNekoBlocks.NEKO_DIAMOND_BLOCK_HOLDER.get(), NekoIds.itemProps("neko_diamond_block")));

        NEKO_CRYSTAL_HOLDER = ITEMS.register("neko_crystal", ()->new Item(NekoIds.itemProps("neko_crystal")));

        NEKO_ENERGY_STORAGE_SMALL_HOLDER = ITEMS.register("neko_energy_storage_small", ()->new NekoEnergyStorageItem("neko_energy_storage_small",150,false));

        NEKO_ENERGY_STORAGE_SMALL_CHARGED_HOLDER = ITEMS.register("neko_energy_storage_small_charged", ()->new NekoEnergyStorageItem("neko_energy_storage_small_charged",150,true));

        NEKO_ENERGY_STORAGE_MEDIUM_HOLDER = ITEMS.register("neko_energy_storage_medium", ()->new NekoEnergyStorageItem("neko_energy_storage_medium",400,false));

        NEKO_ENERGY_STORAGE_MEDIUM_CHARGED_HOLDER = ITEMS.register("neko_energy_storage_medium_charged", ()->new NekoEnergyStorageItem("neko_energy_storage_medium_charged",400,true));

        NEKO_ENERGY_STORAGE_LARGE_HOLDER = ITEMS.register("neko_energy_storage_large", ()->new NekoEnergyStorageItem("neko_energy_storage_large",1000,false));

        NEKO_ENERGY_STORAGE_LARGE_CHARGED_HOLDER = ITEMS.register("neko_energy_storage_large_charged", ()->new NekoEnergyStorageItem("neko_energy_storage_large_charged",1000,true));

        NEKO_ENERGY_BURST_HOLDER = ITEMS.register("neko_energy_burst", ()->new NekoEnergyBurstItem("neko_energy_burst",2f,3f,50f));
        EVIL_NEKO_ENERGY_BURST_HOLDER = ITEMS.register("evil_neko_energy_burst", ()->new EvilNekoEnergyBurstItem("evil_neko_energy_burst",2f,3f,50f));

        GENE_EDITOR_HOLDER = ITEMS.register("gene_editor", ()->new GeneEditorItem(NekoIds.itemProps("gene_editor").stacksTo(1).rarity(Rarity.EPIC)));

        NINE_LIVES_CHARM_HOLDER = ITEMS.register(NineLivesCharmItem.ID, NineLivesCharmItem::new);

        NEKO_MULTI_TOOL_HOLDER = ITEMS.register(NekoMultiToolItem.ID, NekoMultiToolItem::new);

        NEKO_BELL_HOLDER = ITEMS.register(NekoBellItem.ID, NekoBellItem::new);

        NEKO_ENERGY_BATTERY_HOLDER = ITEMS.register(NekoEnergyBatteryItem.ID,
                () -> new NekoEnergyBatteryItem(NekoEnergyBatteryItem.ID, 2000, 10, 5));
        NEKO_ENERGY_BATTERY_LARGE_HOLDER = ITEMS.register(NekoEnergyBatteryItem.ID_LARGE,
                () -> new NekoEnergyBatteryItem(NekoEnergyBatteryItem.ID_LARGE, 10000, 100, 50));

        SHENG_DENG_ITEM_HOLDER = ITEMS.register(ShengDengItem.ID, () -> new ShengDengItem(ToNekoBlocks.SHENG_DENG_HOLDER.get(), NekoIds.itemProps("sheng_deng")));

        LEGWEAR_WORKBENCH_ITEM_HOLDER = ITEMS.register("legwear_workbench", () -> new BlockItem(ToNekoBlocks.LEGWEAR_WORKBENCH_HOLDER.get(), NekoIds.itemProps("legwear_workbench")));

        CLOTHESLINE_ITEM_HOLDER = ITEMS.register("clothesline", () -> new BlockItem(ToNekoBlocks.CLOTHESLINE_HOLDER.get(), NekoIds.itemProps("clothesline")));

        SPOILED_WATER_BUCKET_HOLDER = ITEMS.register(SpoiledWaterBucketItem.ID, SpoiledWaterBucketItem::new);
        SPOILED_WATER_BOTTLE_HOLDER = ITEMS.register(SpoiledWaterBottleItem.ID, SpoiledWaterBottleItem::new);
        SPOILED_WATER_SPLASH_HOLDER = ITEMS.register(SpoiledWaterThrowableItem.SPLASH_ID, () -> new SpoiledWaterThrowableItem(SpoiledWaterThrowableItem.SPLASH_ID, false));
        SPOILED_WATER_LINGERING_HOLDER = ITEMS.register(SpoiledWaterThrowableItem.LINGERING_ID, () -> new SpoiledWaterThrowableItem(SpoiledWaterThrowableItem.LINGERING_ID, true));
        SCENT_PERFUME_HOLDER = ITEMS.register(ScentPerfumeItem.ID, ScentPerfumeItem::new);

        // 注册物品组
        TONEKO_ITEM_GROUP_HOLDER = ToNekoNeoForge.CREATIVE_MODE_TABS.register("toneko_group", ()-> CreativeModeTab.builder()
                .icon(()->NEKO_EARS_HOLDER.get().getDefaultInstance())
                .title(Component.translatable("itemGroup.toneko"))
                .displayItems((parameters, event)->{
                    event.accept(NEKO_POTION_HOLDER.get());
                    event.accept(NEKO_COLLECTOR_HOLDER.get());
                    event.accept(FURRY_BOHE_HOLDER.get());
                    event.accept(NEKO_EARS_HOLDER.get());
                    event.accept(NEKO_TAIL_HOLDER.get());
                    event.accept(LEGWEAR_PANTYHOSE_40D_HOLDER.get());
                    event.accept(LEGWEAR_PANTYHOSE_20D_HOLDER.get());
                    event.accept(LEGWEAR_PANTYHOSE_5D_HOLDER.get());
                    event.accept(LEGWEAR_OVER_KNEE_HOLDER.get());
                    event.accept(ADVENTURER_NEKO_SPAWN_EGG_HOLDER.get());
                    event.accept(GHOST_NEKO_SPAWN_EGG_HOLDER.get());
                    event.accept(FIGHTING_NEKO_SPAWN_EGG_HOLDER.get());
                    event.accept(CATNIP_HOLDER.get());
                    event.accept(CATNIP_SANDWICH_HOLDER.get());
                    event.accept(CATNIP_SEED_HOLDER.get());
                    event.accept(WILD_CATNIP_HOLDER.get());
                    event.accept(MUSIC_DISC_KAWAII_HOLDER.get());
                    if (ConfigUtil.IS_FOOL_DAY){
                        event.accept(MUSIC_DISC_NEVER_GONNA_GIVE_YOU_UP_HOLDER.get());
                    }
                    event.accept(BAZOOKA_HOLDER.get());
                    event.accept(EXPLOSIVE_BOMB_HOLDER.get());
                    event.accept(LIGHTNING_BOMB_HOLDER.get());
                    // event.accept(PLOT_SCROLL_HOLDER.get());
                    event.accept(CONTRACT_HOLDER.get());
                    event.accept(NEKO_AGGREGATOR_ITEM_HOLDER.get());
                    event.accept(NEKO_INGOT_HOLDER.get());
                    event.accept(NEKO_BLOCK_HOLDER.get());
                    event.accept(NEKO_DIAMOND_HOLDER.get());
                    event.accept(NEKO_DIAMOND_BLOCK_HOLDER.get());
                    event.accept(NEKO_CRYSTAL_HOLDER.get());
                    event.accept(NEKO_ENERGY_STORAGE_SMALL_HOLDER.get());
                    event.accept(NEKO_ENERGY_STORAGE_SMALL_CHARGED_HOLDER.get());
                    event.accept(NEKO_ENERGY_STORAGE_MEDIUM_HOLDER.get());
                    event.accept(NEKO_ENERGY_STORAGE_MEDIUM_CHARGED_HOLDER.get());
                    event.accept(NEKO_ENERGY_STORAGE_LARGE_HOLDER.get());
                    event.accept(NEKO_ENERGY_STORAGE_LARGE_CHARGED_HOLDER.get());
                    event.accept(NEKO_ENERGY_BURST_HOLDER.get());
                    event.accept(EVIL_NEKO_ENERGY_BURST_HOLDER.get());
                    event.accept(GENE_EDITOR_HOLDER.get());
                    event.accept(INFINITE_CATNIP_HOLDER.get());
                    event.accept(NEKO_ENERGY_BOMB_HOLDER.get());
                    event.accept(GROWTH_TREAT_HOLDER.get());
                    event.accept(DEAGE_TREAT_HOLDER.get());
                    event.accept(NINE_LIVES_CHARM_HOLDER.get());
                    event.accept(NEKO_MULTI_TOOL_HOLDER.get());
                    event.accept(NEKO_BELL_HOLDER.get());
                    event.accept(NEKO_ENERGY_BATTERY_HOLDER.get());
                    event.accept(NEKO_ENERGY_BATTERY_LARGE_HOLDER.get());
                    event.accept(SHENG_DENG_ITEM_HOLDER.get());
                    event.accept(LEGWEAR_WORKBENCH_ITEM_HOLDER.get());
                    event.accept(CLOTHESLINE_ITEM_HOLDER.get());
                    event.accept(SPOILED_WATER_BUCKET_HOLDER.get());
                    event.accept(SPOILED_WATER_BOTTLE_HOLDER.get());
                    event.accept(SPOILED_WATER_SPLASH_HOLDER.get());
                    event.accept(SPOILED_WATER_LINGERING_HOLDER.get());
                    event.accept(SCENT_PERFUME_HOLDER.get());
                    // 猫猫手册（Patchouli 指南书）
                    ItemStack guideBook = GuideBookItem.createGuideBookStack();
                    if (!guideBook.isEmpty()) {
                        event.accept(guideBook);
                    }
                })
                .build()
        );
    }

    public static boolean tryClass(String clazz){
        try {
            Class.forName(clazz);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab().equals(TONEKO_ITEM_GROUP_HOLDER.get())) {
            event.accept(NEKO_POTION_HOLDER.get());
            event.accept(NEKO_COLLECTOR_HOLDER.get());
            event.accept(FURRY_BOHE_HOLDER.get());
            event.accept(NEKO_EARS_HOLDER.get());
            event.accept(NEKO_TAIL_HOLDER.get());
            event.accept(LEGWEAR_PANTYHOSE_40D_HOLDER.get());
            event.accept(LEGWEAR_PANTYHOSE_20D_HOLDER.get());
            event.accept(LEGWEAR_PANTYHOSE_5D_HOLDER.get());
            event.accept(LEGWEAR_OVER_KNEE_HOLDER.get());
            event.accept(ADVENTURER_NEKO_SPAWN_EGG_HOLDER.get());
            event.accept(GHOST_NEKO_SPAWN_EGG_HOLDER.get());
            event.accept(FIGHTING_NEKO_SPAWN_EGG_HOLDER.get());
            // 诺艾尔猫娘没有刷怪蛋
            event.accept(PLOT_SCROLL_HOLDER.get());
            event.accept(CATNIP_HOLDER.get());
            event.accept(CATNIP_SANDWICH_HOLDER.get());
            event.accept(CATNIP_SEED_HOLDER.get());
            event.accept(WILD_CATNIP_HOLDER.get());
            event.accept(MUSIC_DISC_KAWAII_HOLDER.get());
            event.accept(MUSIC_DISC_NEVER_GONNA_GIVE_YOU_UP_HOLDER.get());
            if (ConfigUtil.IS_FOOL_DAY){
                event.accept(MUSIC_DISC_NEVER_GONNA_GIVE_YOU_UP_HOLDER.get());
            }
            event.accept(BAZOOKA_HOLDER.get());
            // event.accept(PLOT_SCROLL_HOLDER.get());
            event.accept(LIGHTNING_BOMB_HOLDER.get());
            event.accept(EXPLOSIVE_BOMB_HOLDER.get());
            event.accept(CONTRACT_HOLDER.get());
            event.accept(NEKO_AGGREGATOR_ITEM_HOLDER.get());
            event.accept(NEKO_INGOT_HOLDER.get());
            event.accept(NEKO_BLOCK_HOLDER.get());
            event.accept(NEKO_DIAMOND_HOLDER.get());
            event.accept(NEKO_DIAMOND_BLOCK_HOLDER.get());
            event.accept(NEKO_CRYSTAL_HOLDER.get());
            event.accept(NEKO_ENERGY_STORAGE_SMALL_HOLDER.get());
            event.accept(NEKO_ENERGY_STORAGE_SMALL_CHARGED_HOLDER.get());
            event.accept(NEKO_ENERGY_STORAGE_MEDIUM_HOLDER.get());
            event.accept(NEKO_ENERGY_STORAGE_MEDIUM_CHARGED_HOLDER.get());
            event.accept(NEKO_ENERGY_STORAGE_LARGE_HOLDER.get());
            event.accept(NEKO_ENERGY_STORAGE_LARGE_CHARGED_HOLDER.get());
            event.accept(NEKO_ENERGY_BURST_HOLDER.get());
            event.accept(EVIL_NEKO_ENERGY_BURST_HOLDER.get());
            event.accept(GENE_EDITOR_HOLDER.get());
            event.accept(INFINITE_CATNIP_HOLDER.get());
            event.accept(NEKO_ENERGY_BOMB_HOLDER.get());
            event.accept(GROWTH_TREAT_HOLDER.get());
            event.accept(DEAGE_TREAT_HOLDER.get());
            event.accept(NINE_LIVES_CHARM_HOLDER.get());
            event.accept(NEKO_MULTI_TOOL_HOLDER.get());
            event.accept(NEKO_BELL_HOLDER.get());
            event.accept(NEKO_ENERGY_BATTERY_HOLDER.get());
            event.accept(NEKO_ENERGY_BATTERY_LARGE_HOLDER.get());
            event.accept(SHENG_DENG_ITEM_HOLDER.get());
            event.accept(LEGWEAR_WORKBENCH_ITEM_HOLDER.get());
            event.accept(CLOTHESLINE_ITEM_HOLDER.get());
            event.accept(SPOILED_WATER_BUCKET_HOLDER.get());
            event.accept(SPOILED_WATER_BOTTLE_HOLDER.get());
            event.accept(SPOILED_WATER_SPLASH_HOLDER.get());
            event.accept(SPOILED_WATER_LINGERING_HOLDER.get());
            event.accept(SCENT_PERFUME_HOLDER.get());
            // 猫猫手册（Patchouli 指南书）
            ItemStack guideBook = GuideBookItem.createGuideBookStack();
            if (!guideBook.isEmpty()) {
                event.accept(guideBook);
            }
        }
        reg();
    }

    public static void reg(){
        SHENG_DENG_ITEM = SHENG_DENG_ITEM_HOLDER.get();
        LEGWEAR_WORKBENCH_ITEM = LEGWEAR_WORKBENCH_ITEM_HOLDER.get();
        CLOTHESLINE_ITEM = CLOTHESLINE_ITEM_HOLDER.get();
        SPOILED_WATER_BUCKET = SPOILED_WATER_BUCKET_HOLDER.get();
        SPOILED_WATER_BOTTLE = SPOILED_WATER_BOTTLE_HOLDER.get();
        SPOILED_WATER_SPLASH = SPOILED_WATER_SPLASH_HOLDER.get();
        SPOILED_WATER_LINGERING = SPOILED_WATER_LINGERING_HOLDER.get();
        SCENT_PERFUME = SCENT_PERFUME_HOLDER.get();
        CATNIP = CATNIP_HOLDER.get();
        INFINITE_CATNIP = INFINITE_CATNIP_HOLDER.get();
        WILD_CATNIP = WILD_CATNIP_HOLDER.get();
        CATNIP_SANDWICH = CATNIP_SANDWICH_HOLDER.get();
        CATNIP_SEED = CATNIP_SEED_HOLDER.get();
        NEKO_COLLECTOR = NEKO_COLLECTOR_HOLDER.get();
        FURRY_BOHE = FURRY_BOHE_HOLDER.get();
        NEKO_TAIL = NEKO_TAIL_HOLDER.get();
        NEKO_EARS = NEKO_EARS_HOLDER.get();
        NEKO_PAWS = NEKO_PAWS_HOLDER.get();
        LEGWEAR_PANTYHOSE_40D = LEGWEAR_PANTYHOSE_40D_HOLDER.get();
        LEGWEAR_PANTYHOSE_20D = LEGWEAR_PANTYHOSE_20D_HOLDER.get();
        LEGWEAR_PANTYHOSE_5D = LEGWEAR_PANTYHOSE_5D_HOLDER.get();
        LEGWEAR_OVER_KNEE = LEGWEAR_OVER_KNEE_HOLDER.get();
        NEKO_POTION = NEKO_POTION_HOLDER.get();
        MUSIC_DISC_KAWAII = MUSIC_DISC_KAWAII_HOLDER.get();
        MUSIC_DISC_NEVER_GONNA_GIVE_YOU_UP = MUSIC_DISC_NEVER_GONNA_GIVE_YOU_UP_HOLDER.get();
        BAZOOKA = BAZOOKA_HOLDER.get();
        PLOT_SCROLL = PLOT_SCROLL_HOLDER.get();
        FLY_SWORD = FLY_SWORD_HOLDER.get();
        LIGHTNING_BOMB = LIGHTNING_BOMB_HOLDER.get();
        EXPLOSIVE_BOMB = EXPLOSIVE_BOMB_HOLDER.get();
        ENERGY_BOMB = NEKO_ENERGY_BOMB_HOLDER.get();
        CONTRACT = CONTRACT_HOLDER.get();
        NEKO_AGGREGATOR_ITEM = NEKO_AGGREGATOR_ITEM_HOLDER.get();
        NEKO_INGOT = NEKO_INGOT_HOLDER.get();
        NEKO_BLOCK = NEKO_BLOCK_HOLDER.get();
        NEKO_DIAMOND = NEKO_DIAMOND_HOLDER.get();
        NEKO_DIAMOND_BLOCK = NEKO_DIAMOND_BLOCK_HOLDER.get();
        NEKO_CRYSTAL = NEKO_CRYSTAL_HOLDER.get();
        NEKO_ENERGY_STORAGE_SMALL = NEKO_ENERGY_STORAGE_SMALL_HOLDER.get();
        NEKO_ENERGY_STORAGE_SMALL_CHARGED = NEKO_ENERGY_STORAGE_SMALL_CHARGED_HOLDER.get();
        NEKO_ENERGY_STORAGE_MEDIUM = NEKO_ENERGY_STORAGE_MEDIUM_HOLDER.get();
        NEKO_ENERGY_STORAGE_MEDIUM_CHARGED = NEKO_ENERGY_STORAGE_MEDIUM_CHARGED_HOLDER.get();
        NEKO_ENERGY_STORAGE_LARGE = NEKO_ENERGY_STORAGE_LARGE_HOLDER.get();
        NEKO_ENERGY_STORAGE_LARGE_CHARGED = NEKO_ENERGY_STORAGE_LARGE_CHARGED_HOLDER.get();
        NEKO_ENERGY_BURST = NEKO_ENERGY_BURST_HOLDER.get();
        EVIL_NEKO_ENERGY_BURST = EVIL_NEKO_ENERGY_BURST_HOLDER.get();
        GENE_EDITOR = GENE_EDITOR_HOLDER.get();
        GROWTH_TREAT = GROWTH_TREAT_HOLDER.get();
        DEAGE_TREAT = DEAGE_TREAT_HOLDER.get();
        NINE_LIVES_CHARM = NINE_LIVES_CHARM_HOLDER.get();
        NEKO_MULTI_TOOL = NEKO_MULTI_TOOL_HOLDER.get();
        NEKO_BELL = NEKO_BELL_HOLDER.get();
        NEKO_ENERGY_BATTERY = NEKO_ENERGY_BATTERY_HOLDER.get();
        NEKO_ENERGY_BATTERY_LARGE = NEKO_ENERGY_BATTERY_LARGE_HOLDER.get();
        ToNekoEffectNeoForge.reg();
        ToNekoBlocks.reg();
        ToNekoEntities.reg();
        ToNekoCriteriaNeoForge.reg();
        ToNekoMenuTypesNeo.reg();
        ToNekoRecipesNeo.reg();
    }
}
