package org.cneko.toneko.common.mod.client.screens;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.packets.interactives.NekoMatePayload;
import org.jetbrains.annotations.NotNull;

public class MateConfirmScreen extends Screen {
    private final NekoEntity neko;
    private final INeko entity;
    public MateConfirmScreen(NekoEntity neko, INeko entity) {
        super(Component.empty());
        this.neko = neko;
        this.entity = entity;
    }

    @Override
    protected void init() {
        super.init();
        int buttonWidth = 200;
        int buttonHeight = 20;
        int buttonX = this.width / 2 - buttonWidth / 2;

        // 忽略交配过程按钮
        int ignoreButtonY = this.height / 2 - 40; // 上方按钮
        this.addRenderableWidget(new Button.Builder(Component.literal("忽略交配过程"), button -> {
            this.minecraft.setScreen(null);
            ClientPlayNetworking.send(new NekoMatePayload(neko.getUUID().toString(), entity.getEntity().getUUID().toString()));
        }).bounds(buttonX, ignoreButtonY, buttonWidth, buttonHeight).build());

        // 完整交配过程按钮
        int confirmButtonY = this.height / 2; // 下方按钮
        this.addRenderableWidget(new Button.Builder(Component.literal("完整交配过程"), button -> {
            // 添加完整交配过程逻辑
        }).bounds(buttonX, confirmButtonY, buttonWidth, buttonHeight).build());
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

}
