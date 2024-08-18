package org.cneko.toneko.common.mod.quirks;

import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

public interface ModQuirk {
    /**
     * 添加悬浮文本，可返回null
     */
    @Nullable
    Component getTooltip();

    int getInteractionValue(QuirkContext context);

}
