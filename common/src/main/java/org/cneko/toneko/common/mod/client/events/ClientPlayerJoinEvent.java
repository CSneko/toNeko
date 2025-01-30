package org.cneko.toneko.common.mod.client.events;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.cneko.toneko.common.api.TickTasks;
import org.cneko.toneko.common.mod.packets.PluginDetectPayload;
import org.cneko.toneko.common.mod.util.TickTaskQueue;

public class ClientPlayerJoinEvent {
    public static void init(){
        ClientPlayConnectionEvents.JOIN.register(ClientPlayerJoinEvent::onJoin);
    }

    public static void onJoin(ClientPacketListener handler, PacketSender sender, Minecraft client) {
        var t = new TickTaskQueue();
        t.addTask(40,()->{
            if (ClientPlayNetworking.canSend(PluginDetectPayload.ID)) {
                // 告诉服务端，客户端已安装
                ClientPlayNetworking.send(new PluginDetectPayload("YES"));
            }
        });
        TickTasks.addClient(t);
    }
}
