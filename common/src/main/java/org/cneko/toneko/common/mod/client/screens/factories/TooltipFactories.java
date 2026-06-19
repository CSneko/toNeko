package org.cneko.toneko.common.mod.client.screens.factories;

import net.minecraft.network.chat.Component;
import org.cneko.toneko.common.mod.client.screens.NekoScreenBuilder.TooltipFactory;
import org.cneko.toneko.common.mod.entities.NoelleMaidNekoEntity;

public class TooltipFactories {
    public static TooltipFactory NAME_TOOLTIP = screen -> Component.translatable("screen.toneko.neko_entity_interactive.tooltip.name", screen.getNeko().getCustomName());
    public static TooltipFactory MOE_TAGS_TOOLTIP = screen -> Component.translatable("screen.toneko.neko_entity_interactive.tooltip.moe_tags", screen.getNeko().getMoeTagsString());
    public static TooltipFactory GATHERING_POWER_TOOLTIP = screen -> Component.translatable("screen.toneko.neko_entity_interactive.tooltip.gathering_power", screen.getNeko().getGatheringPower());
    public static TooltipFactory AGE_SCALE_TOOLTIP = screen -> {
        double scale = screen.getNeko().getNekoAgeScale();
        int growthPercent = (int) Math.round((scale - 0.3) / 0.7 * 100);
        return Component.translatable("screen.toneko.neko_entity_interactive.tooltip.age_scale", growthPercent);
    };
    public static TooltipFactory NOELLE_TRAUMA_TOOLTIP = screen -> {
        if (screen.getNeko() instanceof NoelleMaidNekoEntity noelle) {
            int trauma = noelle.getCurrentTrauma();
            int care = noelle.getCareScore();
            String stageName = noelle.getStage().getDisplayName();
            // 残花线：显示死亡倒计时
            if (noelle.getStage() == NoelleMaidNekoEntity.Stage.WITHERED) {
                int minutesLeft = noelle.getWitheredDeathTimer() / 1200;
                return Component.translatable("screen.toneko.noelle_interactive.tooltip.trauma_withered",
                        trauma, 100, care, 100, stageName, minutesLeft);
            }
            return Component.translatable("screen.toneko.noelle_interactive.tooltip.trauma",
                    trauma, 100, care, 100, stageName);
        }
        return Component.empty();
    };
}
