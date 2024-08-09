package org.cneko.toneko.neoforge.fabric.client.events;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

public class ClientPlayerJoinEvent {
    public static void init(){
        ClientPlayConnectionEvents.JOIN.register(ClientPlayerJoinEvent::onJoin);
    }

    public static void onJoin(ClientPacketListener handler, PacketSender sender, Minecraft client) {
        // 设置玩家状态为已安装模组
        handler.sendUnsignedCommand("twwdf twwdf");
        // 提醒玩家出错是正常的
        if (client.player != null) {
            client.player.sendSystemMessage(Component.translatable("messages.toneko.client.join"));
        }
    }
}
