package org.cneko.toneko.common.mod.api;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import org.cneko.toneko.common.mod.packets.EntityPosePayload;

/**
 * 玩家姿势钉定的一次性同步助手。
 *
 * 设计原则：姿势的权威状态保存在服务端 EntityPoseManager 中，并通过对
 * Entity#setPose 的普通写入走原版 DATA_POSE 同步链路（自动同步给所有客户端、
 * 自动刷新碰撞箱）。自定义网络包只用于「本人客户端」的预测对齐：
 * 仅在钉定/解除的瞬间发送一次，不再轮询广播。
 */
public final class PoseStateSync {
    private PoseStateSync() {}

    /** 钉定玩家姿势，并一次性通知本人客户端。 */
    public static void pin(ServerPlayer player, Pose pose) {
        EntityPoseManager.setPose(player, pose);
        player.setPose(pose);
        ServerPlayNetworking.send(player, new EntityPosePayload(pose, "self", true));
    }

    /** 解除玩家姿势钉定；仅当原本存在钉定时发送一次清除包。 */
    public static void unpin(ServerPlayer player) {
        Pose prev = EntityPoseManager.getNullablePose(player);
        MushroomBedRest.release(player);
        if (prev == null) {
            return;
        }
        EntityPoseManager.remove(player);
        ServerPlayNetworking.send(player, new EntityPosePayload(prev, "self", false));
    }
}
