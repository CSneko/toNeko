package org.cneko.toneko.common.mod.misc;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.JukeboxSong;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoSongs {
    public static final ResourceKey<JukeboxSong> KAWAII = create("kawaii");
    public static final ResourceKey<JukeboxSong> NEVER_GONNA_GIVE_YOU_UP = create("never_gonna_give_you_up");

    private static ResourceKey<JukeboxSong> create(String name) {
        return ResourceKey.create(Registries.JUKEBOX_SONG, ResourceLocation.fromNamespaceAndPath(MODID,name));
    }
}
