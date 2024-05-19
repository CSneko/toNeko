package org.cneko.toneko.fabric.util;

import net.minecraft.entity.player.PlayerEntity;
import org.cneko.toneko.fabric.ModMeta;

public class PlayerUtil {
    public static PlayerEntity getPlayerByName(String name) {
        return ModMeta.instance.getServer().getPlayerManager().getPlayer(name);
    }
}
