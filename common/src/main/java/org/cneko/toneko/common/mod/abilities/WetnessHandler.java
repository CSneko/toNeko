package org.cneko.toneko.common.mod.abilities;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.misc.FireSourceUtil;
import org.cneko.toneko.common.mod.misc.LegwearUtil;
import org.cneko.toneko.common.mod.misc.Scentable;
import org.cneko.toneko.common.mod.misc.ToNekoComponents;
import org.cneko.toneko.common.mod.misc.WetnessUtil;
import org.cneko.toneko.common.util.ConfigUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 湿度变化：雨水/游泳沾湿、干燥环境随时间风干（热更快）。
 * 服务端权威，小数增量缓冲到整数档位才写组件。
 */
public class WetnessHandler {
    private static final Map<UUID, Float> BUFFER = new ConcurrentHashMap<>();

    public static void onServerTick(MinecraftServer server) {
        if (!ConfigUtil.isWetnessEnabled()) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isRemoved()) continue;
            ItemStack legwear = LegwearUtil.getWornLegwear(player);
            if (!Scentable.isScentable(legwear)) {
                BUFFER.remove(player.getUUID());
                continue;
            }

            float delta;
            if (WetnessUtil.isExposedToWater(player)) {
                delta = player.isInWater()
                        ? ConfigUtil.getWetnessWaterRate()
                        : ConfigUtil.getWetnessRainRate();
            } else {
                float temp = ConfigUtil.isWetnessTemperatureEffect() ? WetnessUtil.temperatureFactor(player) : 1.0f;
                float heat = FireSourceUtil.heat(player.level(), player.blockPosition(), ConfigUtil.getClotheslineFireRadius());
                delta = -ConfigUtil.getWetnessDryRate() * temp * heat; // 风干，近火高温快干
            }
            if (delta == 0f) continue;

            float buf = BUFFER.getOrDefault(player.getUUID(), 0f) + delta;
            int whole = (int) buf;
            if (whole == 0) {
                BUFFER.put(player.getUUID(), buf);
                continue;
            }
            BUFFER.put(player.getUUID(), buf - whole);

            int next = Math.max(0, Math.min(WetnessUtil.MAX_WETNESS, WetnessUtil.get(legwear) + whole));
            if (next != WetnessUtil.get(legwear)) {
                legwear.set(ToNekoComponents.LEGWEAR_WET_COMPONENT, next);
                LegwearUtil.markLegwearDirty(player);
            }
        }
    }
}
