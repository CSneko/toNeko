package org.cneko.toneko.common.mod.client.screens.factories;

import net.minecraft.network.chat.Component;
import org.cneko.toneko.common.mod.client.screens.NekoScreenRegistry.NekoScreenBuilder.TooltipFactory;
public class TooltipFactories {
    public static TooltipFactory NAME_TOOLTIP = screen -> Component.translatable("screen.toneko.neko_entity_interactive.tooltip.name", screen.getNeko().getCustomName());
    public static TooltipFactory MOE_TAGS_TOOLTIP = screen -> Component.translatable("screen.toneko.neko_entity_interactive.tooltip.moe_tags", screen.getNeko().getMoeTagsString());
}
