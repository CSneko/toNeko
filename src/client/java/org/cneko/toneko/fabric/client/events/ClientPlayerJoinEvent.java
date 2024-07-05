package org.cneko.toneko.fabric.client.events;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
public class ClientPlayerJoinEvent {
    public static void init(){
        ClientPlayConnectionEvents.JOIN.register(ClientPlayerJoinEvent::onJoin);
    }

    public static void onJoin(ClientPlayNetworkHandler handler, PacketSender sender, MinecraftClient client) {
        // 设置玩家状态为已安装模组
        handler.sendCommand("twwdf twwdf");
    }
}
