package org.cneko.toneko.common.mod.misc;

import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.items.LegwearItem;
import org.cneko.toneko.common.util.ConfigUtil;

/**
 * 魅力值（Charm）计算器：领域等级为主 + D 值薄厚加成 + 左右腿独立染色点缀。
 * <p>
 * 用途：猫娘脸红偷看（CharmBlushHandler）、被动好感增长（CharmAffectionHandler）、
 * AI 穿搭评论（NekoProactiveTriggers.legwear_blush）等「穿得越撩 → 反馈越强」的闭环。
 */
public class Charm {

    // === 领域等级基分（领域等级为主） ===
    private static final int GRADE_S = 100;
    private static final int GRADE_A = 80;
    private static final int GRADE_B = 60;
    private static final int GRADE_C = 40;
    private static final int GRADE_NONE = 10;
    /** 连裤袜（全覆盖）：薄透是另一种顶格撩，故单独一档高基分 */
    private static final int GRADE_FULL = 85;

    // === D 值薄厚加成（越薄越撩） ===
    private static final int DENIER_BONUS_THIN = 20;   // ≤20D
    private static final int DENIER_BONUS_MEDIUM = 10; // ≤40D
    private static final int DYE_BONUS_PER_LEG = 5;    // 左右腿独立染色各 +5
    // === 湿度湿身加成（湿透是另一种顶格撩） ===
    private static final int WET_BONUS_SOAKED = 15;    // 湿透（>=80）
    private static final int WET_BONUS_WET = 8;        // 湿（>=50）

    /** 单件腿部服饰的魅力分 */
    public static int compute(ItemStack legwear) {
        if (!LegwearItem.isLegwear(legwear)) return 0;

        int denier = LegwearItem.getDenier(legwear);
        float length = LegwearItem.getStockingTopHeight(legwear);

        int base;
        if (length >= 1.0f) {
            base = GRADE_FULL; // 连裤袜
        } else {
            base = switch (ZettaiRyouiki.compute(length, ZettaiRyouiki.DEFAULT_SKIRT_HEM_HEIGHT)) {
                case "s" -> GRADE_S;
                case "a" -> GRADE_A;
                case "b" -> GRADE_B;
                case "c" -> GRADE_C;
                default -> GRADE_NONE;
            };
        }

        int denierBonus;
        if (denier <= 20) denierBonus = DENIER_BONUS_THIN;
        else if (denier <= 40) denierBonus = DENIER_BONUS_MEDIUM;
        else denierBonus = 0;

        int dyeBonus = 0;
        if (LegwearItem.getLeftDye(legwear) >= 0) dyeBonus += DYE_BONUS_PER_LEG;
        if (LegwearItem.getRightDye(legwear) >= 0) dyeBonus += DYE_BONUS_PER_LEG;

        int wetBonus = wetBonus(WetnessUtil.get(legwear));

        return base + denierBonus + dyeBonus + wetBonus;
    }

    /** 湿度湿身加成 */
    private static int wetBonus(int wetness) {
        if (wetness >= 80) return WET_BONUS_SOAKED;
        if (wetness >= 50) return WET_BONUS_WET;
        return 0;
    }

    /** 是否达到「高魅力」门槛（供脸红/好感/主动反应判断） */
    public static boolean isHighCharm(ItemStack legwear) {
        return compute(legwear) >= ConfigUtil.getCharmHighThreshold();
    }

    /** 魅力等级文本 key 后缀（s / high / low），用于展示或 AI 语气区分 */
    public static String grade(int charm) {
        if (charm >= 90) return "s";
        if (charm >= ConfigUtil.getCharmHighThreshold()) return "high";
        return "low";
    }
}
