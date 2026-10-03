package org.cneko.toneko.common.mod.client.screens.factories;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Pose;
import org.cneko.toneko.common.mod.client.screens.NekoScreenBuilder;
import org.cneko.toneko.common.mod.client.screens.InteractionScreen;
import org.cneko.toneko.common.mod.entities.MushroomGirlEntity;
import org.cneko.toneko.common.mod.entities.MushroomRules;
import org.cneko.toneko.common.mod.packets.interactives.FollowOwnerPayload;
import org.cneko.toneko.common.mod.packets.interactives.NekoPosePayload;
import org.cneko.toneko.common.mod.packets.interactives.MushroomInteractionPayload;

public final class MushroomScreen {
    private MushroomScreen() {}
    public static NekoScreenBuilder create() {
        return new NekoScreenBuilder().setStartY(5)
                .addTooltip(TooltipFactories.NAME_TOOLTIP)
                .addTooltip(screen -> {
                    var mushroom = (MushroomGirlEntity) screen.getNeko();
                    if (mushroom.getMushroomLevel() == MushroomRules.MAX_LEVEL)
                        return Component.translatable("screen.toneko.mushroom_girl.growth_max", MushroomRules.MAX_LEVEL);
                    return Component.translatable("screen.toneko.mushroom_girl.growth", mushroom.getMushroomLevel(),
                            mushroom.getLevelExperience(), MushroomRules.xpToNextLevel(mushroom.getMushroomLevel()));
                })
                .addTooltip(screen -> {
                    var mushroom = (MushroomGirlEntity) screen.getNeko();
                    return Component.translatable("screen.toneko.mushroom_girl.growth_stats",
                            String.format(java.util.Locale.ROOT, "%.1f", mushroom.getHealth()),
                            String.format(java.util.Locale.ROOT, "%.1f", mushroom.getMaxHealth()),
                            String.format(java.util.Locale.ROOT, "%.1f", mushroom.getSporeDamage()));
                })
                .addTooltip(screen -> {
                    var mushroom = (MushroomGirlEntity) screen.getNeko();
                    var player = Minecraft.getInstance().player.getUUID();
                    if (mushroom.getFamiliarity(player) < MushroomRules.MAX_FAMILIARITY) {
                        return Component.translatable("screen.toneko.mushroom_girl.familiarity", mushroom.getFamiliarity(player));
                    }
                    int affection = mushroom.getAffection(player);
                    return Component.translatable("screen.toneko.mushroom_girl.affection",
                            Component.translatable("stage.toneko.mushroom_girl." + MushroomRules.affectionStage(affection)), affection);
                })
                .addTooltip(screen -> {
                    var mushroom = (MushroomGirlEntity) screen.getNeko();
                    return Component.translatable("screen.toneko.mushroom_girl.state",
                            Component.translatable(mushroom.isResting() ? "state.toneko.mushroom_girl.resting"
                                    : mushroom.isDrowsy() ? "state.toneko.mushroom_girl.drowsy" : "state.toneko.mushroom_girl.lively"));
                })
                .addTooltip(screen -> Component.translatable("screen.toneko.mushroom_girl.help"))
                .addTooltip(screen -> Component.translatable("screen.toneko.mushroom_girl.relationship_help"))
                .addButton(ButtonFactories.BACK_BUTTON)
                .addButton(ButtonFactories.CHAT_BUTTON)
                .addButton(ButtonFactories.GIFT_BUTTON)
                .addButton(screen -> Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.follow"), button ->
                        ClientPlayNetworking.send(new FollowOwnerPayload(screen.getNeko().getUUID().toString()))))
                .addButton(screen -> Button.builder(Component.translatable("screen.toneko.mushroom_girl.stay"), button ->
                        ClientPlayNetworking.send(new NekoPosePayload(Pose.STANDING, screen.getNeko().getUUID().toString()))))
                .addButton(screen -> Button.builder(Component.translatable("screen.toneko.mushroom_girl.companion"), button ->
                        Minecraft.getInstance().setScreen(new InteractionScreen(screen.getTitle(), screen.getNeko(), screen, companionScreen()))));
    }

    private static NekoScreenBuilder companionScreen() {
        return new NekoScreenBuilder().setStartY(5)
                .addTooltip(screen -> Component.translatable("screen.toneko.mushroom_girl.companion_help"))
                .addTooltip(screen -> Component.translatable("screen.toneko.mushroom_girl.bed_help"))
                .addTooltip(screen -> Component.translatable("screen.toneko.mushroom_girl.bed_wake_help"))
                .addButton(ButtonFactories.BACK_BUTTON)
                .addButton(companionButton("carry"))
                .addButton(companionButton("sit_on_player"))
                .addButton(companionButton("lie_on_player"))
                .addButton(companionButton("get_down"));
    }

    private static NekoScreenBuilder.ButtonFactory companionButton(String action) {
        return screen -> Button.builder(Component.translatable("screen.toneko.mushroom_girl." + action), button ->
                ClientPlayNetworking.send(new MushroomInteractionPayload(screen.getNeko().getUUID().toString(), action)));
    }
}
