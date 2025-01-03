package org.cneko.toneko.common.mod.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class ClientPlayerUtil {
    public static Player getPlayerByUUID(UUID uuid) {
        Minecraft client = Minecraft.getInstance();

        if (client.level != null) {
            for (Player player : client.level.players()) {
                if (player.getUUID().equals(uuid)) {
                    return player;
                }
            }
        }

        return null; // 未找到对应的玩家
    }
}
