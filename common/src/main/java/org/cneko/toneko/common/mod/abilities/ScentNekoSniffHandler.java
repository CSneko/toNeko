package org.cneko.toneko.common.mod.abilities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.misc.Charm;
import org.cneko.toneko.common.mod.misc.LegwearUtil;
import org.cneko.toneko.common.mod.misc.ScentUtil;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.cneko.toneko.common.util.ConfigUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 气味嗅闻：高气味玩家附近（默认 12 格）的猫娘会偷偷嗅闻（害羞动画 + 爱心粒子 + 偷瞄腿部）。
 * 纯服务端视觉，零 token。AI 文案层由 NekoProactiveTriggers 的 scent_sniff 触发器负责（默认关闭）。
 */
public class ScentNekoSniffHandler {
    private static int scanCounter = 0;
    /** 每只猫娘上次嗅闻的游戏 tick，防止高频刷屏 */
    private static final Map<UUID, Long> LAST_SNIFF = new ConcurrentHashMap<>();

    public static void onServerTick(MinecraftServer server) {
        if (!ConfigUtil.isScentEnabled() || !ConfigUtil.isScentNekoSniffEnabled()) return;
        // 每秒扫描一次（性能），实际频率由 per-neko 冷却控制
        if (++scanCounter % 20 != 0) return;

        int interval = ConfigUtil.getScentNekoSniffIntervalTicks();
        float radius = ConfigUtil.getScentNekoSniffRadius();
        int threshold = ConfigUtil.getScentNekoSniffThreshold();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isRemoved()) continue;

            ItemStack legwear = LegwearUtil.getWornLegwear(player);
            if (ScentUtil.getIntensity(legwear) < threshold) continue;

            List<NekoEntity> nekos = EntityUtil.findNekoEntitiesInRange(player, player.level(), (int) radius);
            for (NekoEntity neko : nekos) {
                if (neko.isRemoved()) continue;
                long now = neko.level().getGameTime();
                Long last = LAST_SNIFF.get(neko.getUUID());
                if (last != null && now - last < interval) continue;

                LAST_SNIFF.put(neko.getUUID(), now);
                sniff(neko, player, legwear);
                break; // 每个玩家每次最多触发一只猫娘
            }
        }
    }

    private static void sniff(NekoEntity neko, ServerPlayer player, ItemStack legwear) {
        // 气味 × 魅力复合：高魅力时猫娘反应更强（深呼吸/重度脸红），粒子更多
        boolean compound = Charm.isHighCharm(legwear);
        neko.playExpressAnim(compound ? "nuzzle" : "shy");
        neko.getLookControl().setLookAt(player.getX(), player.getY() + 0.5, player.getZ());
        spawnHeartParticles(neko, compound ? 6 : 3);
        neko.level().playSound(null, neko.getX(), neko.getY(), neko.getZ(),
                SoundEvents.CAT_AMBIENT, neko.getSoundSource(), 1.0f, 1.0f);
    }

    private static void spawnHeartParticles(NekoEntity neko, int count) {
        ServerLevel level = (ServerLevel) neko.level();
        double x = neko.getX();
        double y = neko.getY() + neko.getBbHeight() + 0.3;
        double z = neko.getZ();
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            if (p.distanceToSqr(neko) <= 4096) { // 64 格内可见
                p.connection.send(new ClientboundLevelParticlesPacket(
                        ParticleTypes.HEART, true, x, y, z, 0.3f, 0.3f, 0.3f, 0.02f, count));
            }
        }
    }
}
