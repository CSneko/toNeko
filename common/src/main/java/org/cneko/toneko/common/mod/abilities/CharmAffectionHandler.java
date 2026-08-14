package org.cneko.toneko.common.mod.abilities;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.misc.Charm;
import org.cneko.toneko.common.mod.misc.LegwearUtil;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.cneko.toneko.common.util.ConfigUtil;

import java.util.List;

/**
 * 魅力值 → 被动好感增长：穿着高魅力腿饰的玩家，其主人猫娘在附近时会缓慢增长好感。
 * 默认关闭（避免老存档好感通胀）；节奏受配置控制，纯增益、无负面。
 */
public class CharmAffectionHandler {
    private static int tickCounter = 0;

    public static void onServerTick(MinecraftServer server) {
        if (!ConfigUtil.isCharmEnabled() || !ConfigUtil.isCharmAffectionEnabled()) return;
        int interval = ConfigUtil.getCharmAffectionIntervalTicks();
        if (interval <= 0 || ++tickCounter % interval != 0) return;

        int amount = ConfigUtil.getCharmAffectionAmount();
        int max = ConfigUtil.getCharmAffectionMax();
        float radius = ConfigUtil.getCharmAffectionRadius();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isRemoved()) continue;

            ItemStack legwear = LegwearUtil.getWornLegwear(player);
            if (!Charm.isHighCharm(legwear)) continue;

            List<NekoEntity> nekos = EntityUtil.findNekoEntitiesInRange(player, player.level(), (int) radius);
            for (NekoEntity neko : nekos) {
                if (!neko.hasOwner(player.getUUID())) continue;
                int xp = neko.getXpWithOwner(player.getUUID());
                if (max > 0 && xp >= max) continue;

                int next = xp + amount;
                if (max > 0) {
                    next = Math.min(max, Math.max(0, next));
                } else {
                    next = Math.max(0, next);
                }
                neko.setXpWithOwner(player.getUUID(), next);
            }
        }
    }
}
