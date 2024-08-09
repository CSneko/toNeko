package org.cneko.toneko.neoforge.items;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.cneko.toneko.common.mod.items.NekoArmor;
import org.cneko.toneko.common.mod.items.NekoPotionItem;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.neoforge.fabric.items.NekoCollectorItem;

import static org.cneko.toneko.common.Bootstrap.MODID;
import static org.cneko.toneko.neoforge.ToNekoNeoForge.CREATIVE_MODE_TABS;
import static org.cneko.toneko.neoforge.ToNekoNeoForge.ITEMS;

public class ToNekoItems {
    public static DeferredHolder<Item,Item> NEKO_EARS;
    public static DeferredHolder<Item,Item> NEKO_POTION;
    public static DeferredHolder<Item,Item> NEKO_COLLECTOR;
    public static DeferredHolder<Item,Item> NEKO_TAIL;

    public static ResourceKey<CreativeModeTab> TONEKO_ITEM_GROUP_KEY;
    public static CreativeModeTab TONEKO_ITEM_GROUP;
    public static boolean isGeckolibInstalled = tryClass("software.bernie.geckolib.animatable.GeoItem");
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
        // 如果安装了geckolib，则注册为ArmorItem
        if (isGeckolibInstalled) {
            NEKO_EARS = ITEMS.register(NekoArmor.NekoEarsItem.ID, NekoArmor.NekoEarsItem::new);
            NEKO_TAIL = ITEMS.register(NekoArmor.NekoTailItem.ID,NekoArmor.NekoTailItem::new);
            ITEMS.register(NekoArmor.NekoPawsItem.ID, NekoArmor.NekoPawsItem::new); // 此物品暂不添加
            TONEKO_ITEM_GROUP = FabricItemGroup.builder()
                    .icon(() -> new ItemStack(NEKO_EARS))
                    .title(Component.translatable("itemGroup.toneko"))
                    .build();
        }else {
            TONEKO_ITEM_GROUP = FabricItemGroup.builder()
                    .icon(() -> new ItemStack(NEKO_POTION))
                    .title(Component.translatable("itemGroup.toneko"))
                    .build();
        }
        // 注册物品组
        TONEKO_ITEM_GROUP_KEY = ResourceKey.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(), ResourceLocation.fromNamespaceAndPath(MODID, "item_group"));
        CREATIVE_MODE_TABS.register("item_group",()->TONEKO_ITEM_GROUP);

        ItemGroupEvents.modifyEntriesEvent(TONEKO_ITEM_GROUP_KEY).register(content -> {
            content.accept(NEKO_POTION.get());
            content.accept(NEKO_COLLECTOR.get());
            if (isGeckolibInstalled) {
                content.accept(NEKO_EARS.get());
                content.accept(NEKO_TAIL.get());
            }
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
