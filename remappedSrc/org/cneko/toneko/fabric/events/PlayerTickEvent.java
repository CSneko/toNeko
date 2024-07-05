package org.cneko.toneko.fabric.events;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

public class PlayerTickEvent {
    public static List<PlayerEntity> lyingPlayers = new ArrayList<>();
    public static void init(){
        ServerTickEvents.START_SERVER_TICK.register(PlayerTickEvent::startTick);
    }


    public static void startTick(MinecraftServer server) {
        // 设置所有躺下的玩家动作为SLEEPING
        lyingPlayers.forEach(player -> {
            // 如果玩家为空，则移除
            if (player == null) {
                lyingPlayers.remove(player);
                return;
            }
            player.setPose(EntityPose.SLEEPING);
        });
    }
}
