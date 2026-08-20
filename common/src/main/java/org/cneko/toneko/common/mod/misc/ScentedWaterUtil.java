package org.cneko.toneko.common.mod.misc;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * 变质水/香水通用工具：读写水质等级与气味来源，生成带组件的 ItemStack。
 */
public class ScentedWaterUtil {
    public static final int MAX_SPOILAGE = 100;

    public static int getSpoilage(ItemStack stack) {
        return stack.getOrDefault(ToNekoComponents.SPOILED_WATER_SPOILAGE_COMPONENT, 0);
    }

    public static String getWearer(ItemStack stack) {
        return stack.getOrDefault(ToNekoComponents.SPOILED_WATER_WEARER_COMPONENT, "");
    }

    public static void setSpoilage(ItemStack stack, int spoilage, String wearer) {
        stack.set(ToNekoComponents.SPOILED_WATER_SPOILAGE_COMPONENT,
                Mth.clamp(spoilage, 0, MAX_SPOILAGE));
        stack.set(ToNekoComponents.SPOILED_WATER_WEARER_COMPONENT, wearer == null ? "" : wearer);
    }

    public static ItemStack create(ItemLike item, int spoilage, String wearer) {
        ItemStack stack = new ItemStack(item);
        setSpoilage(stack, spoilage, wearer);
        return stack;
    }

    /** 变质等级 lang 后缀：fresh / slight / moderate / heavy / foul / overwhelming */
    public static String grade(int spoilage) {
        if (spoilage <= 0) return "fresh";
        if (spoilage < 20) return "slight";
        if (spoilage < 40) return "moderate";
        if (spoilage < 60) return "heavy";
        if (spoilage < 80) return "foul";
        return "overwhelming";
    }
}
