package org.cneko.toneko.common.mod.abilities;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.misc.LegwearUtil;
import org.cneko.toneko.common.mod.misc.ScentUtil;
import org.cneko.toneko.common.util.ConfigUtil;

/**
 * 狼犬追踪风险侧：高气味玩家会被附近野生狼从更远处追踪（被闻到是有代价的）。
 * 只影响未驯服且当前无目标的狼；每 2 秒扫描，每玩家每次最多一只狼。
 */
public class ScentWolfTrackingHandler {
    private static int scanCounter = 0;

    public static void onServerTick(MinecraftServer server) {
        if (!ConfigUtil.isScentWolfTrackingEnabled()) return;
        if (++scanCounter % 40 != 0) return; // 每 2 秒扫描

        float radius = ConfigUtil.getScentWolfTrackingRadius();
        int threshold = ConfigUtil.getScentWolfTrackingThreshold();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isRemoved()) continue;
            ItemStack legwear = LegwearUtil.getWornLegwear(player);
            if (ScentUtil.getIntensity(legwear) < threshold) continue;

            ServerLevel level = (ServerLevel) player.level();
            for (Wolf wolf : level.getEntitiesOfClass(Wolf.class,
                    player.getBoundingBox().inflate(radius),
                    w -> !w.isTame() && w.getTarget() == null && w.isAlive())) {
                wolf.setTarget(player);
                break; // 每个玩家每次最多触发一只狼
            }
        }
    }
}
