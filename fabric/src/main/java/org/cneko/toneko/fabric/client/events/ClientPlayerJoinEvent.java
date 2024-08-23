package org.cneko.toneko.fabric.client.events;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public class ClientPlayerJoinEvent {
    public static void init(){
        ClientPlayConnectionEvents.JOIN.register(ClientPlayerJoinEvent::onJoin);
    }

    public static void onJoin(ClientPacketListener handler, PacketSender sender, Minecraft client) {
    }
}
