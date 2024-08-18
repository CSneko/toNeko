package org.cneko.toneko.common.mod.quirks;

import net.minecraft.network.chat.Component;

import java.util.List;

public interface ModQuirk {
    /**
     * 添加悬浮文本，可参考物品的appendTooltip方法
     * @param context 上下文
     * @param tooltip tooltip列表
     */
    default void appendTooltip(QuirkContext context,List<Component> tooltip){
    }

    int getInteractionValue(QuirkContext context);

}
