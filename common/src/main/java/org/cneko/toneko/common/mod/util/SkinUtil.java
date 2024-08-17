package org.cneko.toneko.common.mod.util;

import net.minecraft.server.level.ServerPlayer;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.api.NekoSkin;

import java.util.UUID;

public class SkinUtil {
    public static boolean isInstalled = tryClass("net.lionarius.skinrestorer.SkinRestorer");
    public static void tryToSetSkin(ServerPlayer player) {
        if (!isInstalled) return;
        UUID uuid = player.getUUID();
        NekoQuery.Neko neko = NekoQuery.getNeko(uuid);
        if (neko.hasSkin()){
            NekoSkin skin = neko.getSkin();
            player.server.getCommands().performPrefixedCommand(player.createCommandSourceStack(), "skin set mojang " + skin.getSkin());
        }
    }

    public static boolean tryClass(String clazz){
        try {
            Class.forName(clazz);
            return true;
        }catch (Exception e){
            return false;
        }
    }
}
