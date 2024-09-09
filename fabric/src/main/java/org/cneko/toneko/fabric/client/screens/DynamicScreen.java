package org.cneko.toneko.fabric.client.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class DynamicScreen extends Screen {
    public DynamicScreen(Component title) {
        super(title);
    }


    // 打开时不暂停游戏
    @Override
    public boolean isPauseScreen() {
        return false;
    }


    // 移除背景渲染
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }
}
