package org.cneko.toneko.common.mod.client.screens;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.packets.interactives.ChatWithNekoPayload;
import org.jetbrains.annotations.NotNull;


public class ChatWithNekoScreen extends Screen implements INekoScreen {
    private final NekoEntity neko;
    private EditBox textField;
    public ChatWithNekoScreen(NekoEntity neko) {
        super(Component.empty());
        this.neko = neko;
    }

    @Override
    public NekoEntity getNeko() {
        return neko;
    }

    @Override
    protected void init() {
        super.init();
        // 添加一个大文本框
        textField = new EditBox(font, width / 2 - 150, height / 2 - 10, 300, 30, Component.empty());
        textField.setMaxLength(1000);
        this.addRenderableWidget(textField);
        // 添加一个发送按钮
        this.addRenderableWidget(new Button.Builder(Component.translatable("screen.toneko.chat_with_neko.button.send"), button -> {
            if (!textField.getValue().isEmpty()){
                ClientPlayNetworking.send(new ChatWithNekoPayload(neko.getUUID().toString(),textField.getValue()));
                this.onClose();
            }
        }).size(100,20).pos(width / 2 - 50,height / 2 + 30).build());
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
