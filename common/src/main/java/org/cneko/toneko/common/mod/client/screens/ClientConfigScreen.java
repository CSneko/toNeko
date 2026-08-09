package org.cneko.toneko.common.mod.client.screens;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.cneko.toneko.common.mod.client.util.ClientConfig;
import org.jetbrains.annotations.NotNull;

/**
 * 客户端本地配置屏幕（config/toneko_client.json 的编辑界面）。
 * 由 {@link ConfigScreen} 顶部按钮打开，配置项与服务器配置分离。
 */
public class ClientConfigScreen extends Screen {
    private final Screen lastScreen;

    // == 配色（与 ConfigScreen 一致的粉色系） ==
    private static final int COLOR_PINK_ACCENT = 0xFFFF69B4;
    private static final int COLOR_BG_TOP = 0xC0200510;
    private static final int COLOR_BG_BOTTOM = 0xD0100520;
    /** 预设气泡颜色（RGB） */
    private static final int[] PRESET_COLORS = {0xFF69B4, 0xD08AE0, 0x6FB7FF, 0x7BE07B, 0xFFB35C, 0x404040};

    private EditBox durationBox;
    private EditBox colorBox;

    public ClientConfigScreen(Screen lastScreen) {
        super(Component.translatable("screen.toneko.client_config.title"));
        this.lastScreen = lastScreen;
    }

    @Override
    public void init() {
        super.init();

        int centerX = width / 2;
        int rowY = (int) (height * 0.26);

        // 显示方式切换行：label 左侧，切换按钮右侧
        addRenderableWidget(Button.builder(modeButtonText(), btn -> {
                    ClientConfig.setChatDisplay(ClientConfig.isBubbleMode() ? "chat" : "bubble");
                    btn.setMessage(modeButtonText());
                })
                .bounds(centerX + 60, rowY, 120, 20)
                .build());

        // 气泡时长输入（毫秒）
        durationBox = new EditBox(font, centerX + 60, rowY + 36, 100, 20, Component.literal("duration"));
        durationBox.setMaxLength(6);
        durationBox.setValue(String.valueOf(ClientConfig.getBubbleDuration()));
        durationBox.setResponder(text -> {
            try {
                ClientConfig.setBubbleDuration(Integer.parseInt(text.trim()));
            } catch (NumberFormatException ignored) {
                // 输入中非数字状态，暂不保存
            }
        });
        addRenderableWidget(durationBox);

        // 气泡颜色：预设色块 + hex 输入
        int swatchY = rowY + 72;
        int swatchX = centerX + 60;
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            int color = PRESET_COLORS[i];
            addRenderableWidget(new ColorSwatchButton(swatchX + i * 24, swatchY, color,
                    () -> {
                        ClientConfig.setBubbleColor(String.format("%06X", color));
                        colorBox.setValue(String.format("%06X", ClientConfig.getBubbleColor()));
                    }));
        }
        colorBox = new EditBox(font, swatchX + PRESET_COLORS.length * 24 + 8, swatchY, 90, 20, Component.literal("color"));
        colorBox.setMaxLength(7);
        colorBox.setValue(String.format("%06X", ClientConfig.getBubbleColor()));
        colorBox.setResponder(text -> ClientConfig.setBubbleColor(text));
        addRenderableWidget(colorBox);

        // 返回按钮（底部）
        addRenderableWidget(Button.builder(Component.translatable("screen.toneko.config.button.quit"), btn -> onClose())
                .bounds(centerX - 50, (int) (height * 0.85), 100, 20)
                .build());
    }

    private Component modeButtonText() {
        String mode = ClientConfig.getChatDisplay();
        MutableComponent text = Component.translatable(
                "screen.toneko.client_config.chat_display." + mode);
        return text.withStyle("bubble".equals(mode) ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 背景渐变（与 ConfigScreen 一致）
        guiGraphics.fill(0, 0, width, height, COLOR_BG_TOP);
        guiGraphics.fill(0, height / 2, width, height, COLOR_BG_BOTTOM);

        Component title = Component.translatable("screen.toneko.client_config.title")
                .withStyle(ChatFormatting.BOLD, ChatFormatting.LIGHT_PURPLE);
        guiGraphics.drawCenteredString(this.font, title, this.width / 2, 20, 0xFFFFFF);

        int centerX = width / 2;
        int rowY = (int) (height * 0.26);

        // 行 label
        guiGraphics.drawString(this.font,
                Component.translatable("screen.toneko.client_config.chat_display").withStyle(ChatFormatting.GOLD),
                centerX - 220, rowY + 6, 0xE0E0E0, false);
        guiGraphics.drawString(this.font,
                Component.translatable("screen.toneko.client_config.bubble_duration").withStyle(ChatFormatting.GOLD),
                centerX - 220, rowY + 42, 0xE0E0E0, false);
        guiGraphics.drawString(this.font,
                Component.translatable("screen.toneko.client_config.bubble_color").withStyle(ChatFormatting.GOLD),
                centerX - 220, rowY + 78, 0xE0E0E0, false);

        // 说明文字
        int descY = rowY + 110;
        for (String line : Component.translatable("screen.toneko.client_config.desc").getString().split("\n")) {
            guiGraphics.drawCenteredString(this.font, Component.literal(line).withStyle(ChatFormatting.GRAY),
                    this.width / 2, descY, 0xFFFFFF);
            descY += 12;
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(lastScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {}

    /** 预设颜色色块按钮：点击选中并保存；当前选中色带高亮边框 */
    private static class ColorSwatchButton extends AbstractWidget {
        private final int color;
        private final Runnable onPick;

        ColorSwatchButton(int x, int y, int color, Runnable onPick) {
            super(x, y, 20, 20, Component.literal(String.format("%06X", color)));
            this.color = color;
            this.onPick = onPick;
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int size = 20;
            g.fill(getX(), getY(), getX() + size, getY() + size, 0xFF000000 | color);
            // 选中态：粉框高亮
            if (ClientConfig.getBubbleColor() == color) {
                g.renderOutline(getX() - 1, getY() - 1, size + 2, size + 2, COLOR_PINK_ACCENT);
            }
            // 悬停：亮框
            if (isHovered()) {
                g.renderOutline(getX() - 1, getY() - 1, size + 2, size + 2, 0xFFFFFFFF);
            }
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {}

        @Override
        public void onClick(double mouseX, double mouseY) {
            onPick.run();
        }
    }
}
