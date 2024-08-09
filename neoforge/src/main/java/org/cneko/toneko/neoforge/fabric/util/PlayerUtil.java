package org.cneko.toneko.neoforge.fabric.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.neoforge.fabric.ModMeta;

import java.util.List;
import java.util.UUID;

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
