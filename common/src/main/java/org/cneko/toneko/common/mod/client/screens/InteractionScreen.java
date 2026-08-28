package org.cneko.toneko.common.mod.client.screens;

import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InteractionScreen extends Screen implements INekoScreen {
    @Setter
    private NekoScreenBuilder builder;
    private List<TooltipWidget> tooltips;
    public Screen lastScreen;
    private final NekoEntity neko;
    protected int startY = 0;

    public InteractionScreen(Component title, NekoEntity neko, @Nullable Screen lastScreen) {
        super(title);
        this.lastScreen = lastScreen;
        this.neko = neko;
    }

    public InteractionScreen(Component title, NekoEntity neko, @Nullable Screen lastScreen, @NotNull NekoScreenBuilder builder) {
        super(title);
        this.builder = builder;
        this.lastScreen = lastScreen;
        this.neko = neko;
    }

    @Override
    public NekoEntity getNeko() {
        return neko;
    }

    public void init() {
        super.init();

        if (builder != null) {
            startY = builder.getStartY();
            // 定义左侧起始位置（屏幕宽度的3%处）
            int leftX = (int) (width * 0.03);
            // 右侧按钮位置（屏幕宽度的70%处）
            int rightX = (int) (width * 0.7);
            // 使用默认的起始Y位置
            int baseY = startY != 0 ? startY : (int) (this.height * 0.1);
            int buttonWidth = 100;
            int buttonHeight = 20;
            int buttonBound = 30;
            int tooltipBound = 20;

            // 分别记录按钮和工具提示的当前Y位置
            int currentButtonY = baseY;
            int currentTooltipY = baseY;

            tooltips = new ArrayList<>();
            for (NekoScreenBuilder.WidgetFactory widget : this.builder.getWidgets()) {
                if (widget instanceof NekoScreenBuilder.ButtonFactory buttonFactory) {
                    Button.Builder btnBuilder = buttonFactory.build(this);
                    // 按钮放置在右侧，使用按钮的Y位置
                    Button button = btnBuilder.size(buttonWidth, buttonHeight).pos(rightX, currentButtonY).build();
                    if (button.getMessage() instanceof MutableComponent component && component.getContents() instanceof TranslatableContents t) {
                        // 判断语言文件中有没有描述
                        if (!Objects.equals(t.getFallback(), t.getKey())) {
                            button.setTooltip(Tooltip.create(Component.translatable(t.getKey() + ".des")));
                        }
                    }
                    addRenderableWidget(button);
                    // 更新按钮的Y位置（下移）
                    currentButtonY += buttonBound;
                }
                if (widget instanceof NekoScreenBuilder.TooltipFactory tooltipFactory) {
                    // 工具提示放置在左侧，使用工具提示的Y位置
                    tooltips.add(new TooltipWidget(leftX, currentTooltipY, tooltipFactory));
                    // 更新工具提示的Y位置（下移）
                    currentTooltipY += tooltipBound;
                }
            }
        }
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        try {
            if (tooltips != null) {
                for (TooltipWidget widget : tooltips) {
                    net.minecraft.network.chat.Component comp = widget.tooltip().build(this);
                    if (comp == null || comp.getString().isEmpty()) {
                        continue;
                    }
                    // 26.1.2：setTooltipForNextFrame 每帧只保留最后一次调用，导致多行信息只剩一条；
                    // 这里改为在固定位置逐条绘制带底色的文本（保留原悬浮框观感，超宽自动换行）
                    int x = widget.x();
                    int y = widget.y();
                    var lines = this.font.split(comp, 200);
                    for (var line : lines) {
                        int textWidth = this.font.width(line);
                        guiGraphics.fill(x - 3, y - 3, x + textWidth + 3, y + this.font.lineHeight + 2, 0xF0100010);
                        // 26.1.2：GuiGraphicsExtractor.text 要求颜色带非 0 alpha，否则整条文本被跳过
                        guiGraphics.text(this.font, line, x, y, 0xFFFFFFFF, false);
                        y += this.font.lineHeight + 2;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        if (redFlashTimer > 0) {
            int alpha = (int) (76 * redFlashTimer / 10.0f);
            guiGraphics.fill(0, 0, width, height, (alpha << 24) | 0xFF0000);
        }
    }

    // 打开时不暂停游戏
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // 26.x：背景由抽取管线处理；保留空实现以维持原视觉
    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(lastScreen);
    }

    private int redFlashTimer = 0;

    public void triggerRedFlash() {
        redFlashTimer = 10;
    }

    @Override
    public void tick() {
        if (redFlashTimer > 0) redFlashTimer--;
    }

    public record TooltipWidget(int x, int y, NekoScreenBuilder.TooltipFactory tooltip) {
    }
}