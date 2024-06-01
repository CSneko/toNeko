package org.cneko.toneko.fabric.util;

import net.minecraft.entity.player.PlayerEntity;
import org.cneko.toneko.fabric.ModMeta;

import java.util.UUID;

public class PlayerUtil {
    public static PlayerEntity getPlayerByName(String name) {
        return ModMeta.instance.getServer().getPlayerManager().getPlayer(name);
    }
    public static PlayerEntity getPlayerByUUID(UUID uuid) {
        return ModMeta.instance.getServer().getPlayerManager().getPlayer(uuid);
    }
}
