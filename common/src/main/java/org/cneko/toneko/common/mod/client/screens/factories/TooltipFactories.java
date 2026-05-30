package org.cneko.toneko.common.mod.client.screens.factories;

import net.minecraft.network.chat.Component;
import org.cneko.toneko.common.mod.client.screens.NekoScreenBuilder.TooltipFactory;
public class TooltipFactories {
    public static TooltipFactory NAME_TOOLTIP = screen -> Component.translatable("screen.toneko.neko_entity_interactive.tooltip.name", screen.getNeko().getCustomName());
    public static TooltipFactory MOE_TAGS_TOOLTIP = screen -> Component.translatable("screen.toneko.neko_entity_interactive.tooltip.moe_tags", screen.getNeko().getMoeTagsString());
    public static TooltipFactory GATHERING_POWER_TOOLTIP = screen -> Component.translatable("screen.toneko.neko_entity_interactive.tooltip.gathering_power", screen.getNeko().getGatheringPower());
    public static TooltipFactory AGE_SCALE_TOOLTIP = screen -> {
        double scale = screen.getNeko().getNekoAgeScale();
        int growthPercent = (int) Math.round((scale - 0.3) / 0.7 * 100);
        return Component.translatable("screen.toneko.neko_entity_interactive.tooltip.age_scale", growthPercent);
    };
}
