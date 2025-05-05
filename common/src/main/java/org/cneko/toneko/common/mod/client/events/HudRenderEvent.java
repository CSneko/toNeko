package org.cneko.toneko.common.mod.client.events;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class HudRenderEvent {
    public static void init(){
        HudRenderCallback.EVENT.register((guiGraphics, deltaTracker) -> {
            renderNekoEnergyBar(guiGraphics);
        });
    }

    private static final ResourceLocation CATNIP_ICON = ResourceLocation.fromNamespaceAndPath(MODID,"textures/item/catnip.png");
    public static void renderNekoEnergyBar(GuiGraphics context) {
        Minecraft client = Minecraft.getInstance();
        if (client.options.hideGui) return;

        if (client.player == null) return;
        Player player = client.player;

        float nekoEnergy = player.getNekoEnergy();
        float maxNekoEnergy = player.getMaxNekoEnergy();

        // 如果能量是满的，则隐藏
        if (nekoEnergy >= maxNekoEnergy) return;

        // 获取屏幕尺寸
        int width = context.guiWidth();
        int height = context.guiHeight();

        // 设置位置和尺寸
        int barWidth = 91;
        int barHeight = 4;
        int iconSize = 8;
        int margin = 10;
        int x = width - margin - barWidth;
        int y = height - margin - barHeight;

        // 计算能量百分比
        float percent = nekoEnergy / maxNekoEnergy;
        int energyWidth = (int)(barWidth * percent);

        // 绘制背景条
        context.fill(
                x, y,
                x + barWidth, y + barHeight,
                0x80000000 // 半透明黑色背景
        );

        // 绘制渐变能量条（绿色调）
        for (int i = 0; i < energyWidth; i++) {
            float progress = (float)i / barWidth;
            // 渐变从暗绿色(100,200,0)到亮绿色(150,255,100)
            int red = (int)(100 * (1 - progress) + 150 * progress);
            int green = (int)(200 * (1 - progress) + 255 * progress);
            int blue = (int)(0 * (1 - progress) + 100 * progress);
            int color = (0xFF << 24) | (red << 16) | (green << 8) | blue;

            context.fill(
                    x + i, y,
                    x + i + 1, y + barHeight,
                    color
            );
        }

        // 绘制猫薄荷图标（在能量条左侧）
        context.blit(CATNIP_ICON,
                x - iconSize - 2,  // 放在能量条左边，间隔2像素
                y - (iconSize - barHeight) / 2,
                0, 0,
                iconSize, iconSize,
                iconSize, iconSize);
    }
}