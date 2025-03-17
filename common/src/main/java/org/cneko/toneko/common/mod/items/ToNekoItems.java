package org.cneko.toneko.common.mod.items;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoItems {
    public static FurryBoheItem FURRY_BOHE;
    public static NekoPotionItem NEKO_POTION ;
    public static NekoCollectorItem NEKO_COLLECTOR;
    public static CatnipItem CATNIP;
    public static CatnipItem CATNIP_SANDWICH;
    public static Item CATNIP_SEED;
    public static Item MUSIC_DISC_KAWAII;

    public static final TagKey<Item> CATNIP_TAG = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c","foods/catnip"));

    public static ResourceKey<Item> key(String id){
        return ResourceKey.create(BuiltInRegistries.ITEM.key(), ResourceLocation.fromNamespaceAndPath(MODID, id));
    }
}
