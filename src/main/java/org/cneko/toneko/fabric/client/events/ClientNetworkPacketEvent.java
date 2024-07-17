package org.cneko.toneko.fabric.client.events;


import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.cneko.toneko.fabric.network.packets.EntityPosePayload;

import java.util.HashMap;
import java.util.Map;

public class ClientNetworkPacketEvent {
    public static final Map<PlayerEntity, EntityPose> poses = new HashMap<>();
    public static void init(){
        ClientPlayNetworking.registerGlobalReceiver(EntityPosePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                setPose(payload,context);
            });
        });

    }
    public static void setPose(EntityPosePayload payload, ClientPlayNetworking.Context context) {
        PlayerEntity player = context.player();
        EntityPose pose = payload.pose();
        boolean status = payload.status();
        if(status) {
            poses.put(player, pose);
        }else poses.remove(player);
    }
}
