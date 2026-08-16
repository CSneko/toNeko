package org.cneko.toneko.common.mod.misc;

import net.minecraft.world.item.ItemStack;

/**
 * 气味载体接口：实现并注入 {@link ToNekoComponents#LEGWEAR_SCENT_COMPONENT} 的物品参与气味/湿度系统。
 * 当前仅 {@link org.cneko.toneko.common.mod.items.LegwearItem} 实现，未来"穿过的衣物"（衬衫等）接入即可复用。
 */
public interface Scentable {
    /** 气味积累系数：&gt;1 更快、&lt;1 更慢（丝袜按 D 值映射：厚袜更快） */
    default float scentAccumulationFactor(ItemStack stack) {
        return 1.0f;
    }

    /** 该物品是否挂了气味组件（即是否参与气味/湿度系统） */
    static boolean isScentable(ItemStack stack) {
        return stack.has(ToNekoComponents.LEGWEAR_SCENT_COMPONENT);
    }
}
