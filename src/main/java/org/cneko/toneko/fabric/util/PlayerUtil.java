package org.cneko.toneko.fabric.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.cneko.toneko.fabric.ModMeta;

import java.util.List;
import java.util.UUID;

public class PlayerUtil {
    public static PlayerEntity getPlayerByName(String name) {
        return ModMeta.INSTANCE.getServer().getPlayerManager().getPlayer(name);
    }
    public static PlayerEntity getPlayerByUUID(UUID uuid) {
        return ModMeta.INSTANCE.getServer().getPlayerManager().getPlayer(uuid);
    }
    public static List<ServerPlayerEntity> getPlayerList() {
        return ModMeta.INSTANCE.getServer().getPlayerManager().getPlayerList();
    }
}
