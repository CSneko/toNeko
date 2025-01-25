package org.cneko.toneko.common.mod.client.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.cneko.toneko.common.util.ConfigBuilder;
import org.cneko.toneko.common.util.ConfigUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ConfigScreen extends Screen {
    private final Screen lastScreen;
    private ScrollPanel scrollPanel;
    private ConfigWidget randomText;
    private static int randoms = 29;

    public ConfigScreen(Screen lastScreen) {
        super(Component.empty());
        this.lastScreen = lastScreen;
    }

    public ConfigScreen() {
        this(null);
    }


    @Override
    public void init() {
        super.init();

        // 分区1：标题（固定）
        Component titleText = Component.translatable("screen.toneko.config.title");
        int titleX = (this.width - this.font.width(titleText)) / 2;
        int titleY = (int) (this.height * 0.05);
        addRenderableWidget(new TextWidget(titleX, titleY, titleText, this.font));

        // 分区2：滚动内容
        int scrollX = (int) (this.width * 0.1);
        int scrollY = (int) (this.height * 0.15);
        int scrollWidth = (int) (this.width * 0.8);
        int scrollHeight = (int) (this.height * 0.65);

        this.scrollPanel = new ScrollPanel(scrollX, scrollY, scrollWidth, scrollHeight, Component.empty());

        ConfigUtil.CONFIG_BUILDER.getKeys().forEach(key -> {
            int widgetHeight = 20; // 每行组件的高度
            int space = 10; // 文本与编辑框/按钮之间的水平间隔

            // 获取配置项
            ConfigBuilder.Entry entry = ConfigUtil.CONFIG_BUILDER.get(key);

            // 文本组件
            int textWidth = (int) (scrollWidth * 0.35); // 文本宽度占滚动区域的35%
            ConfigWidget configWidget = new ConfigWidget(0, 0, textWidth, widgetHeight,
                    Component.translatable("screen.toneko.config.key." + key), this.font,entry.url());
            configWidget.setTooltip(Tooltip.create(Component.translatable("screen.toneko.config.key." + key + ".des")));

            AbstractWidget inputComponent;

            if (entry.type() == ConfigBuilder.Entry.Types.BOOLEAN) {
                inputComponent = new ConfigButton.Builder(key, ConfigUtil.CONFIG_BUILDER)
                        .bounds(0, 0, textWidth, widgetHeight)
                        .build();
            } else if (entry.type() == ConfigBuilder.Entry.Types.STRING) {
                EditBox editBox = new EditBox(this.font, 0, 0, textWidth, widgetHeight,
                        Component.literal(key));
                editBox.setMaxLength(1000);
                editBox.setValue(ConfigUtil.CONFIG_BUILDER.getExist(key).string());
                editBox.setResponder(text -> ConfigUtil.CONFIG_BUILDER.setString(key, text));
                inputComponent = editBox;
            } else {
                return; // 未知类型，跳过
            }

            inputComponent.setX(configWidget.getX() + textWidth + space);
            inputComponent.setY(configWidget.getY() + (configWidget.getHeight() - inputComponent.getHeight()) / 2);

            // 添加到滚动面板
            scrollPanel.addTextWidget(configWidget);
            scrollPanel.addWidget(inputComponent);

            // 更新内容总高度（不添加额外间隔）
            scrollPanel.totalContentHeight += widgetHeight;
        });

        // 文本组件
        int textWidth = (int) (scrollWidth * 0.35); // 文本宽度占滚动区域的35%

        int random = new Random().nextInt(randoms);
        randomText = new ConfigWidget(0, 0,textWidth,20,Component.translatable("screen.toneko.config.random."+random),this.font);
        scrollPanel.addTextWidget(randomText);
        scrollPanel.addWidget(new Button.Builder(Component.translatable("screen.toneko.config.button.random"),(btn)-> modifyRandomText()).size(textWidth,20).build());
        scrollPanel.totalContentHeight += 20;

        addRenderableWidget(scrollPanel);

        // 分区3：底部按钮（固定）
        int btnWidth = (int) (this.width * 0.3);
        int btnHeight = 20;
        int btnY = (int) (this.height * 0.9);
        int btnSpacing = (int) (this.width * 0.05);

        Button leftButton = Button.builder(Component.translatable("screen.toneko.config.button.quit"),
                        btn -> minecraft.setScreen(lastScreen))
                .bounds(this.width / 2 - btnWidth - btnSpacing / 2, btnY, btnWidth, btnHeight).build();
        Button rightButton = Button.builder(Component.translatable("screen.toneko.config.button.apply"),
                        btn -> ConfigUtil.load())
                .bounds(this.width / 2 + btnSpacing / 2, btnY, btnWidth, btnHeight).build();

        addRenderableWidget(leftButton);
        addRenderableWidget(rightButton);
    }

    public void modifyRandomText(){
        int random = new Random().nextInt(randoms);
        randomText.setMessage(Component.translatable("screen.toneko.config.random."+random));
    }



    @Override
    public void onClose() {
        minecraft.setScreen(lastScreen);
    }

    public static class ConfigWidget extends AbstractStringWidget {
        private String url;
        public ConfigWidget(int x, int y, int width, int height, Component message, Font font) {
            super(x, y, width, height, message, font);
        }
        public ConfigWidget(int x, int y, int width, int height, Component message, Font font,@Nullable String url){
            this(x, y, width, height, message, font);
            this.url = url;
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.drawString(getFont(), this.getMessage(), this.getX() + 5, this.getY() + (this.height - 8) / 2,
                    16777215, false);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (url != null){
                ConfirmLinkScreen.confirmLinkNow(Minecraft.getInstance().screen, url);
            }
        }
    }

    public static class ScrollPanel extends AbstractContainerWidget {
        private final List<AbstractWidget> children = new ArrayList<>();
        private final int panelWidth;
        private final int panelHeight;
        private int scrollAmount = 0; // 当前滚动偏移量
        private int totalContentHeight = 0; // 子组件的总高度
        private final int scrollbarWidth = 6; // 滚动条宽度
        private boolean isDragging = false; // 是否在拖动滚动条

        public ScrollPanel(int x, int y, int width, int height, Component title) {
            super(x, y, width, height, title);
            this.panelWidth = width;
            this.panelHeight = height;
        }

        public void addWidget(AbstractWidget widget) {
            widget.setX((int) (this.getX() + width*0.5)); // 添加间距
            widget.setY(this.getY() + totalContentHeight - scrollAmount + 5);
            this.children.add(widget);
        }
        public void addTextWidget(ConfigWidget configWidget) {
            configWidget.setX(this.getX() + 5);
            configWidget.setY(this.getY() + totalContentHeight - scrollAmount + 5);
            this.children.add(configWidget);
            this.totalContentHeight += configWidget.getHeight() + 10; // 更新内容高度
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            // 裁剪区域，防止组件超出面板边界
            guiGraphics.pose().pushPose();
            guiGraphics.enableScissor(this.getX(), this.getY(), this.getX() + this.panelWidth, this.getY() + this.panelHeight);

            for (AbstractWidget widget : children) {
                int widgetY = widget.getY();
                if (widgetY >= this.getY() && widgetY <= this.getY() + this.panelHeight) {
                    widget.render(guiGraphics, mouseX, mouseY, partialTick);
                }
            }

            guiGraphics.disableScissor();
            guiGraphics.pose().popPose();

            // 绘制滚动条
            if (totalContentHeight > panelHeight) {
                int scrollbarHeight = Math.max((int) ((double) panelHeight / totalContentHeight * panelHeight), 10);
                int scrollbarX = this.getX() + this.panelWidth - scrollbarWidth;
                int scrollbarY = this.getY() + (int) ((double) scrollAmount / totalContentHeight * panelHeight);
                guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + scrollbarWidth, scrollbarY + scrollbarHeight, 0xFFAAAAAA);
            }
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            if (isDragging) {
                // 拖动开始时的鼠标 Y
                int dragStartY = 0;
                int dragDelta = (int) (mouseY - dragStartY);
                double scrollRatio = (double) totalContentHeight / panelHeight;
                // 拖动开始时的滚动偏移量
                int initialScrollAmount = 0;
                scrollAmount = (int) Math.max(0, Math.min(totalContentHeight - panelHeight, initialScrollAmount + dragDelta * scrollRatio));
                updateChildrenPositions();
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            if (totalContentHeight > panelHeight) {
                // 更新滚动偏移量，20 是滚动速度，可以根据需要调整
                scrollAmount = (int) Math.max(0, Math.min(totalContentHeight - panelHeight, scrollAmount - scrollY * 20));
                updateChildrenPositions(); // 更新子组件位置
                return true;
            }
            return false;
        }



        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (button == 0 && isDragging) {
                isDragging = false;
                return true;
            }
            return false;
        }

        private boolean isOverScrollbar(double mouseX, double mouseY) {
            int scrollbarX = this.getX() + this.panelWidth - scrollbarWidth;
            return mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth &&
                    mouseY >= this.getY() && mouseY <= this.getY() + this.panelHeight;
        }

        private void updateChildrenPositions() {
            int currentY = this.getY() + 5 - scrollAmount;
            for (AbstractWidget widget : children) {
                widget.setY(currentY);
                // 如果是文本与输入框的组合，需要特殊处理
                if (widget instanceof ConfigWidget configWidget) {
                    currentY += configWidget.getHeight(); // 增加文本高度
                } else {
                    currentY += widget.getHeight() + 10; // 增加间隔
                }
            }
        }


        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            return children;
        }

        @Override
        public void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
            // Narration is optional
        }

    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollPanel != null && mouseX >= scrollPanel.getX() && mouseX <= scrollPanel.getX() + scrollPanel.getWidth()
                && mouseY >= scrollPanel.getY() && mouseY <= scrollPanel.getY() + scrollPanel.getHeight()) {
            // 将滚轮滚动量传递给滚动面板
            return scrollPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }


    public static class TextWidget extends AbstractWidget {
        private final Font font;

        public TextWidget(int x, int y, Component message, Font font) {
            super(x, y, font.width(message), font.lineHeight, message);
            this.font = font;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.drawString(this.font, this.getMessage(), this.getX(), this.getY(), 16777215, false);
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {

        }
    }

    public static class ConfigButton extends Button {

        public ConfigButton(int x, int y, int width, int height, Component message, OnPress onPress, CreateNarration createNarration) {
            super(x, y, width, height, message, onPress, createNarration);
//            this.setAlpha(0.5f);
        }

        @Override
        protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
//            int color = 0X5cb85c;
//            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, color);
        }

        public static class Builder{
            private final ConfigBuilder.Entry entry;
            private int x;
            private int y;
            private int width;
            private int height;
            private final ConfigBuilder config;
            private final String key;
            public boolean value;
            public Builder(String key,ConfigBuilder cfg){
                this.entry = cfg.getExist(key);
                this.config = cfg;
                this.key = key;
                this.value = entry.bool();
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
                    config.setBoolean(key,!value);
                    value = !value;
                    btn.setMessage(value?Component.translatable("screen.toneko.config.button.true"):Component.translatable("screen.toneko.config.button.false"));
                }, ConfigButton.DEFAULT_NARRATION);
            }

        }
    }
}
