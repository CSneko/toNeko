package org.cneko.toneko.common.mod.misc;

import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.items.LegwearItem;

/**
 * 绝对领域计算器：领域 = 袜口高度 − 裙摆高度（同一 0~1 尺度，1 = 髋部）。
 * 未来裙装接入时只需提供真实裙摆高度（见 getSkirtHemHeight），
 * 等级判定与展示链路零改动。
 */
public class ZettaiRyouiki {
    /** 假设的标准裙摆高度（在裙装实装前使用） */
    public static final float DEFAULT_SKIRT_HEM_HEIGHT = 0.45f;
    public static final float THRESHOLD_S = 0.45f;
    public static final float THRESHOLD_A = 0.30f;
    public static final float THRESHOLD_B = 0.15f;

    /**
     * @param stockingTop 袜口高度（0~1）
     * @param skirtHem 裙摆高度（0~1）
     * @return lang 后缀：full / s / a / b / c / none
     */
    public static String compute(float stockingTop, float skirtHem) {
        // 连裤袜：全覆盖，无裸腿领域
        if (stockingTop >= 1.0f) return "full";
        // 袜口在裙摆之上才有领域
        float territory = stockingTop - skirtHem;
        if (territory <= 0.01f) return "none";
        if (territory >= THRESHOLD_S) return "s";
        if (territory >= THRESHOLD_A) return "a";
        if (territory >= THRESHOLD_B) return "b";
        return "c";
    }

    /** 用假设标准裙摆计算一件腿部服饰的领域等级 */
    public static String compute(ItemStack legwear) {
        return compute(LegwearItem.getStockingTopHeight(legwear), DEFAULT_SKIRT_HEM_HEIGHT);
    }

    /**
     * 领域连续值（0~1 比例）：袜口高出裙摆的部分。
     * 连裤袜（全覆盖）或袜口低于裙摆时返回 0。
     */
    public static float computeTerritory(float stockingTop, float skirtHem) {
        if (stockingTop >= 1.0f) return 0f;
        return Math.max(0f, stockingTop - skirtHem);
    }

    /** 领域连续值重载（用假设标准裙摆） */
    public static float computeTerritory(ItemStack legwear) {
        return computeTerritory(LegwearItem.getStockingTopHeight(legwear), DEFAULT_SKIRT_HEM_HEIGHT);
    }
}
