package org.cneko.toneko.common.mod.client.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.effects.ToNekoEffects;

/**
 * 「兴奋 II」的屏幕表现：视野四周浮起一层猫薄荷绿，猫薄荷叶从边缘缓缓飘落。
 *
 * <p>范围限制：左右 / 上下各只占 30%（中间 40% 完全干净），叶子也只在四条边带里飘，
 * 不会糊住准星区域。
 *
 * <p>触发条件：只要玩家身上有「兴奋 II」（amplifier ≥ 1）就显示，和来源无关。
 */
public final class CatnipAuraOverlay {
    /** 放在 textures/gui 下，确保会被 GUI 图集收录（item 贴图不在 GUI 图集里） */
    private static final Identifier CATNIP = Identifier.fromNamespaceAndPath("toneko", "textures/gui/catnip_leaf.png");
    /** 与 CatnipBrewing.COLOR 一致 */
    private static final int COLOR = 0x88B008;
    /** 边缘带占屏幕的比例（每边） */
    private static final float BAND_RATIO = 0.30f;
    /** 渐晕的最大不透明度 */
    private static final float MAX_ALPHA = 0.22f;
    private static final int BAND_STEPS = 10;
    private static final int LEAF_COUNT = 18;

    private CatnipAuraOverlay() {
    }

    public static boolean active(Player player) {
        if (player == null || ToNekoEffects.NEKO_EFFECT == null) {
            return false;
        }
        MobEffectInstance effect = player.getEffect(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ToNekoEffects.NEKO_EFFECT));
        return effect != null && effect.getAmplifier() >= 1;
    }

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (!active(player)) {
            return;
        }
        MobEffectInstance effect = player.getEffect(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ToNekoEffects.NEKO_EFFECT));
        if (effect == null) {
            return;
        }
        float fade = Math.min(1f, effect.getDuration() / 40f);
        if (fade <= 0f) {
            return;
        }
        double time = player.tickCount + client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        renderVignette(graphics, width, height, fade);
        renderLeaves(graphics, width, height, fade, time);
    }

    /** 四周渐晕：每条边 30%，向内逐渐透明 */
    private static void renderVignette(GuiGraphicsExtractor graphics, int width, int height, float fade) {
        int bandW = (int) (width * BAND_RATIO);
        int bandH = (int) (height * BAND_RATIO);
        for (int i = 0; i < BAND_STEPS; i++) {
            float edge = 1f - (i + 0.5f) / BAND_STEPS;
            int alpha = (int) (255 * MAX_ALPHA * fade * edge * edge);
            if (alpha <= 0) {
                continue;
            }
            int color = (alpha << 24) | COLOR;
            int x0 = i * bandW / BAND_STEPS;
            int x1 = (i + 1) * bandW / BAND_STEPS;
            int y0 = i * bandH / BAND_STEPS;
            int y1 = (i + 1) * bandH / BAND_STEPS;
            // 左右
            graphics.fill(x0, 0, x1, height, color);
            graphics.fill(width - x1, 0, width - x0, height, color);
            // 上下
            graphics.fill(0, y0, width, y1, color);
            graphics.fill(0, height - y1, width, height - y0, color);
        }
    }

    /** 边缘飘落的猫薄荷叶 */
    private static void renderLeaves(GuiGraphicsExtractor graphics, int width, int height, float fade, double time) {
        int bandW = (int) (width * BAND_RATIO);
        int bandH = (int) (height * BAND_RATIO);
        for (int i = 0; i < LEAF_COUNT; i++) {
            double phase = time * 0.02 + i * 0.83;
            float size = 8f + (i % 4) * 3f;
            int alpha = (int) ((55 + 45 * (0.5 + 0.5 * Math.sin(phase * 1.6))) * fade);
            if (alpha <= 0) {
                continue;
            }
            int tint = (alpha << 24) | 0xFFFFFF;
            int side = i % 4;
            double x;
            double y;
            if (side == 0 || side == 1) {
                // 左右两条竖带：x 在边带内摆动，y 循环下落
                double progress = (time * (0.55 + (i % 5) * 0.18) + i * 47) % height;
                double sway = Math.sin(phase) * (bandW * 0.18);
                x = side == 0
                        ? bandW * 0.05 + bandW * 0.65 * ((i * 37) % 100) / 100.0 + sway
                        : width - bandW * 0.05 - bandW * 0.65 * ((i * 53) % 100) / 100.0 + sway;
                y = progress;
            } else {
                // 上下两条横带：y 在边带内摆动，x 循环横移
                double progress = (time * (0.45 + (i % 4) * 0.16) + i * 61) % width;
                double sway = Math.sin(phase) * (bandH * 0.18);
                y = side == 2
                        ? bandH * 0.05 + bandH * 0.65 * ((i * 41) % 100) / 100.0 + sway
                        : height - bandH * 0.05 - bandH * 0.65 * ((i * 59) % 100) / 100.0 + sway;
                x = progress;
            }
            graphics.blit(RenderPipelines.GUI_TEXTURED, CATNIP, (int) x, (int) y, 0, 0,
                    (int) size, (int) size, 16, 16, 16, 16, tint);
        }
    }
}
