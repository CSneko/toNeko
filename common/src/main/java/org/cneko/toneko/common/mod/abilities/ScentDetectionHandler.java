package org.cneko.toneko.common.mod.abilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.cneko.toneko.common.mod.blocks.ClotheslineBlockEntity;
import org.cneko.toneko.common.mod.misc.LegwearUtil;
import org.cneko.toneko.common.mod.misc.ScentUtil;
import org.cneko.toneko.common.util.ConfigUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 气味感知：附近有「有气味的丝袜」（其他玩家穿着 或 晾衣架挂着）时，
 * 玩家在动作栏收到提示 + 来源处冒出飘散粒子。每玩家带冷却，防刷屏。
 */
public class ScentDetectionHandler {
    private static final Map<UUID, Long> LAST_NOTIFY = new ConcurrentHashMap<>();
    private static int scanCounter = 0;

    public static void onServerTick(MinecraftServer server) {
        if (!ConfigUtil.isScentDetectEnabled()) return;
        if (++scanCounter % 20 != 0) return; // 每秒扫描

        float radius = ConfigUtil.getScentDetectRadius();
        int threshold = ConfigUtil.getScentDetectThreshold();
        int cooldown = ConfigUtil.getScentDetectCooldownTicks();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isRemoved()) continue;
            long now = player.level().getGameTime();
            Long last = LAST_NOTIFY.get(player.getUUID());
            if (last != null && now - last < cooldown) continue;

            BlockPos source = findScentSource(player, radius, threshold);
            if (source != null) {
                LAST_NOTIFY.put(player.getUUID(), now);
                notify(player, source);
            }
        }
    }

    /** 找附近有气味丝袜的来源（其他玩家穿着 / 手持挂着有气味丝袜的晾衣架），返回来源坐标用于粒子。 */
    private static BlockPos findScentSource(ServerPlayer player, float radius, int threshold) {
        ServerLevel level = (ServerLevel) player.level();
        double r2 = radius * radius;

        for (ServerPlayer other : level.getServer().getPlayerList().getPlayers()) {
            if (other == player || other.isRemoved()) continue;
            if (other.distanceToSqr(player) > r2) continue;
            if (ScentUtil.getIntensity(LegwearUtil.getWornLegwear(other)) >= threshold) {
                return other.blockPosition();
            }
        }

        // 晾衣架挂着
        int r = (int) Math.ceil(radius);
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -2, -r), center.offset(r, 2, r))) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ClotheslineBlockEntity clothesline
                    && ScentUtil.getIntensity(clothesline.getItem()) >= threshold) {
                return pos;
            }
        }
        return null;
    }

    private static void notify(ServerPlayer player, BlockPos source) {
        player.displayClientMessage(Component.translatable("hint.toneko.scent.detect"), true);
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.SMOKE,
                source.getX() + 0.5, source.getY() + 0.8, source.getZ() + 0.5,
                3, 0.3, 0.2, 0.3, 0.02);
    }
}
