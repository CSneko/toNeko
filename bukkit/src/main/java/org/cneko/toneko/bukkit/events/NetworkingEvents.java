package org.cneko.toneko.bukkit.events;

import org.bukkit.entity.Player;
import org.cneko.toneko.bukkit.ChatModeHolder;
import org.cneko.toneko.bukkit.api.ClientStatus;

import java.util.logging.Level;

import static org.cneko.toneko.bukkit.ToNeko.INSTANCE;
public class NetworkingEvents {
    private static final String[] CHANNELS = {
        "toneko:detect",
        "toneko:chat_history_response",
        "toneko:tts_send",
        "toneko:entity_pose",
        "toneko:neko_info_sync",
        "toneko:chat_mode",
        "toneko:management_data",
        "toneko:open_neko_info_screen",
        "toneko:quirk_query"
    };

    public static void init(){
        for (String ch : CHANNELS) {
            INSTANCE.getServer().getMessenger().registerOutgoingPluginChannel(INSTANCE, ch);
        }
        INSTANCE.getServer().getMessenger().registerIncomingPluginChannel(
                INSTANCE,
                "toneko:detect",
                NetworkingEvents::onClientResponse
        );
        INSTANCE.getServer().getMessenger().registerIncomingPluginChannel(
                INSTANCE,
                "toneko:chat_mode",
                NetworkingEvents::onChatMode
        );
        // quirk_query is handled by /quirk gui command (command → S2C payload)
    }

    private static void onClientResponse(String channel, Player player, byte[] data) {
        if (channel.equalsIgnoreCase("toneko:detect")) {
            ClientStatus.setInstalled(player,true);
            INSTANCE.getLogger().log(Level.INFO, "玩家 {0} 安装了toNeko模组", player.getName());
        }
    }

    private static void onChatMode(String channel, Player player, byte[] data) {
        if (data.length > 0) {
            ChatModeHolder.setAreaChat(player.getUniqueId(), data[0] != 0);
        }
    }

}
