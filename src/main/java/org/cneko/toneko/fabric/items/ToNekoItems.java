package org.cneko.toneko.fabric.items;

import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.cneko.toneko.common.util.ConfigUtil;

import static org.cneko.toneko.common.Bootstrap.LOGGER;
import static org.cneko.toneko.common.Bootstrap.MODID;


public class ToNekoItems {
    public static NekoPotionItem NEKO_POTION;
    public static NekoTailItem NEKO_TAIL;
    public static void init() {
        // 如果启用了仅服务器端，则不注册物品
        if (!ConfigUtil.ONLY_SERVER) registerWithOutConfig();
    }

    /**
     * 强制注册物品，无论配置文件如何设置
     */
    public static void registerWithOutConfig() {
        NEKO_POTION = new NekoPotionItem();
        Registry.register(Registries.ITEM, Identifier.of(MODID, NekoPotionItem.ID), NEKO_POTION);

        boolean isGeckolibInstalled = false;
        boolean isTrinketsInstalled = false;
        try {
            Class.forName("software.bernie.geckolib.animatable.GeoItem");
            isGeckolibInstalled = true;
        } catch (Exception ignored){}
        try{
            Class.forName("dev.emi.trinkets.api.TrinketItem");
            isTrinketsInstalled = true;
        }catch (Exception ignored){}

        // 如果安装了geckolib，则注册为ArmorItem
        if (isGeckolibInstalled) {
            NEKO_TAIL = new NekoTailItem();
            Registry.register(Registries.ITEM, Identifier.of(MODID, NekoTailItem.ID), NEKO_TAIL);
            // 如果安装了trinkets，则注册为TrinketItem
            if (isTrinketsInstalled){
                LOGGER.info("Trinkets detected, registering Neko Armors as TrinketItem");
                TrinketsApi.registerTrinket(NEKO_TAIL,NEKO_TAIL);
            }
        }

        // 注册到物品组
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(content -> {
            content.add(NEKO_POTION);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(content -> {
            if (NEKO_TAIL != null) {
                content.add(NEKO_TAIL);
            }
        });
    }

}
