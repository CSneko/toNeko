package org.cneko.toneko.common.mod.client.screens;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.cneko.toneko.common.util.ConfigBuilder;
import org.cneko.toneko.common.util.ConfigUtil;
import org.jetbrains.annotations.NotNull;

public class ConfigScreen extends Screen {
    private final Screen lastScreen;
    public ConfigScreen(Screen lastScreen) {
        super(Component.empty());
        this.lastScreen = lastScreen;
    }

    @Override
    public void init() {
        super.init();
        int x = (int) (this.width * 0.1);
        var ref = new Object() {
            int y = (int) (height * 0.1);
        };
        int widgetWidth = (int) (this.width * 0.3);
        int inputWidth = (int) (this.width * 0.3); // EditBox 或 Button 的宽度
        int height = (int) (this.height * 0.05);
        int space = 30;
        ConfigUtil.CONFIG_BUILDER.getKeys().forEach(key -> {
            // 创建并添加 ConfigWidget
            ConfigWidget configWidget = new ConfigWidget(x, ref.y, widgetWidth, height, Component.literal(key), this.font);
            addRenderableWidget(configWidget);

            // 计算 EditBox 或 Button 的 x 坐标
            int inputX = x + widgetWidth + space;

            ConfigBuilder.Entry entry = ConfigUtil.CONFIG_BUILDER.get(key);
            switch (entry.type()) {
                case ConfigBuilder.Entry.Types.BOOLEAN -> // 创建并添加 ConfigButton
                        addRenderableWidget(new ConfigButton.Builder(key, ConfigUtil.CONFIG_BUILDER)
                                .bounds(inputX, ref.y, inputWidth, height)
                                .build());
                case ConfigBuilder.Entry.Types.STRING -> {
                    // 创建并添加 EditBox
                    EditBox editBox = new EditBox(this.font, inputX, ref.y, inputWidth, height, Component.literal(key));
                    editBox.setValue(ConfigUtil.CONFIG_BUILDER.get(key).string()); // 设置初始值
                    editBox.setMaxLength(100); // 设置最大长度
                    editBox.setBordered(true); // 设置边框
                    editBox.setVisible(true); // 设置可见

                    // 为 EditBox 添加监听器
                    editBox.setResponder((text) -> {
                        ConfigUtil.CONFIG_BUILDER.setString(key, text);
                    });

                    addRenderableWidget(editBox);
                }
            }

            ref.y += height + space; // 每添加一个组件后，调整 y 坐标
        });
    }


    @Override
    public void onClose() {
        minecraft.setScreen(lastScreen);
    }

    public static class ConfigWidget extends AbstractStringWidget{

        public ConfigWidget(int x, int y, int width, int height, Component message, Font font) {
            super(x, y, width, height, message, font);
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.drawString(getFont(), this.getMessage(), this.getX() + 5, this.getY() + (this.height - 8) / 2, 16777215, false);
        }
    }

    public static class ConfigButton extends Button {

        public ConfigButton(int x, int y, int width, int height, Component message, OnPress onPress, CreateNarration createNarration) {
            super(x, y, width, height, message, onPress, createNarration);
        }

        public static class Builder{
            private final ConfigBuilder.Entry entry;
            private int x;
            private int y;
            private int width;
            private int height;
            private final ConfigBuilder config;
            private final String key;
            public Builder(String key,ConfigBuilder cfg){
                this.entry = cfg.get(key);
                this.config = cfg;
                this.key = key;
            }
            public Builder pos(int x, int y) {
                this.x = x;
                this.y = y;
                return this;
            }

            public Builder width(int width) {
                this.width = width;
                return this;
            }

            public Builder size(int width, int height) {
                this.width = width;
                this.height = height;
                return this;
            }

            public Builder bounds(int x, int y, int width, int height) {
                return this.pos(x, y).size(width, height);
            }

            public ConfigButton build() {
                Component message;
                if (entry.bool()){
                    message = Component.translatable("screen.toneko.config.button.true");
                }else {
                    message = Component.translatable("screen.toneko.config.button.false");
                }
                return new ConfigButton(x, y, width, height,message , (btn)->{
                    config.setBoolean(key,!entry.bool());
                    btn.setMessage(entry.bool()?Component.translatable("screen.toneko.config.button.true"):Component.translatable("screen.toneko.config.button.false"));
                }, ConfigButton.DEFAULT_NARRATION);
            }

        }
    }
}
