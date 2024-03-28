package com.crystalneko.tonekofabric.event;

import com.crystalneko.tonekofabric.api.Messages;
import com.crystalneko.tonekofabric.libs.base;
import com.crystalneko.tonekofabric.util.TextUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.cneko.ctlib.common.util.ChatPrefix;

import static com.crystalneko.tonekofabric.api.Messages.translatable;

public class playerLeave {
    public playerLeave(){
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = (ServerPlayerEntity) handler.player;
            String playerName = TextUtil.getPlayerName(player);
            String worldName = TextUtil.getWorldName(player.getWorld());
            String prefix = Messages.translatable("chat.neko.prefix").getString();
            //判断玩家是否为猫娘
            if(base.isNekoHasOwner(playerName,worldName) != null){
                ChatPrefix.removePrivatePrefix(playerName,prefix);
            }
            player.sendMessage(translatable("msg.toneko.leave",playerName));
        });
    }
}
