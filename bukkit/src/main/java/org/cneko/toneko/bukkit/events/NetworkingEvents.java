package org.cneko.toneko.bukkit.events;

import org.bukkit.entity.Player;
import org.cneko.toneko.bukkit.api.ClientStatus;

import java.util.logging.Level;

import static org.cneko.toneko.bukkit.ToNeko.INSTANCE;
public class NetworkingEvents {
    public static void init(){
        INSTANCE.getServer().getMessenger().registerIncomingPluginChannel(
                INSTANCE,
                "toneko:detect",
                NetworkingEvents::onClientResponse
        );
        INSTANCE.getServer().getMessenger().registerOutgoingPluginChannel(
                INSTANCE,
                "toneko:detect"
        );
    }

    private static void onClientResponse(String channel, Player player, byte[] data) {
        if (channel.equalsIgnoreCase("toneko:detect")) {
            ClientStatus.setInstalled(player,true); // 标记玩家已安装模组
            INSTANCE.getLogger().log(Level.INFO, "玩家 {0} 安装了toNeko模组", player.getName());
        }
    }
}
