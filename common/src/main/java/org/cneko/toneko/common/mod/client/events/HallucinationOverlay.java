package org.cneko.toneko.common.mod.client.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.cneko.toneko.common.mod.effects.ToNekoEffects;

/** Ephemeral visual illusions: no entities, blocks, or player movement are changed. */
public final class HallucinationOverlay {
    private static final Identifier MUSHROOM = Identifier.fromNamespaceAndPath("toneko", "textures/gui/mushroom_illusion.png");
    private HallucinationOverlay() {}

    public static boolean active() {
        var player = Minecraft.getInstance().player;
        return player != null && ToNekoEffects.HALLUCINATION != null
                && player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ToNekoEffects.HALLUCINATION));
    }

    public static void render(GuiGraphicsExtractor graphics) {
        if (!active()) return;
        var client = Minecraft.getInstance();
        var effect = client.player.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ToNekoEffects.HALLUCINATION));
        double time = client.player.tickCount + client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float fade = Math.min(1, effect.getDuration() / 40f);
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        int alpha = (int) ((40 + 12 * Math.sin(time * 0.045)) * fade);
        int purple = (alpha << 24) | 0x9769BD;
        graphics.fillGradient(0, 0, width, height / 3, purple, 0x009769BD);
        graphics.fillGradient(0, height * 2 / 3, width, height, 0x009769BD, purple);
        // Staggered mushroom silhouettes drift across the peripheral vision and fade naturally.
        for (int i = 0; i < 4; i++) {
            double phase = time * 0.014 + i * 1.7;
            int size = 18 + i * 7;
            int x = (int) (width * (0.15 + i * 0.23) + Math.sin(phase) * 18);
            int y = (int) (height * (0.3 + (i % 2) * 0.4) + Math.cos(phase * 0.8) * 12);
            int tint = ((int) ((30 + 45 * (0.5 + 0.5 * Math.sin(phase))) * fade) << 24) | 0xDCC5FF;
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, MUSHROOM, x, y, 0, 0, size, size, 32, 32, 32, 32, tint);
        }
    }
}
