package org.cneko.toneko.common.mod.misc;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.codecs.Scent;

/**
 * 气味（Scent）纯函数计算器：分级、强度读写、积累与洗涤。
 * 与 {@link Charm} 同风格：不持有状态，只从组件现算。
 */
public class ScentUtil {
    public static final int MAX_INTENSITY = 100;

    public static Scent get(ItemStack stack) {
        return stack.getOrDefault(ToNekoComponents.LEGWEAR_SCENT_COMPONENT, Scent.EMPTY);
    }

    public static int getIntensity(ItemStack stack) {
        return get(stack).intensity();
    }

    public static String getWearer(ItemStack stack) {
        return get(stack).wearer();
    }

    public static boolean isScented(ItemStack stack) {
        return getIntensity(stack) > 0;
    }

    /** 气味等级 lang 后缀：clean / faint / light / noticeable / strong / overwhelming */
    public static String grade(int intensity) {
        if (intensity <= 0) return "clean";
        if (intensity < 20) return "faint";
        if (intensity < 40) return "light";
        if (intensity < 60) return "noticeable";
        if (intensity < 80) return "strong";
        return "overwhelming";
    }

    public static String grade(ItemStack stack) {
        return grade(getIntensity(stack));
    }

    /** 是否有「明显气味」（&gt;= noticeable，供猫娘嗅闻等反应判断） */
    public static boolean isScenty(ItemStack stack) {
        return getIntensity(stack) >= 40;
    }

    /** 积累 delta 点气味；delta 为正时记录当前穿着者，归零时清空穿着者。返回新 Scent。 */
    public static Scent accumulate(ItemStack stack, int delta, String wearerName) {
        Scent s = get(stack);
        int next = Mth.clamp(s.intensity() + delta, 0, MAX_INTENSITY);
        String w;
        if (next <= 0) {
            w = "";
        } else if (wearerName != null && !wearerName.isEmpty()) {
            w = wearerName;
        } else {
            w = s.wearer();
        }
        return new Scent(next, w);
    }

    /** 水缸洗涤：气味对折（向下取整），归零则清空穿着者。 */
    public static Scent wash(ItemStack stack) {
        Scent s = get(stack);
        int next = (int) Math.floor(s.intensity() * 0.5f);
        return new Scent(next, next <= 0 ? "" : s.wearer());
    }

    /** 设置强度并 clamp，归零时清空穿着者（供晾衣架/外界直接写强度用） */
    public static Scent withClampedIntensity(ItemStack stack, int intensity) {
        Scent s = get(stack);
        int c = Mth.clamp(intensity, 0, MAX_INTENSITY);
        return new Scent(c, c <= 0 ? "" : s.wearer());
    }
}
