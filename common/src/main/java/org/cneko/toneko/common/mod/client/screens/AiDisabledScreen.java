package org.cneko.toneko.common.mod.client.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * AI 未启用提示屏（单机/局域网主机）：点击"和猫娘聊天"按钮时 AI 未启用，
 * 提示玩家需要启用 AI，并提供"打开 AI 设置"按钮直达 AI 配置页。
 */
public class AiDisabledScreen extends Screen {
    private final Screen lastScreen;
    /** 提示文字的顶部 Y（居中偏上） */
    private int messageTop;

    public AiDisabledScreen(Screen lastScreen) {
        super(Component.translatable("screen.toneko.ai_disabled.title"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        super.init();
        int buttonWidth = 120;
        int buttonHeight = 20;
        int centerX = this.width / 2;
        messageTop = this.height / 3;

        // 打开 AI 设置：直达 AI 配置页（第一行就是 ai.enable 开关，apply 后生效）
        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.toneko.ai_disabled.open_config"),
                        btn -> Minecraft.getInstance().setScreen(new AIConfigScreen(this)))
                .bounds(centerX - buttonWidth / 2, messageTop + 60, buttonWidth, buttonHeight)
                .build());
        // 返回
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.back"),
                        btn -> this.onClose())
                .bounds(centerX - buttonWidth / 2, messageTop + 90, buttonWidth, buttonHeight)
                .build());
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 居中绘制提示文字（支持 \n 换行）
        String message = Component.translatable("screen.toneko.ai_disabled.message").getString();
        int lineY = messageTop;
        for (String line : message.split("\n")) {
            int x = (this.width - this.font.width(line)) / 2;
            guiGraphics.drawString(this.font, line, x, lineY, 0xFFFFFF);
            lineY += this.font.lineHeight + 4;
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(lastScreen);
    }

    // 打开时不暂停游戏
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
