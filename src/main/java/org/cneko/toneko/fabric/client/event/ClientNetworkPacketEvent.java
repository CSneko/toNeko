package org.cneko.toneko.fabric.client.event;


import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import org.cneko.toneko.fabric.network.packets.EntitySetPoseS2CPacket;

import java.util.HashMap;
import java.util.Map;

public class ClientNetworkPacketEvent {
    public static final Map<PlayerEntity, EntityPose> poses = new HashMap<>();
    public static void init(){
        ServerPlayNetworking.registerGlobalReceiver(EntitySetPoseS2CPacket.ID, (server, player, handler, buf, responseSender) -> {
            // 处理接收到的数据包
            setPose(player, buf);
        });
    }
    public static void setPose(PlayerEntity player, PacketByteBuf buf) {
        EntityPose pose = buf.readEnumConstant(EntityPose.class);
        boolean status = buf.readBoolean();
        if(status) {
            poses.put(player, pose);
        }else poses.remove(player);
    }
}
