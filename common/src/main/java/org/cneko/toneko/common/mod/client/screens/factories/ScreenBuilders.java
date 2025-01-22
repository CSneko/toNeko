package org.cneko.toneko.common.mod.client.screens.factories;

import org.cneko.toneko.common.mod.client.screens.NekoScreenRegistry.NekoScreenBuilder;
public class ScreenBuilders {
    public static final NekoScreenBuilder COMMON_INTERACTION_SCREEN = new NekoScreenBuilder()
            .addTooltip(TooltipFactories.NAME_TOOLTIP)
            .addTooltip(TooltipFactories.MOE_TAGS_TOOLTIP)
            .addButton(ButtonFactories.CHAT_BUTTON)
            .addButton(ButtonFactories.GIFT_BUTTON)
            .addButton(ButtonFactories.ACTION_BUTTON)
            .addButton(ButtonFactories.BREED_BUTTON);
    public static final NekoScreenBuilder COMMON_ACTION_SCREEN = new NekoScreenBuilder()
            .addTooltip(TooltipFactories.NAME_TOOLTIP)
            .addTooltip(TooltipFactories.MOE_TAGS_TOOLTIP)
            .addButton(ButtonFactories.ACTION_FOLLOW_BUTTON)
            .addButton(ButtonFactories.ACTION_RIDE_BUTTON)
            .addButton(ButtonFactories.ACTION_LIE_BUTTON)
            .addButton(ButtonFactories.ACTION_GET_DOWN_BUTTON);
    public static final NekoScreenBuilder CRYSTAL_NEKO_INTERACTION_SCREEN = new NekoScreenBuilder()
            .addTooltip(TooltipFactories.NAME_TOOLTIP)
            .addTooltip(TooltipFactories.MOE_TAGS_TOOLTIP)
            .addButton(ButtonFactories.CRYSTAL_NEKO_WHO_BUTTON)
            .addButton(ButtonFactories.CRYSTAL_NEKO_ABOUT_MOD_BUTTON)
            .addButton(ButtonFactories.CRYSTAL_NEKO_PLANS_BUTTON)
            .addButton(ButtonFactories.CRYSTAL_NEKO_LINKS)
            .addButton(ButtonFactories.CRYSTAL_NEKO_INTERACTIVE)
            .addButton(ButtonFactories.CRYSTAL_NEKO_MORE_BUTTON);
    public static final NekoScreenBuilder CRYSTAL_NEKO_BASE_INTERACTION_SCREEN = new NekoScreenBuilder()
            .addTooltip(TooltipFactories.NAME_TOOLTIP)
            .addTooltip(TooltipFactories.MOE_TAGS_TOOLTIP)
            .addButton(ButtonFactories.CHAT_BUTTON)
            .addButton(ButtonFactories.GIFT_BUTTON)
            .addButton(ButtonFactories.ACTION_BUTTON)
            .addButton(ButtonFactories.CRYSTAL_NEKO_BREED_BUTTON);
    public static final NekoScreenBuilder CRYSTAL_NEKO_MORE_INTERACTION_SCREEN = new NekoScreenBuilder()
            .addTooltip(TooltipFactories.NAME_TOOLTIP)
            .addTooltip(TooltipFactories.MOE_TAGS_TOOLTIP)
            .addButton(ButtonFactories.CRYSTAL_NEKO_MORE_INTERACTION_NYA_BUTTON);
    public static final NekoScreenBuilder LINKS_SCREEN = new NekoScreenBuilder()
            .addButton(ButtonFactories.LINKS_GITHUB_BUTTON)
            .addButton(ButtonFactories.LINKS_MODRINTH_BUTTON)
            .addButton(ButtonFactories.LINKS_DISCORD_BUTTON)
            .addButton(ButtonFactories.LINKS_BILIBILI_BUTTON);
}
