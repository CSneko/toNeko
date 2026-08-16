package org.cneko.toneko.common.mod.api;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.packets.StompAnimPayload;
import org.cneko.toneko.common.mod.util.EntityUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端「玩味的踩」会话管理：记录谁在踩谁，负责开始/结束时的躺倒与起身，
 * 并在踩踏期间锁定非玩家目标（禁止其 AI 移动），以及处理踩踏者断线、目标死亡等清理。
 */
public class StompSessionManager {
    /** 踩踏动作允许的最远距离（格），与服务端校验保持一致。 */
    public static final double MAX_DISTANCE = 3.0;

    /** 踩踏者 UUID → 会话。 */
    private static final Map<UUID, StompSession> SESSIONS = new ConcurrentHashMap<>();

    private StompSessionManager() {}

    /** 一次踩踏会话。 */
    public record StompSession(UUID stomperUuid, UUID targetUuid, String part, String pose, boolean targetWasNoAi) {}

    /**
     * 开始踩踏：结束该踩踏者可能已有的旧会话，让目标躺倒、锁定非玩家目标，并广播开始。
     */
    public static void start(ServerPlayer stomper, LivingEntity target, String part, String pose) {
        UUID stomperUuid = stomper.getUUID();
        // 若该踩踏者已有会话（换目标重踩），先结束旧的
        StompSession old = SESSIONS.get(stomperUuid);
        if (old != null) {
            endSession(old, stomper.serverLevel(), stomper);
        }

        // 记录目标原本的 NoAi 状态，并对非玩家 Mob 锁定 AI（玩家除外，可自行挣脱）
        boolean wasNoAi = false;
        if (!(target instanceof Player) && target instanceof Mob mob) {
            wasNoAi = mob.isNoAi();
            mob.setNoAi(true);
        }

        StompSession session = new StompSession(stomperUuid, target.getUUID(), part, pose, wasNoAi);
        SESSIONS.put(stomperUuid, session);

        // 被踩者躺倒（仰面/趴着，当前统一用游泳躺姿，后续可按 pose 细化）
        EntityPoseManager.setPose(target, Pose.SWIMMING);

        broadcast(stomper, session, true);
    }

    /**
     * 结束踩踏（松开按键）：恢复目标姿态与 AI 并广播结束。
     */
    public static void stop(ServerPlayer stomper) {
        StompSession session = SESSIONS.remove(stomper.getUUID());
        if (session != null) {
            endSession(session, stomper.serverLevel(), stomper);
        }
    }

    /**
     * 踩踏者断线：恢复被踩者姿态与 AI。踩踏者实体即将移除，其动画随实体一并消失，
     * 无需向其它客户端广播「停止动画」。
     */
    public static void onPlayerQuit(ServerPlayer player) {
        StompSession session = SESSIONS.remove(player.getUUID());
        if (session == null) return;
        restoreTarget(player.serverLevel(), session);
    }

    /**
     * 目标死亡：若它正被踩，清理会话并确保姿态不再被强制保持。
     */
    public static void onTargetDeath(LivingEntity dead) {
        UUID deadUuid = dead.getUUID();
        // 找到所有以该实体为目标的会话并移除
        SESSIONS.values().removeIf(session -> {
            if (session.targetUuid().equals(deadUuid)) {
                EntityPoseManager.remove(dead);
                return true;
            }
            return false;
        });
    }

    private static void endSession(StompSession session, ServerLevel level, ServerPlayer stomper) {
        restoreTarget(level, session);
        broadcast(stomper, session, false);
    }

    /** 恢复目标：起身 + 恢复 AI。 */
    private static void restoreTarget(ServerLevel level, StompSession session) {
        Entity target = level.getEntity(session.targetUuid());
        if (!(target instanceof LivingEntity living) || living.isRemoved()) return;
        if (EntityPoseManager.contains(living)) {
            EntityPoseManager.remove(living);
        }
        // 恢复非玩家 Mob 的 AI
        if (!(living instanceof Player) && living instanceof Mob mob) {
            mob.setNoAi(session.targetWasNoAi());
        }
    }

    private static void broadcast(ServerPlayer stomper, StompSession session, boolean active) {
        StompAnimPayload anim = new StompAnimPayload(
                session.stomperUuid().toString(),
                session.targetUuid().toString(),
                session.part(),
                session.pose(),
                active);
        for (Player p : EntityUtil.getPlayersInRange(stomper, stomper.serverLevel(), 64)) {
            ServerPlayNetworking.send((ServerPlayer) p, anim);
        }
    }
}
