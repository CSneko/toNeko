package org.cneko.toneko.common.mod.util;

import net.minecraft.resources.ResourceLocation;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ResourceLocationUtil {
    public static ResourceLocation toNekoLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
