package org.cneko.toneko.common.mod.events;

import net.minecraft.server.MinecraftServer;

import static org.cneko.toneko.common.mod.api.PlayerPoseAPI.poseMap;

public class CommonPlayerTickEvent {
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
