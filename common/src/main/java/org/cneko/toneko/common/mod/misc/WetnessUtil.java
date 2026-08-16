package org.cneko.toneko.common.mod.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 湿度（Wetness）纯函数与气候辅助：分级、读写，以及温度/沾水判定（气味与湿度 handler 共用）。
 */
public class WetnessUtil {
    public static final int MAX_WETNESS = 100;

    public static int get(ItemStack stack) {
        return stack.getOrDefault(ToNekoComponents.LEGWEAR_WET_COMPONENT, 0);
    }

    public static boolean isWet(ItemStack stack) {
        return get(stack) > 0;
    }

    /** 湿度等级 lang 后缀：dry / damp / wet / soaked / drenched */
    public static String grade(int wetness) {
        if (wetness <= 0) return "dry";
        if (wetness < 25) return "damp";
        if (wetness < 50) return "wet";
        if (wetness < 80) return "soaked";
        return "drenched";
    }

    public static String grade(ItemStack stack) {
        return grade(get(stack));
    }

    /** 温度系数：群系基础温度越高越大（0.5~2.0），用于气味积累与干燥速度 */
    public static float temperatureFactor(Level level, BlockPos pos) {
        float temp = level.getBiome(pos).value().getBaseTemperature();
        return Mth.clamp(0.5f + temp, 0.5f, 2.0f);
    }

    public static float temperatureFactor(ServerPlayer player) {
        return temperatureFactor(player.level(), player.blockPosition());
    }

    /** 是否正暴露在水中/雨水中（游泳，或露天淋雨） */
    public static boolean isExposedToWater(ServerPlayer player) {
        if (player.isInWater()) return true;
        return player.level().isRainingAt(player.blockPosition())
                && player.level().canSeeSky(player.blockPosition());
    }
}
