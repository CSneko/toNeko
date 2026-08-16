package org.cneko.toneko.common.mod.client.events;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.misc.LegwearUtil;
import org.cneko.toneko.common.mod.misc.WetnessUtil;

/**
 * 穿着湿润丝袜时腿上滴落水滴粒子，频率按湿度分档（纯客户端，零协议）。
 */
@Environment(EnvType.CLIENT)
public class LegwearWetDripHandler {
    /** 开始滴水的最低湿度 */
    private static final int DRIP_MIN_WETNESS = 25;
    private static int tickCounter = 0;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(LegwearWetDripHandler::tick);
    }

    private static void tick(Minecraft client) {
        Player player = client.player;
        if (player == null || player.isRemoved() || client.level == null) return;

        ItemStack legwear = LegwearUtil.getWornLegwear(player);
        int wetness = WetnessUtil.get(legwear);
        if (wetness < DRIP_MIN_WETNESS) return;

        if (tickCounter++ % intervalFor(wetness) != 0) return;

        double y = player.getY() + 0.3;
        client.level.addParticle(ParticleTypes.FALLING_WATER,
                player.getX() - 0.15 + player.getRandom().nextDouble() * 0.3, y,
                player.getZ() - 0.15 + player.getRandom().nextDouble() * 0.3,
                0, 0, 0);
    }

    /** 湿度越高滴水越频繁 */
    private static int intervalFor(int wetness) {
        if (wetness >= 100) return 1;
        if (wetness >= 80) return 2;
        if (wetness >= 50) return 5;
        return 10;
    }
}
