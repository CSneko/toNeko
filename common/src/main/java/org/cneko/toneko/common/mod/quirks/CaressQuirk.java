package org.cneko.toneko.common.mod.quirks;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.List;

public class CaressQuirk extends ToNekoQuirk{
    public CaressQuirk(String id) {
        super(id);
    }

    @Override
    public int getInteractionValue(QuirkContext context) {
        return getInteractionValue();
    }

    @Override
    public int getInteractionValue() {
        return 1;
    }

    @Override
    public Component getTooltip() {
        // 添加描述
        return Component.translatable("quirk.toneko.caress.des");
    }
}
