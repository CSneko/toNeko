package org.cneko.toneko.fabric.items;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.cneko.toneko.common.util.ConfigUtil;

import static org.cneko.toneko.common.Bootstrap.MODID;


public class ToNekoItems {
    public static NekoPotionItem NEKO_POTION;
    public static NekoCollectorItem NEKO_COLLECTOR;
    public static NekoArmor.NekoEarsItem NEKO_EARS;
    public static NekoArmor.NekoTailItem NEKO_TAIL;
    public static NekoArmor.NekoPawsItem NEKO_PAWS;
    public static boolean isGeckolibInstalled = FabricLoader.getInstance().isModLoaded("geckolib");
    public static boolean isTrinketsInstalled = FabricLoader.getInstance().isModLoaded("trinkets");
    public static void init() {
        // 如果启用了仅服务器端，则不注册物品
        if (!ConfigUtil.ONLY_SERVER) registerWithOutConfig();
    }

    /**
     * 强制注册物品，无论配置文件如何设置
     */
    public static void registerWithOutConfig() {
        NEKO_POTION = new NekoPotionItem();
        NEKO_COLLECTOR = new NekoCollectorItem();
        Registry.register(Registries.ITEM, Identifier.of(MODID, NekoPotionItem.ID), NEKO_POTION);
        Registry.register(Registries.ITEM, Identifier.of(MODID, NekoCollectorItem.ID), NEKO_COLLECTOR);

        // 如果安装了geckolib，则注册为ArmorItem
        if (isGeckolibInstalled) {
            // 如果安装了trinkets，则注册为TrinketItem
            if (isTrinketsInstalled){
                NekoArmorTrinkets.init();
            }else {
                NEKO_EARS = new NekoArmor.NekoEarsItem();
                NEKO_TAIL = new NekoArmor.NekoTailItem();
                NEKO_PAWS = new NekoArmor.NekoPawsItem();
            }
            Registry.register(Registries.ITEM, Identifier.of(MODID, NekoArmor.NekoEarsItem.ID), NEKO_EARS);
            Registry.register(Registries.ITEM, Identifier.of(MODID, NekoArmor.NekoTailItem.ID), NEKO_TAIL);
            Registry.register(Registries.ITEM, Identifier.of(MODID, NekoArmor.NekoPawsItem.ID), NEKO_PAWS);
        }

        // 注册到物品组
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(content -> {
            content.add(NEKO_POTION);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(content -> {
            content.add(NEKO_COLLECTOR);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(content -> {
            if (isGeckolibInstalled) {
                content.add(NEKO_EARS);
                content.add(NEKO_TAIL);
            }
        });
    }

}
