package org.cneko.toneko.fabric.util;

import org.cneko.toneko.fabric.ModMeta;

import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class PlayerUtil {
    public static Player getPlayerByName(String name) {
        return ModMeta.INSTANCE.getServer().getPlayerList().getPlayerByName(name);
    }
    public static Player getPlayerByUUID(UUID uuid) {
        return ModMeta.INSTANCE.getServer().getPlayerList().getPlayer(uuid);
    }
    public static List<ServerPlayer> getPlayerList() {
        return ModMeta.INSTANCE.getServer().getPlayerList().getPlayers();
    }
}
