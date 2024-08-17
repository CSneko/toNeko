package org.cneko.toneko.neoforge.items;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.cneko.toneko.common.mod.items.FurryBoheItem;
import org.cneko.toneko.common.mod.items.NekoArmor;
import org.cneko.toneko.common.mod.items.NekoPotionItem;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.neoforge.fabric.items.NekoCollectorItem;

import java.util.function.Supplier;

import static org.cneko.toneko.common.Bootstrap.MODID;
import static org.cneko.toneko.neoforge.ToNekoNeoForge.CREATIVE_MODE_TABS;
import static org.cneko.toneko.neoforge.ToNekoNeoForge.ITEMS;

public class ToNekoItems {
    public static DeferredHolder<Item,Item> NEKO_EARS;
    public static DeferredHolder<Item,Item> NEKO_POTION;
    public static DeferredHolder<Item,Item> NEKO_COLLECTOR;
    public static DeferredHolder<Item,Item> NEKO_TAIL;
    public static DeferredHolder<Item,Item> FURRY_BOHE;

    public static ResourceKey<CreativeModeTab> TONEKO_ITEM_GROUP_KEY;
    public static Supplier<CreativeModeTab> TONEKO_ITEM_GROUP;
    public static boolean isGeckolibInstalled = false; //tryClass("software.bernie.geckolib.animatable.GeoItem");
    public static void init() {
        // 如果启用了仅服务器端，则不注册物品
        if (!ConfigUtil.ONLY_SERVER) registerWithOutConfig();
    }

    /**
     * 强制注册物品，无论配置文件如何设置
     */
    public static void registerWithOutConfig() {
        NEKO_POTION = ITEMS.register(NekoPotionItem.ID, NekoPotionItem::new);
        NEKO_COLLECTOR = ITEMS.register(NekoCollectorItem.ID,NekoCollectorItem::new);
        FURRY_BOHE = ITEMS.register(FurryBoheItem.ID, FurryBoheItem::new);
        DeferredHolder<Item, Item> showItem = NEKO_POTION;
        // 如果安装了geckolib，则注册为ArmorItem
        if (isGeckolibInstalled) {
            NEKO_EARS = ITEMS.register(NekoArmor.NekoEarsItem.ID, NekoArmor.NekoEarsItem::new);
            NEKO_TAIL = ITEMS.register(NekoArmor.NekoTailItem.ID,NekoArmor.NekoTailItem::new);
            ITEMS.register(NekoArmor.NekoPawsItem.ID, NekoArmor.NekoPawsItem::new); // 此物品暂不添加
            showItem = NEKO_EARS;
        }
        // 注册物品
        DeferredHolder<Item, Item> finalShowItem = showItem;
        TONEKO_ITEM_GROUP = CREATIVE_MODE_TABS.register("item_group",() -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.toneko"))
                .icon(() -> new ItemStack(finalShowItem.get().asItem()))
                .displayItems((params, content) -> {
                    content.accept(NEKO_POTION.get());
                    content.accept(NEKO_COLLECTOR.get());
                    if (isGeckolibInstalled) {
                        content.accept(NEKO_EARS.get());
                        content.accept(NEKO_TAIL.get());
                    }
                })
                .build()
        );
        // 注册物品组
        TONEKO_ITEM_GROUP_KEY = ResourceKey.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(), ResourceLocation.fromNamespaceAndPath(MODID, "item_group"));

//        ItemGroupEvents.modifyEntriesEvent(TONEKO_ITEM_GROUP_KEY).register(content -> {
//            content.accept(NEKO_POTION.get());
//            content.accept(NEKO_COLLECTOR.get());
//            if (isGeckolibInstalled) {
//                content.accept(NEKO_EARS.get());
//                content.accept(NEKO_TAIL.get());
//            }
//        });
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
        if (event.getTabKey().equals(TONEKO_ITEM_GROUP_KEY)) {
            event.accept(NEKO_POTION.get());
            event.accept(NEKO_COLLECTOR.get());
            event.accept(FURRY_BOHE.get());
            if (isGeckolibInstalled) {
                event.accept(NEKO_EARS.get());
                event.accept(NEKO_TAIL.get());
            }
        }
    }
}
