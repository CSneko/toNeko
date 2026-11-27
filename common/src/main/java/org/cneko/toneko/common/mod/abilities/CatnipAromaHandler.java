package org.cneko.toneko.common.mod.abilities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.cneko.toneko.common.mod.effects.ToNekoEffects;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.entities.ai.BehaviorPriority;
import org.cneko.toneko.common.mod.entities.ai.NekoBrain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 「猫薄荷香气」的实体侧逻辑：让 30 格内的 NPC 猫娘主动朝带香气的玩家靠近。
 *
 * <p>为什么不用 {@code MobEffect#applyEffectTick}：香气可能同时挂在多个玩家身上，
 * 而每只猫娘需要一个统一的「跟谁走」决策（随机挑一个带香气的玩家），放在效果 tick 里会重复处理。
 * 所以统一在服务端每 5 tick 跑一次：
 * <ol>
 *   <li>收集所有带香气的玩家；</li>
 *   <li>找出她们 30 格内的 NPC 猫娘（去重）；</li>
 *   <li>距离 &gt; 4 格 → 朝目标玩家移动；≤ 4 格 → 停下，冒爱心并看向玩家；</li>
 *   <li>不再被任何香气覆盖的猫娘释放导航。</li>
 * </ol>
 *
 * <p>战斗 / 逃跑中的猫娘不打断：这两类行为在 {@link NekoBrain} 里的优先级分别是
 * {@link BehaviorPriority#COMBAT} 和 {@link BehaviorPriority#CRITICAL}，这里直接跳过。
 */
public final class CatnipAromaHandler {
    /** 提交移动意图时使用的来源标记（用于停止自己提交的导航） */
    private static final Object SOURCE = new Object();

    private static final double RADIUS = 30.0;
    private static final double RADIUS_SQ = RADIUS * RADIUS;
    /** 靠近到 4 格就停下 */
    private static final double STOP_DISTANCE_SQ = 4.0 * 4.0;
    /** 每 5 tick 决策一次（爱心也是这个频率） */
    private static final int INTERVAL_TICKS = 5;
    /** 靠近速度 */
    private static final double MOVE_SPEED = 1.2;

    /** 猫娘 UUID → 当前被吸引的目标玩家 UUID（避免每 tick 随机换人） */
    private static final Map<UUID, UUID> TARGETS = new HashMap<>();
    private static int tickCounter;

    private CatnipAromaHandler() {
    }

    public static void onServerTick(MinecraftServer server) {
        if (++tickCounter % INTERVAL_TICKS != 0) {
            return;
        }
        List<ServerPlayer> aromaPlayers = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (hasAroma(player)) {
                aromaPlayers.add(player);
            }
        }
        if (aromaPlayers.isEmpty()) {
            releaseAll(server);
            return;
        }

        Set<UUID> handled = new HashSet<>();
        for (ServerPlayer player : aromaPlayers) {
            for (NekoEntity neko : player.level().getEntitiesOfClass(
                    NekoEntity.class, player.getBoundingBox().inflate(RADIUS))) {
                if (!handled.add(neko.getUUID())) {
                    continue;
                }
                attract(neko, aromaPlayers);
            }
        }
        releaseStale(server, handled);
    }

    private static void attract(NekoEntity neko, List<ServerPlayer> aromaPlayers) {
        if (!neko.isAlive() || neko.isPassenger() || neko.isNoAi()) {
            return;
        }
        NekoBrain brain = neko.getNekoBrain();
        BehaviorPriority current = brain.getCurrentPriority();
        // 战斗 / 逃跑不打断
        if (current == BehaviorPriority.CRITICAL || current == BehaviorPriority.COMBAT) {
            return;
        }
        if (neko.getTarget() != null && neko.getTarget().isAlive()) {
            return;
        }

        List<ServerPlayer> candidates = new ArrayList<>();
        for (ServerPlayer player : aromaPlayers) {
            if (player.level() == neko.level() && player.distanceToSqr(neko) <= RADIUS_SQ) {
                candidates.add(player);
            }
        }
        if (candidates.isEmpty()) {
            TARGETS.remove(neko.getUUID());
            brain.stopMoving(SOURCE);
            return;
        }

        ServerPlayer target = pickTarget(neko, candidates);
        if (neko.distanceToSqr(target) > STOP_DISTANCE_SQ) {
            brain.submitMove(target, MOVE_SPEED, BehaviorPriority.NORMAL, SOURCE);
            neko.getLookControl().setLookAt(target, 30.0f, 30.0f);
        } else {
            brain.stopMoving(SOURCE);
            neko.getLookControl().setLookAt(target, 30.0f, 30.0f);
            spawnHearts(neko, target);
        }
    }

    /** 固定的目标优先；目标失效或不在候选里时随机挑一个 */
    private static ServerPlayer pickTarget(NekoEntity neko, List<ServerPlayer> candidates) {
        UUID chosen = TARGETS.get(neko.getUUID());
        if (chosen != null) {
            for (ServerPlayer player : candidates) {
                if (player.getUUID().equals(chosen)) {
                    return player;
                }
            }
        }
        ServerPlayer target = candidates.get(neko.getRandom().nextInt(candidates.size()));
        TARGETS.put(neko.getUUID(), target.getUUID());
        return target;
    }

    private static void spawnHearts(NekoEntity neko, ServerPlayer target) {
        if (!(neko.level() instanceof ServerLevel level)) {
            return;
        }
        double y = neko.getY() + neko.getBbHeight() + 0.35;
        level.sendParticles(ParticleTypes.HEART, neko.getX(), y, neko.getZ(),
                1, 0.25, 0.15, 0.25, 0.0);
        Vec3 toward = target.position().subtract(neko.position()).normalize().scale(0.6);
        level.sendParticles(ParticleTypes.HEART,
                neko.getX() + toward.x, neko.getY() + neko.getBbHeight() * 0.8, neko.getZ() + toward.z,
                1, 0.1, 0.1, 0.1, 0.0);
    }

    /** 释放已经不在任何香气范围内的猫娘 */
    private static void releaseStale(MinecraftServer server, Set<UUID> handled) {
        Iterator<Map.Entry<UUID, UUID>> iterator = TARGETS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, UUID> entry = iterator.next();
            if (handled.contains(entry.getKey())) {
                continue;
            }
            NekoEntity neko = findNeko(server, entry.getKey());
            if (neko != null) {
                neko.getNekoBrain().stopMoving(SOURCE);
            }
            iterator.remove();
        }
    }

    private static void releaseAll(MinecraftServer server) {
        if (TARGETS.isEmpty()) {
            return;
        }
        for (UUID id : new ArrayList<>(TARGETS.keySet())) {
            NekoEntity neko = findNeko(server, id);
            if (neko != null) {
                neko.getNekoBrain().stopMoving(SOURCE);
            }
        }
        TARGETS.clear();
    }

    private static NekoEntity findNeko(MinecraftServer server, UUID uuid) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof NekoEntity neko) {
                return neko;
            }
        }
        return null;
    }

    private static boolean hasAroma(ServerPlayer player) {
        return ToNekoEffects.CATNIP_AROMA != null
                && player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ToNekoEffects.CATNIP_AROMA));
    }
}
