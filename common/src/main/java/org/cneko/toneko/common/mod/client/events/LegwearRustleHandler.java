package org.cneko.toneko.common.mod.client.events;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.cneko.toneko.common.mod.items.LegwearItem;
import org.cneko.toneko.common.mod.misc.LegwearUtil;
import org.cneko.toneko.common.mod.misc.ToNekoSoundEvents;

/**
 * 穿着丝袜移动时播放尼龙摩擦声：疾跑更急促响亮，潜行轻缓，静止/游泳/飞行时无声。
 * 纯客户端，无协议开销。
 */
@Environment(EnvType.CLIENT)
public class LegwearRustleHandler {
    private static int cooldown = 0;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(LegwearRustleHandler::tick);
    }

    private static void tick(Minecraft client) {
        var player = client.player;
        if (player == null) return;
        // 腿部穿着丝袜（Trinkets 饰品槽 / 盔甲槽均识别）
        if (!LegwearItem.isLegwear(LegwearUtil.getWornLegwear(player))) return;
        // 在地面、非游泳、非滑翔
        if (!player.onGround() || player.isSwimming() || player.isInWater() || player.isFallFlying()) return;
        // 静止不动
        if (player.getDeltaMovement().horizontalDistanceSqr() <= 0.0025) return;
        if (--cooldown > 0) return;

        boolean sprint = player.isSprinting();
        boolean sneak = player.isShiftKeyDown();
        cooldown = (sprint ? 5 : 9) + player.getRandom().nextInt(3);
        float volume = sprint ? 0.40f : (sneak ? 0.12f : 0.25f);
        float pitch = (sprint ? 1.15f : (sneak ? 0.85f : 1.0f))
                + player.getRandom().nextFloat() * 0.1f - 0.05f;
        player.playSound(ToNekoSoundEvents.LEGWEAR_RUSTLE, volume, pitch);
    }
}
