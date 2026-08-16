package org.cneko.toneko.common.mod.abilities;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.items.LegwearItem;
import org.cneko.toneko.common.mod.misc.LegwearUtil;
import org.cneko.toneko.common.mod.misc.ToNekoComponents;
import org.cneko.toneko.common.mod.misc.WetnessUtil;
import org.cneko.toneko.common.util.ConfigUtil;

/**
 * 袜子滑落：过膝袜（袜口 ≤ 自然高度）移动时袜口缓慢下滑、静止时缓慢回弹。
 * 服务端权威计算并写 {@code legwear_length} 组件；滑落会让绝对领域等级下降，
 * 玩家可用「提袜」键（{@code LegwearPullUpPayload}）复位。只对过膝袜生效（连裤袜全覆盖不滑）。
 */
public class LegwearSagHandler {

    public static void onServerTick(MinecraftServer server) {
        if (!ConfigUtil.isLegwearSagEnabled()) return;

        float decay = ConfigUtil.getLegwearSagDecayPerTick();
        float recover = ConfigUtil.getLegwearSagRecoverPerTick();
        float min = ConfigUtil.getLegwearSagMinLength();
        if (decay <= 0 && recover <= 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isRemoved()) continue;

            ItemStack legwear = LegwearUtil.getWornLegwear(player);
            if (!(legwear.getItem() instanceof LegwearItem.OverKneeSockItem)) continue;

            float length = LegwearItem.getStockingTopHeight(legwear);
            // 只处理自然高度及以下：玩家用工作台把袜口拉高到接近连裤袜时不再下滑
            if (length > LegwearItem.OverKneeSockItem.NATURAL_TOP) continue;

            // 湿袜贴腿：湿度越高滑落/回弹越慢（湿透时几乎定住）
            int wetness = WetnessUtil.get(legwear);
            float wetSlowdown = ConfigUtil.getLegwearSagWetSlowdown();
            float wetFactor = wetSlowdown > 0 && wetness > 0
                    ? 1.0f - wetSlowdown * (wetness / (float) WetnessUtil.MAX_WETNESS)
                    : 1.0f;

            boolean moving = player.onGround()
                    && player.getDeltaMovement().horizontalDistanceSqr() > 0.0025
                    && !player.isSwimming() && !player.isFallFlying();

            float next = moving
                    ? Math.max(min, length - decay * wetFactor)
                    : Math.min(LegwearItem.OverKneeSockItem.NATURAL_TOP, length + recover * wetFactor);

            if (Math.abs(next - length) > 1e-4f) {
                legwear.set(ToNekoComponents.LEGWEAR_LENGTH_COMPONENT, next);
                LegwearUtil.markLegwearDirty(player);
            }
        }
    }
}
