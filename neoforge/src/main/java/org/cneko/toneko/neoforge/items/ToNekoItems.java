package org.cneko.toneko.neoforge.items;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.cneko.toneko.common.mod.items.FurryBoheItem;
import org.cneko.toneko.common.mod.items.NekoArmor;
import org.cneko.toneko.common.mod.items.NekoCollectorItem;
import org.cneko.toneko.common.mod.items.NekoPotionItem;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.neoforge.ToNekoNeoForge;
import org.cneko.toneko.neoforge.entities.ToNekoEntities;

import static org.cneko.toneko.common.mod.items.ToNekoItems.*;

import java.util.function.Supplier;

import static org.cneko.toneko.common.Bootstrap.MODID;
import static org.cneko.toneko.neoforge.ToNekoNeoForge.ITEMS;


public class ToNekoItems {

    public static DeferredHolder<Item,DeferredSpawnEggItem> ADVENTURER_NEKO_SPAWN_EGG_HOLDER;
    public static DeferredHolder<Item,NekoPotionItem> NEKO_POTION_HOLDER;
    public static DeferredHolder<Item,NekoCollectorItem> NEKO_COLLECTOR_HOLDER;
    public static DeferredHolder<Item,FurryBoheItem> FURRY_BOHE_HOLDER;
    public static DeferredHolder<Item,NekoArmor.NekoEarsItem> NEKO_EARS_HOLDER;
    public static DeferredHolder<Item,NekoArmor.NekoTailItem> NEKO_TAIL_HOLDER;
    public static DeferredHolder<CreativeModeTab,CreativeModeTab> TONEKO_ITEM_GROUP_HOLDER;

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

        NEKO_EARS_HOLDER = ITEMS.register(NekoArmor.NekoEarsItem.ID, NekoArmor.NekoEarsItem::new);

        NEKO_TAIL_HOLDER = ITEMS.register(NekoArmor.NekoTailItem.ID, NekoArmor.NekoTailItem::new);

        ADVENTURER_NEKO_SPAWN_EGG_HOLDER = ITEMS.register("adventurer_neko_spawn_egg",()->new DeferredSpawnEggItem(()->ToNekoEntities.ADVENTURER_NEKO_HOLDER.get(), 0x7e7e7e, 0xffffff,new Item.Properties()));

        ITEMS.register(NekoArmor.NekoPawsItem.ID, NekoArmor.NekoPawsItem::new); // 此物品暂不添加
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
        }
    }

    @SubscribeEvent
    public static void registerEvent(FMLCommonSetupEvent event){
        NEKO_POTION = NEKO_POTION_HOLDER.get();
        NEKO_COLLECTOR = NEKO_COLLECTOR_HOLDER.get();
        FURRY_BOHE = FURRY_BOHE_HOLDER.get();
        NEKO_TAIL = NEKO_TAIL_HOLDER.get();
        NEKO_EARS = NEKO_EARS_HOLDER.get();
    }
}
