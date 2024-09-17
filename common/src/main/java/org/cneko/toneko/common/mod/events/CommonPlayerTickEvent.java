package org.cneko.toneko.common.mod.events;

import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.api.TickTasks;

import static org.cneko.toneko.common.mod.api.EntityPoseManager.poseMap;

public class CommonPlayerTickEvent {
    public static void startTick(MinecraftServer server) {
        TickTasks.executeDefault();
        // 强行设置玩家的动作
        poseMap.forEach((player, pose) -> {
            if (player == null){
                return;
            }
            player.setPose(pose);
        });
    }
}
