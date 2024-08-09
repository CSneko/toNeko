package org.cneko.toneko.fabric.events;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import static org.cneko.toneko.fabric.api.PlayerPoseAPI.poseMap;
public class PlayerTickEvent {
    public static void init(){
        ServerTickEvents.START_SERVER_TICK.register(PlayerTickEvent::startTick);
    }


    public static void startTick(MinecraftServer server) {
        // 强行设置玩家的动作
        poseMap.forEach((player, pose) -> {
            if (player == null){
                return;
            }
            player.setPose(pose);
        });
    }
}
