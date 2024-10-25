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
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.cneko.toneko.common.mod.items.FurryBoheItem;
import org.cneko.toneko.common.mod.items.NekoArmor;
import org.cneko.toneko.common.mod.items.NekoCollectorItem;
import org.cneko.toneko.common.mod.items.NekoPotionItem;
import org.cneko.toneko.common.util.ConfigUtil;
import static org.cneko.toneko.common.mod.items.ToNekoItems.*;

import java.util.function.Supplier;

import static org.cneko.toneko.common.Bootstrap.MODID;
import static org.cneko.toneko.neoforge.ToNekoNeoForge.ITEMS;

public class ToNekoItems {

    public static ResourceKey<CreativeModeTab> TONEKO_ITEM_GROUP_KEY;
    public static Supplier<CreativeModeTab> TONEKO_ITEM_GROUP;
    public static void init() {
        registerWithOutConfig();
    }

    /**
     * 强制注册物品，无论配置文件如何设置
     */
    public static void registerWithOutConfig() {
        NEKO_POTION = ITEMS.register(NekoPotionItem.ID, NekoPotionItem::new).get();
        NEKO_COLLECTOR = ITEMS.register(NekoCollectorItem.ID, NekoCollectorItem::new).get();
        FURRY_BOHE = ITEMS.register(FurryBoheItem.ID, FurryBoheItem::new).get();

        NEKO_EARS = ITEMS.register(NekoArmor.NekoEarsItem.ID, NekoArmor.NekoEarsItem::new).get();
        NEKO_TAIL = ITEMS.register(NekoArmor.NekoTailItem.ID,NekoArmor.NekoTailItem::new).get();
        ITEMS.register(NekoArmor.NekoPawsItem.ID, NekoArmor.NekoPawsItem::new); // 此物品暂不添加
        // 注册物品组
        TONEKO_ITEM_GROUP_KEY = ResourceKey.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(), ResourceLocation.fromNamespaceAndPath(MODID, "item_group"));
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TONEKO_ITEM_GROUP_KEY,TONEKO_ITEM_GROUP.get());
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
            event.accept(NEKO_POTION);
            event.accept(NEKO_COLLECTOR);
            event.accept(FURRY_BOHE);
            event.accept(NEKO_EARS);
            event.accept(NEKO_TAIL);
        }
    }
}
