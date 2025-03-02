package org.cneko.toneko.common.mod.items;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.JukeboxSong;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoSongs {
    public static final ResourceKey<JukeboxSong> KAWAII = create("kawaii");

    private static ResourceKey<JukeboxSong> create(String name) {
        return ResourceKey.create(Registries.JUKEBOX_SONG, ResourceLocation.fromNamespaceAndPath(MODID,name));
    }
}
