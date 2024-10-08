package org.cneko.toneko.fabric.items;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.cneko.toneko.common.mod.items.FurryBoheItem;
import org.cneko.toneko.common.mod.items.NekoArmor;
import org.cneko.toneko.common.mod.items.NekoPotionItem;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.fabric.entities.ToNekoEntities;

import static org.cneko.toneko.common.Bootstrap.MODID;


public class ToNekoItems {
    public static NekoPotionItem NEKO_POTION;
    public static NekoCollectorItem NEKO_COLLECTOR;
    public static NekoArmor.NekoEarsItem NEKO_EARS;
    public static NekoArmor.NekoTailItem NEKO_TAIL;
    public static NekoArmor.NekoPawsItem NEKO_PAWS;
    public static FurryBoheItem FURRY_BOHE;
    public static ResourceKey<CreativeModeTab> TONEKO_ITEM_GROUP_KEY;
    public static CreativeModeTab TONEKO_ITEM_GROUP;
    public static final SpawnEggItem ADVENTURER_NEKO_SPAWN_EGG = new SpawnEggItem(ToNekoEntities.ADVENTURER_NEKO, 0x7e7e7e, 0xffffff,new Item.Properties());
    public static boolean isTrinketsInstalled = tryClass("dev.emi.trinkets.api.Trinket");
    public static void init() {
        // 如果启用了仅服务器端，则不注册物品
        registerWithOutConfig();
    }

    /**
     * 强制注册物品，无论配置文件如何设置
     */
    public static void registerWithOutConfig() {
        NEKO_POTION = new NekoPotionItem();
        NEKO_COLLECTOR = new NekoCollectorItem();
        FURRY_BOHE = new FurryBoheItem();
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MODID, NekoPotionItem.ID), NEKO_POTION);
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MODID, NekoCollectorItem.ID), NEKO_COLLECTOR);
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MODID, FurryBoheItem.ID), FURRY_BOHE);
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MODID, "adventurer_neko_spawn_egg"), ADVENTURER_NEKO_SPAWN_EGG);

        // 如果安装了trinkets，则注册为TrinketItem
        if (isTrinketsInstalled){
            NekoArmorTrinkets.init();
        }else {
            NEKO_EARS = new NekoArmor.NekoEarsItem();
            NEKO_TAIL = new NekoArmor.NekoTailItem();
            NEKO_PAWS = new NekoArmor.NekoPawsItem();
        }
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MODID, NekoArmor.NekoEarsItem.ID), NEKO_EARS);
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MODID, NekoArmor.NekoTailItem.ID), NEKO_TAIL);
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MODID, NekoArmor.NekoPawsItem.ID), NEKO_PAWS);
        TONEKO_ITEM_GROUP = FabricItemGroup.builder()
                .icon(() -> new ItemStack(NEKO_EARS))
                .title(Component.translatable("itemGroup.toneko"))
                .build();
        // 注册物品组
        TONEKO_ITEM_GROUP_KEY = ResourceKey.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(), new ResourceLocation(MODID, "item_group"));
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TONEKO_ITEM_GROUP_KEY,TONEKO_ITEM_GROUP);

        ItemGroupEvents.modifyEntriesEvent(TONEKO_ITEM_GROUP_KEY).register(content -> {
            content.accept(NEKO_POTION);
            content.accept(NEKO_COLLECTOR);
            content.accept(FURRY_BOHE);
            content.accept(NEKO_EARS);
            content.accept(NEKO_TAIL);
            content.accept(ADVENTURER_NEKO_SPAWN_EGG);
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
