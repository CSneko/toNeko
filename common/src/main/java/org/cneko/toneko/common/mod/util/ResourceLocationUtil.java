package org.cneko.toneko.common.mod.util;

import net.minecraft.resources.Identifier;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ResourceLocationUtil {
    public static Identifier toNekoLoc(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
