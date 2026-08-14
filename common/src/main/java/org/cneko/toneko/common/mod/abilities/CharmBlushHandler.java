package org.cneko.toneko.common.mod.abilities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.misc.Charm;
import org.cneko.toneko.common.mod.misc.LegwearUtil;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.cneko.toneko.common.util.ConfigUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 脸红偷看：高魅力玩家附近（默认 16 格）的猫娘会害羞（express.shy 动画）、
 * 偷瞄玩家（LookControl）、头顶冒出爱心粒子。纯服务端视觉，零 token 开销。
 * AI 文案层由 NekoProactiveTriggers 的 legwear_blush 触发器负责（默认关闭）。
 */
public class CharmBlushHandler {
    private static int scanCounter = 0;
    /** 每只猫娘上次脸红的游戏 tick，防止高频刷屏 */
    private static final Map<UUID, Long> LAST_BLUSH = new ConcurrentHashMap<>();

    public static void onServerTick(MinecraftServer server) {
        if (!ConfigUtil.isCharmEnabled() || !ConfigUtil.isCharmBlushEnabled()) return;
        // 每秒扫描一次（性能），实际频率由 per-neko 冷却控制
        if (++scanCounter % 20 != 0) return;

        int interval = ConfigUtil.getCharmBlushIntervalTicks();
        float radius = ConfigUtil.getCharmBlushRadius();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isRemoved()) continue;

            ItemStack legwear = LegwearUtil.getWornLegwear(player);
            if (!Charm.isHighCharm(legwear)) continue;

            List<NekoEntity> nekos = EntityUtil.findNekoEntitiesInRange(player, player.level(), (int) radius);
            for (NekoEntity neko : nekos) {
                if (neko.isRemoved()) continue;
                long now = neko.level().getGameTime();
                Long last = LAST_BLUSH.get(neko.getUUID());
                if (last != null && now - last < interval) continue;

                LAST_BLUSH.put(neko.getUUID(), now);
                blush(neko, player);
                break; // 每个玩家每次最多触发一只猫娘
            }
        }
    }

    private static void blush(NekoEntity neko, ServerPlayer player) {
        neko.playExpressAnim("shy");
        neko.getLookControl().setLookAt(player.getX(), player.getEyeY(), player.getZ());
        spawnHeartParticles(neko);
    }

    private static void spawnHeartParticles(NekoEntity neko) {
        ServerLevel level = (ServerLevel) neko.level();
        double x = neko.getX();
        double y = neko.getY() + neko.getBbHeight() + 0.3;
        double z = neko.getZ();
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            if (p.distanceToSqr(neko) <= 4096) { // 64 格内可见
                p.connection.send(new ClientboundLevelParticlesPacket(
                        ParticleTypes.HEART, true, x, y, z, 0.3f, 0.3f, 0.3f, 0.02f, 3));
            }
        }
    }
}
