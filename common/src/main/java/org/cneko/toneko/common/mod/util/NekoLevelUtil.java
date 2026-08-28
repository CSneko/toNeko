package org.cneko.toneko.common.mod.util;

import net.minecraft.world.level.Level;

/**
 * 26.x：Level.getOverworldClockTime()/isDay() 被移除，改用 Overworld Clock。
 * 这里提供统一的时间/昼夜判断入口。
 */
public final class NekoLevelUtil {

    private NekoLevelUtil() {}

    /** 当前一天的 tick（0~23999）。 */
    public static long getDayTime(Level level) {
        return Math.floorMod(level.getOverworldClockTime(), 24000L);
    }

    public static boolean isDay(Level level) {
        long t = getDayTime(level);
        return t >= 0L && t < 12000L;
    }

    public static boolean isNight(Level level) {
        return !isDay(level);
    }
}
