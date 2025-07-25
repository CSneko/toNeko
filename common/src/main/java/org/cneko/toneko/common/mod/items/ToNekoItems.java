package org.cneko.toneko.common.mod.items;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

public class ToNekoItems {
    public static FurryBoheItem FURRY_BOHE;
    public static NekoPotionItem NEKO_POTION ;
    public static NekoArmor.NekoTailItem NEKO_TAIL;
    public static NekoArmor.NekoEarsItem NEKO_EARS;
    public static NekoArmor.NekoPawsItem NEKO_PAWS;
    public static NekoCollectorItem NEKO_COLLECTOR;
    public static CatnipItem CATNIP;
    public static CatnipItem CATNIP_SANDWICH;
    public static Item CATNIP_SEED;
    public static Item MUSIC_DISC_KAWAII;
    public static Item MUSIC_DISC_NEVER_GONNA_GIVE_YOU_UP;
    public static Item BAZOOKA;
    public static Item PLOT_SCROLL;
    public static Item LIGHTNING_BOMB;
    public static Item EXPLOSIVE_BOMB;
    public static Item CONTRACT;
    public static Item NEKO_AGGREGATOR_ITEM;
    public static Item NEKO_INGOT;

    public static final TagKey<Item> CATNIP_TAG = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c","foods/catnip"));
    public static final TagKey<Item> BAZOOKA_AMMO_TAG = TagKey.create(Registries.ITEM, toNekoLoc("bazooka_ammo"));
}
