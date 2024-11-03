package org.cneko.toneko.common.mod.client.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class InteractionScreen extends Screen {
    private final ButtonBuilders buttonBuilders;
    private Screen lastScreen;
    public InteractionScreen(Component title, @Nullable Screen lastScreen ,ButtonBuilders buttonBuilders) {
        super(title);
        this.lastScreen = lastScreen;
        this.buttonBuilders = buttonBuilders;
    }

    public void init() {
        super.init();
        Map<String,Button.Builder> builders = this.buttonBuilders.getBuilders(this);
        // 仅在屏幕x轴70%外的屏幕中绘制
        int x = (int) (this.width * 0.7);
        int y = (int) (this.height * 0.1);
        int buttonWidth = (int)(this.width * 0.2);
        int buttonHeight = (int)(this.height * 0.06);
        int buttonBound = (int)(this.height * 0.13);

        if (lastScreen != null){
            // 添加返回按钮
            addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> {
                Minecraft.getInstance().setScreen(lastScreen);
            }).size(buttonWidth,buttonHeight).pos(x,y).build());
            y += buttonBound;
        }

        for (String key : builders.keySet()) {
            Button.Builder builder = builders.get(key);
            Button button = builder.size(buttonWidth,buttonHeight).pos(x,y).build();
            button.setTooltip(Tooltip.create(Component.translatable(key+".des")));
            addRenderableWidget(button);
            y += buttonBound;
        }
    }



    // 打开时不暂停游戏
    @Override
    public boolean isPauseScreen() {
        return false;
    }


    // 移除背景渲染
    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(lastScreen);
    }

    @FunctionalInterface
    public interface ButtonBuilders {
        Map<String,Button.Builder> getBuilders(InteractionScreen screen);
    }

}
