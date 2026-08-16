package org.cneko.toneko.common.mod.abilities;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.codecs.Scent;
import org.cneko.toneko.common.mod.advencements.ToNekoCriteria;
import org.cneko.toneko.common.mod.misc.FireSourceUtil;
import org.cneko.toneko.common.mod.misc.LegwearUtil;
import org.cneko.toneko.common.mod.misc.Scentable;
import org.cneko.toneko.common.mod.misc.ScentUtil;
import org.cneko.toneko.common.mod.misc.ToNekoComponents;
import org.cneko.toneko.common.mod.misc.WetnessUtil;
import org.cneko.toneko.common.util.ConfigUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 气味积累：穿着丝袜时按 基础速率 × 运动 × D值(厚袜更快) × 温度 × 湿度(湿袜发酵更快) 累积；
 * 雨水/游泳时叠加洗涤衰减。服务端权威，小数增量缓冲到整数档位才写组件（避免每 tick markDirty）。
 */
public class ScentAccumulationHandler {
    private static final Map<UUID, Float> BUFFER = new ConcurrentHashMap<>();

    public static void onServerTick(MinecraftServer server) {
        if (!ConfigUtil.isScentEnabled()) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isRemoved()) continue;
            ItemStack legwear = LegwearUtil.getWornLegwear(player);
            if (!Scentable.isScentable(legwear)) {
                BUFFER.remove(player.getUUID());
                continue;
            }

            float delta = computeDelta(player, legwear);
            if (delta == 0f) continue;

            float buf = BUFFER.getOrDefault(player.getUUID(), 0f) + delta;
            int whole = (int) buf;
            if (whole == 0) {
                BUFFER.put(player.getUUID(), buf);
                continue;
            }
            BUFFER.put(player.getUUID(), buf - whole);

            int before = ScentUtil.getIntensity(legwear);
            Scent next = ScentUtil.accumulate(legwear, whole, player.getName().getString());
            legwear.set(ToNekoComponents.LEGWEAR_SCENT_COMPONENT, next);
            LegwearUtil.markLegwearDirty(player);
            if (before <= 0 && next.intensity() > 0) {
                ToNekoCriteria.LEGWEAR_FIRST_SCENT.trigger(player);
            }
        }
    }

    private static float computeDelta(ServerPlayer player, ItemStack legwear) {
        float base = ConfigUtil.getScentBaseRate();
        if (base <= 0f) return 0f;

        // 运动系数：疾跑 > 步行 > 静止
        boolean sprinting = player.isSprinting();
        boolean moving = player.onGround()
                && player.getDeltaMovement().horizontalDistanceSqr() > 0.0025
                && !player.isSwimming() && !player.isFallFlying();
        float move;
        if (sprinting && moving) move = ConfigUtil.getScentSprintFactor();
        else if (moving) move = 1.0f;
        else move = ConfigUtil.getScentIdleFactor();

        // D 值系数（Scentable：厚袜更快、薄袜更慢）
        float denier = legwear.getItem() instanceof Scentable s
                ? s.scentAccumulationFactor(legwear) : 1.0f;

        // 温度系数（热更快；近火高温进一步挥发加剧）
        float temp = ConfigUtil.isScentTemperatureEffect() ? WetnessUtil.temperatureFactor(player) : 1.0f;
        temp *= FireSourceUtil.heat(player.level(), player.blockPosition(), ConfigUtil.getClotheslineFireRadius());

        // 湿度系数（湿袜闷汗发酵更快）
        float wet = WetnessUtil.get(legwear) > 0 ? ConfigUtil.getScentWetFactor() : 1.0f;

        float delta = base * move * denier * temp * wet;

        // 雨水/游泳：叠加洗涤衰减（可能把净增量冲到负值，即被水冲淡）
        if (WetnessUtil.isExposedToWater(player)) {
            delta -= ConfigUtil.getScentWashRate();
        }
        return delta;
    }
}
