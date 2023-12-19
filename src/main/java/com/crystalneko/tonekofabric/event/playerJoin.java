package com.crystalneko.tonekofabric.event;

import com.crystalneko.ctlibPublic.inGame.chatPrefix;
import com.crystalneko.tonekofabric.libs.base;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public class playerJoin {
    public playerJoin(){
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = (ServerPlayerEntity) handler.player;
            String playerName = base.getPlayerName(player);
            String worldName = base.getWorldName(player.getWorld());
            String prefix = base.translatable("chat.neko.prefix").getString();
            //判断玩家是否为猫娘
            if(base.isNekoHasOwner(playerName,worldName) != null){
                chatPrefix.addPrivatePrefix(playerName,prefix);
            }
            base.start(worldName);
        });
    }
}
