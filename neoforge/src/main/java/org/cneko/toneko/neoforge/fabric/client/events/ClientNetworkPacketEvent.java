package org.cneko.toneko.neoforge.fabric.client.events;


import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.packets.EntityPosePayload;
import org.cneko.toneko.common.mod.packets.QuirkQueryPayload;
import org.cneko.toneko.neoforge.fabric.client.screens.QuirkScreen;

import java.util.HashMap;
import java.util.Map;

public class ClientNetworkPacketEvent {
    public static final Map<Player, Pose> poses = new HashMap<>();
    public static void init(){
        ClientPlayNetworking.registerGlobalReceiver(EntityPosePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                setPose(payload,context);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(QuirkQueryPayload.ID, (payload, context) ->{
            if (payload.isOpenScreen()) {
                // 打开屏幕
                context.client().execute(() -> {
                    // 打开设置屏幕
                    context.client().setScreen(new QuirkScreen(context.client().screen,payload.getQuirks(),payload.getAllQuirks()));
                });
            }
        });

    }
    public static void setPose(EntityPosePayload payload, ClientPlayNetworking.Context context) {
        Player player = context.player();
        Pose pose = payload.pose();
        boolean status = payload.status();
        if(status) {
            poses.put(player, pose);
        }else poses.remove(player);
    }
}
