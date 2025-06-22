package org.cneko.toneko.neoforge.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.cneko.toneko.common.mod.items.*;
import org.cneko.toneko.common.mod.items.ammo.ExplosiveBombItem;
import org.cneko.toneko.common.mod.items.ammo.LightningBombItem;
import org.cneko.toneko.common.mod.misc.ToNekoSongs;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.neoforge.ToNekoNeoForge;
import org.cneko.toneko.neoforge.entities.ToNekoEntities;
import org.cneko.toneko.neoforge.msic.ToNekoCriteriaNeoForge;
import org.cneko.toneko.neoforge.msic.ToNekoEffectNeoForge;

import java.util.List;
import java.util.Optional;

import static org.cneko.toneko.common.mod.items.ToNekoItems.*;
import static org.cneko.toneko.neoforge.ToNekoNeoForge.ITEMS;


public class ToNekoItems {

    public static DeferredHolder<Item,DeferredSpawnEggItem> ADVENTURER_NEKO_SPAWN_EGG_HOLDER;
    public static DeferredHolder<Item,DeferredSpawnEggItem> GHOST_NEKO_SPAWN_EGG_HOLDER;
    public static DeferredHolder<Item,DeferredSpawnEggItem> FIGHTING_NEKO_SPAWN_EGG_HOLDER;
    public static DeferredHolder<Item,NekoPotionItem> NEKO_POTION_HOLDER;
    public static DeferredHolder<Item,NekoCollectorItem> NEKO_COLLECTOR_HOLDER;
    public static DeferredHolder<Item,FurryBoheItem> FURRY_BOHE_HOLDER;
    public static DeferredHolder<Item,NekoArmor.NekoEarsItem> NEKO_EARS_HOLDER;
    public static DeferredHolder<Item,NekoArmor.NekoTailItem> NEKO_TAIL_HOLDER;
    public static DeferredHolder<Item, CatnipItem> CATNIP_HOLDER;
    public static DeferredHolder<Item, CatnipItem> CATNIP_SANDWICH_HOLDER;
    public static DeferredHolder<Item,Item> CATNIP_SEED_HOLDER;
    public static DeferredHolder<CreativeModeTab,CreativeModeTab> TONEKO_ITEM_GROUP_HOLDER;
    public static DeferredHolder<Item,Item> MUSIC_DISC_KAWAII_HOLDER;
    public static DeferredHolder<Item,Item> MUSIC_DISC_NEVER_GONNA_GIVE_YOU_UP_HOLDER;
    public static DeferredHolder<Item,Item> BAZOOKA_HOLDER;
    public static DeferredHolder<Item,Item> PLOT_SCROLL_HOLDER;
    public static DeferredHolder<Item,Item> LIGHTNING_BOMB_HOLDER;
    public static DeferredHolder<Item,Item> EXPLOSIVE_BOMB_HOLDER;
    public static DeferredHolder<Item,Item> CONTRACT_HOLDER;

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

        ADVENTURER_NEKO_SPAWN_EGG_HOLDER = ITEMS.register("adventurer_neko_spawn_egg",()->new DeferredSpawnEggItem(()->ToNekoEntities.ADVENTURER_NEKO_HOLDER.get(), 0x7e7e7e, 0xffffff,new Item.Properties()));
        GHOST_NEKO_SPAWN_EGG_HOLDER = ITEMS.register("ghost_neko_spawn_egg",()->new DeferredSpawnEggItem(()->ToNekoEntities.GHOST_NEKO_HOLDER.get(), 0x7e7e7e, 0xffffff,new Item.Properties()));
        FIGHTING_NEKO_SPAWN_EGG_HOLDER = ITEMS.register("fighting_neko_spawn_egg",()->new DeferredSpawnEggItem(()->ToNekoEntities.FIGHTING_NEKO_HOLDER.get(), 0x7e7e7e, 0xffffff,new Item.Properties()));

        CATNIP_HOLDER = ITEMS.register("catnip", ()->new CatnipItem(new Item.Properties().component(DataComponents.FOOD,
                new FoodProperties(2,1.0f,true,1.6f, Optional.empty(),
                        List.of()
                ))));

        CATNIP_SANDWICH_HOLDER = ITEMS.register("catnip_sandwich", ()->new CatnipItem(new Item.Properties().component(DataComponents.FOOD,
                new FoodProperties(6,3.0f,false,1.6f, Optional.empty(),
                        List.of()
                ))));

        CATNIP_SEED_HOLDER = ITEMS.register("catnip_seed",()->new ItemNameBlockItem(ToNekoBlocks.CATNIP_HOLDER.get(), new Item.Properties()));

        MUSIC_DISC_KAWAII_HOLDER = ITEMS.register("music_disc_kawaii",()->new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ToNekoSongs.KAWAII)));
        MUSIC_DISC_NEVER_GONNA_GIVE_YOU_UP_HOLDER = ITEMS.register("music_disc_never_gonna_give_you_up",()->new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ToNekoSongs.NEVER_GONNA_GIVE_YOU_UP)));

        BAZOOKA_HOLDER = ITEMS.register("bazooka",()->new BazookaItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

        PLOT_SCROLL_HOLDER = ITEMS.register("plot_scroll",()->new PlotScrollItem(new Item.Properties()));

        LIGHTNING_BOMB_HOLDER = ITEMS.register("lightning_bomb",()->new LightningBombItem(new Item.Properties()));

        EXPLOSIVE_BOMB_HOLDER = ITEMS.register("explosive_bomb",()->new ExplosiveBombItem(new Item.Properties()));

        CONTRACT_HOLDER = ITEMS.register("contract",()->new ContractItem(new Item.Properties()));

        ITEMS.register(NekoArmor.NekoPawsItem.ID, ()->new NekoArmor.NekoPawsItem(ToNekoArmorMaterials.NEKO)); // 此物品暂不添加

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
                    event.accept(ADVENTURER_NEKO_SPAWN_EGG_HOLDER.get());
                    event.accept(GHOST_NEKO_SPAWN_EGG_HOLDER.get());
                    event.accept(FIGHTING_NEKO_SPAWN_EGG_HOLDER.get());
                    event.accept(CATNIP_HOLDER.get());
                    event.accept(CATNIP_SANDWICH_HOLDER.get());
                    event.accept(CATNIP_SEED_HOLDER.get());
                    event.accept(MUSIC_DISC_KAWAII_HOLDER.get());
                    if (ConfigUtil.IS_FOOL_DAY){
                        event.accept(MUSIC_DISC_NEVER_GONNA_GIVE_YOU_UP_HOLDER.get());
                    }
                    event.accept(BAZOOKA_HOLDER.get());
                    event.accept(EXPLOSIVE_BOMB_HOLDER.get());
                    event.accept(LIGHTNING_BOMB_HOLDER.get());
                    event.accept(PLOT_SCROLL_HOLDER.get());
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
            event.accept(ADVENTURER_NEKO_SPAWN_EGG_HOLDER.get());
            event.accept(GHOST_NEKO_SPAWN_EGG_HOLDER.get());
            event.accept(FIGHTING_NEKO_SPAWN_EGG_HOLDER.get());
            event.accept(PLOT_SCROLL_HOLDER.get());
            event.accept(CATNIP_HOLDER.get());
            event.accept(CATNIP_SANDWICH_HOLDER.get());
            event.accept(CATNIP_SEED_HOLDER.get());
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
        }
        reg();
    }

    public static void reg(){
        CATNIP = CATNIP_HOLDER.get();
        CATNIP_SANDWICH = CATNIP_SANDWICH_HOLDER.get();
        CATNIP_SEED = CATNIP_SEED_HOLDER.get();
        NEKO_COLLECTOR = NEKO_COLLECTOR_HOLDER.get();
        FURRY_BOHE = FURRY_BOHE_HOLDER.get();
        NEKO_TAIL = NEKO_TAIL_HOLDER.get();
        NEKO_EARS = NEKO_EARS_HOLDER.get();
        NEKO_POTION = NEKO_POTION_HOLDER.get();
        MUSIC_DISC_KAWAII = MUSIC_DISC_KAWAII_HOLDER.get();
        MUSIC_DISC_NEVER_GONNA_GIVE_YOU_UP = MUSIC_DISC_NEVER_GONNA_GIVE_YOU_UP_HOLDER.get();
        BAZOOKA = BAZOOKA_HOLDER.get();
        PLOT_SCROLL = PLOT_SCROLL_HOLDER.get();
        LIGHTNING_BOMB = LIGHTNING_BOMB_HOLDER.get();
        EXPLOSIVE_BOMB = EXPLOSIVE_BOMB_HOLDER.get();
        CONTRACT = CONTRACT_HOLDER.get();
        ToNekoEffectNeoForge.reg();
        ToNekoBlocks.reg();
        ToNekoEntities.reg();
        ToNekoCriteriaNeoForge.reg();
    }
}
