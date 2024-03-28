package com.crystalneko.tonekofabric.event;

import com.crystalneko.tonekofabric.api.Messages;
import com.crystalneko.tonekofabric.api.Query;
import com.crystalneko.tonekofabric.libs.base;
import com.crystalneko.tonekofabric.util.TextUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.cneko.ctlib.common.util.ChatPrefix;
import static com.crystalneko.tonekofabric.api.Messages.translatable;
public class playerJoin {
    public playerJoin(){
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            String playerName = TextUtil.getPlayerName(player);
            String worldName = TextUtil.getWorldName(player.getWorld());
            String prefix = Messages.translatable("chat.neko.prefix").getString();
            //判断玩家是否为猫娘
            if(Query.hasOwner(playerName,worldName)){
                ChatPrefix.addPrivatePrefix(playerName,prefix);
            }

            // 发送欢迎消息
            player.sendMessage(translatable("msg.toneko.join",playerName));

            base.start(worldName);
        });
    }
}
