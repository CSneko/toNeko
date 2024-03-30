package com.crystalneko.tonekofabric.event;

import com.crystalneko.tonekofabric.api.Messages;
import com.crystalneko.tonekofabric.api.Query;
import com.crystalneko.tonekofabric.util.TextUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.cneko.ctlib.common.util.ChatPrefix;

import static com.crystalneko.tonekofabric.api.Messages.translatable;

public class PlayerLeave {
    public PlayerLeave(){
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.player;
            String playerName = TextUtil.getPlayerName(player);
            String worldName = TextUtil.getWorldName(player.getWorld());
            String prefix = Messages.translatable("chat.neko.prefix").getString();
            //判断玩家是否为猫娘
            if(Query.isNeko(playerName, worldName)){
                ChatPrefix.removePrivatePrefix(playerName,prefix);
            }
        });
    }
}
