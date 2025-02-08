package org.cneko.toneko.common.mod.client.screens;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.cneko.toneko.common.util.ConfigBuilder;
import org.cneko.toneko.common.util.ConfigUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 修改说明：
 * 1. 根据配置 key 构建了一颗树，节点类型 ConfigNode。
 * 2. 重写了 init()，先构造滚动面板，再调用 buildConfigTree() 构造树，再递归添加组件（组头和叶节点）。
 * 3. 对折叠组使用了新的 FoldableGroupHeaderWidget，当点击时切换折叠状态，并重建滚动面板中的组件。
 * 4. 为子项添加缩进，每下一级缩进20px。
 */
public class ConfigScreen extends Screen {
    private final Screen lastScreen;
    private ScrollPanel scrollPanel;
    private ConfigWidget randomText;
    private static int randoms = 29;
    // 存放所有配置项构成的树状结构
    private List<ConfigNode> configTree;

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

        // 根据 ConfigBuilder 中已排序好的 key 构造树
        configTree = buildConfigTree();
        // 重建滚动面板：递归地添加所有配置项组件
        rebuildScrollPanel();

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

    /**
     * 重建滚动面板内容，根据 configTree 递归添加组件。
     */
    private void rebuildScrollPanel() {
        scrollPanel.clearWidgets();

        // 先添加配置树组件
        int indent = 0;
        for (ConfigNode node : configTree) {
            addNodeWidgets(node, indent);
        }

        // 添加随机文本和按钮（固定在底部）
        int scrollWidth = scrollPanel.getWidth();
        int textWidth = (int) (scrollWidth * 0.35);
        int random = new Random().nextInt(randoms);
        // 将 X 坐标设置为 scrollPanel.getX() + 5，与其他组件一致
        randomText = new ConfigWidget(scrollPanel.getX() + 5, 0, textWidth, 20,
                Component.translatable("screen.toneko.config.random." + random), this.font);
        scrollPanel.addTextWidget(randomText);
        scrollPanel.addWidget(new Button.Builder(
                Component.translatable("screen.toneko.config.button.random"),
                (btn) -> modifyRandomText())
                .size(textWidth, 20).build());



        scrollPanel.updateChildrenPositions();
    }


    /**
     * 递归添加树节点对应的组件到滚动面板
     *
     * @param node   当前树节点
     * @param indent 当前缩进像素数（每级建议20px）
     */
    private void addNodeWidgets(ConfigNode node, int indent) {
        int widgetHeight = 20;
        int space = 10;
        int scrollWidth = scrollPanel.getWidth();
        // 文本区域宽度：原先占滚动区域 35% 的宽度，再减去缩进
        int textWidth = (int) (scrollWidth * 0.35) - indent;
        if (textWidth < 50) textWidth = 50;

        if (node.isGroupHeader()) {
            // 如果没有对应配置值且有子节点，则作为折叠组头显示
            String arrow = node.collapsed ? " [+] " : " [-] ";
            Component headerText = Component.literal(arrow + node.name);
            // 组头 x 坐标增加缩进：scrollPanel.getX() + 5 + indent
            FoldableGroupHeaderWidget groupHeader = new FoldableGroupHeaderWidget(
                    scrollPanel.getX() + 5 + indent, 0, textWidth, widgetHeight,
                    headerText, this.font, node, this::rebuildScrollPanel);
            scrollPanel.addTextWidget(groupHeader);
            if (!node.collapsed) {
                // 展开状态下递归添加子节点（缩进加 20 像素）
                for (ConfigNode child : node.children) {
                    addNodeWidgets(child, indent + 20);
                }
            }
        } else if (node.fullKey != null) {
            // 叶节点：创建标签和对应的输入组件
            Component labelText = Component.translatable("screen.toneko.config.key." + node.fullKey);
            ConfigWidget configWidget = new ConfigWidget(0, 0, textWidth, widgetHeight,
                    labelText, this.font, node.entry.url());
            configWidget.setTooltip(Tooltip.create(Component.translatable("screen.toneko.config.key." + node.fullKey + ".des")));
            // 设置标签 x 坐标为滚动面板起始位置 + 5 + 缩进
            configWidget.setX(scrollPanel.getX() + 5 + indent);
            scrollPanel.addTextWidget(configWidget);

            AbstractWidget inputComponent;
            if (node.entry.type() == ConfigBuilder.Entry.Types.BOOLEAN) {
                inputComponent = new ConfigButton.Builder(node.fullKey, ConfigUtil.CONFIG_BUILDER)
                        .bounds(0, 0, textWidth, widgetHeight)
                        .build();
            } else if (node.entry.type() == ConfigBuilder.Entry.Types.STRING) {
                EditBox editBox = new EditBox(this.font, 0, 0, textWidth, widgetHeight,
                        Component.literal(node.fullKey));
                editBox.setMaxLength(1000);
                editBox.setValue(ConfigUtil.CONFIG_BUILDER.getExist(node.fullKey).string());
                editBox.setResponder(text -> ConfigUtil.CONFIG_BUILDER.setString(node.fullKey, text));
                inputComponent = editBox;
            } else {
                return; // 未知类型则跳过
            }
            // 设置输入组件的 x 坐标，同样加上缩进
            inputComponent.setX(configWidget.getX() + textWidth + space);
            inputComponent.setY(configWidget.getY() + (widgetHeight - inputComponent.getHeight()) / 2);
            scrollPanel.addWidget(inputComponent);
            scrollPanel.totalContentHeight += widgetHeight;
        }
    }

    /**
     * 根据 ConfigBuilder 的 key 构造树状结构
     */
    private List<ConfigNode> buildConfigTree() {
        List<ConfigNode> roots = new ArrayList<>();
        List<String> keys = ConfigUtil.CONFIG_BUILDER.getKeys();
        for (String key : keys) {
            String[] parts = key.split("\\.");
            insertNode(roots, parts, 0, key);
        }
        return roots;
    }

    /**
     * 辅助方法：递归插入 key（已分割后的 parts）到节点列表中
     *
     * @param nodes    当前节点列表
     * @param parts    key 按点号分割的各部分
     * @param index    当前处理到的部分下标
     * @param fullKey  完整的 key 字符串
     */
    private void insertNode(List<ConfigNode> nodes, String[] parts, int index, String fullKey) {
        String part = parts[index];
        ConfigNode node = null;
        for (ConfigNode n : nodes) {
            if (n.name.equals(part)) {
                node = n;
                break;
            }
        }
        if (node == null) {
            node = new ConfigNode(part);
            nodes.add(node);
        }
        if (index == parts.length - 1) {
            // 最后一个部分，表示这是一个实际的配置项
            node.fullKey = fullKey;
            node.entry = ConfigUtil.CONFIG_BUILDER.get(fullKey);
        } else {
            // 如果该节点已经有值，则根据要求它不能作为父键（忽略后续子项）
            if (node.fullKey != null) {
                return;
            }
            if (node.children == null) {
                node.children = new ArrayList<>();
            }
            insertNode(node.children, parts, index + 1, fullKey);
        }
    }

    public void modifyRandomText(){
        int random = new Random().nextInt(randoms);
        randomText.setMessage(Component.translatable("screen.toneko.config.random." + random));
    }

    @Override
    public void onClose() {
        minecraft.setScreen(lastScreen);
    }

    /*
     * ================================
     * 以下为内部辅助类，主要修改了：
     * 1. 新增 ConfigNode 表示配置树的节点
     * 2. 新增 FoldableGroupHeaderWidget 用于折叠组头显示
     * 3. 修改 ScrollPanel，增加 clearWidgets() 方法以支持重建组件列表
     * 其余组件基本沿用原有代码
     * ================================
     */

    /**
     * 配置树节点（注意：如果 node.fullKey != null 则表示这是一个实际配置项，不能作为父键）
     */
    public static class ConfigNode {
        String name;
        @Nullable
        String fullKey = null;
        @Nullable
        ConfigBuilder.Entry entry = null;
        List<ConfigNode> children = new ArrayList<>();
        boolean collapsed = false;

        public ConfigNode(String name) {
            this.name = name;
        }

        /**
         * 当该节点没有配置值且有子节点时，认为它是一个折叠组头
         */
        public boolean isGroupHeader() {
            return (entry == null) && (!children.isEmpty());
        }
    }

    /**
     * 用于显示折叠组头的组件，点击时切换折叠状态并重建滚动面板
     */
    public class FoldableGroupHeaderWidget extends AbstractWidget {
        private final ConfigNode node;
        private final Runnable onToggle;
        private final Font font;

        public FoldableGroupHeaderWidget(int x, int y, int width, int height, Component message, Font font,
                                         ConfigNode node, Runnable onToggle) {
            super(x, y, width, height, message);
            this.font = font;
            this.node = node;
            this.onToggle = onToggle;
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            // 根据当前折叠状态确定箭头符号
            String arrow = node.collapsed ? " [+] " : " [-] ";
            // 使用本地化键 "screen.toneko.config.group.<节点名>"
            MutableComponent text = Component.literal(arrow).withStyle(ChatFormatting.GRAY);
            Component headerText = Component.translatable("screen.toneko.config.group." + node.name);
            text = text.append(headerText);
            // 绘制文本（左边留出 5px 内边距）
            guiGraphics.drawString(font, text, getX() + 5, getY() + (getHeight() - font.lineHeight) / 2,
                    0xFFFFFF, false);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            node.collapsed = !node.collapsed;
            onToggle.run();
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
        }
    }

    /**
     * 修改后的滚动面板，增加了 clearWidgets() 方法以便每次重建内容时清空已有组件
     */
    public static class ScrollPanel extends AbstractContainerWidget {
        private final List<AbstractWidget> children = new ArrayList<>();
        private final int panelWidth;
        private final int panelHeight;
        private int scrollAmount = 0; // 当前滚动偏移量
        public int totalContentHeight = 0; // 子组件的总高度
        private final int scrollbarWidth = 6; // 滚动条宽度
        private boolean isDragging = false; // 是否在拖动滚动条

        public ScrollPanel(int x, int y, int width, int height, Component title) {
            super(x, y, width, height, title);
            this.panelWidth = width;
            this.panelHeight = height;
        }

        public void clearWidgets() {
            children.clear();
            totalContentHeight = 0;
        }

        public void addWidget(AbstractWidget widget) {
            widget.setX((int) (this.getX() + width * 0.5)); // 该方法调用后可在外部调整 x 坐标
            widget.setY(this.getY() + totalContentHeight - scrollAmount + 5);
            this.children.add(widget);
        }

        public void addTextWidget(AbstractWidget widget) {
            widget.setY(this.getY() + totalContentHeight - scrollAmount + 5);
            this.children.add(widget);
            totalContentHeight += widget.getHeight() + 10;
        }


        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
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
                int dragStartY = 0;
                int dragDelta = (int) (mouseY - dragStartY);
                double scrollRatio = (double) totalContentHeight / panelHeight;
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
                scrollAmount = (int) Math.max(0, Math.min(totalContentHeight - panelHeight, scrollAmount - scrollY * 20));
                updateChildrenPositions();
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

        public void updateChildrenPositions() {
            int currentY = this.getY() + 5 - scrollAmount;
            for (int i = 0; i < children.size(); i++) {
                AbstractWidget widget = children.get(i);
                if (widget instanceof ConfigWidget &&
                        i + 1 < children.size() &&
                        (children.get(i + 1) instanceof EditBox || children.get(i + 1) instanceof Button)) {
                    // 将标签和输入组件对齐在同一行
                    widget.setY(currentY);
                    AbstractWidget inputWidget = children.get(i + 1);
                    int labelHeight = widget.getHeight();
                    inputWidget.setY(currentY + (labelHeight - inputWidget.getHeight()) / 2);
                    currentY += labelHeight + 10;
                    i++; // 跳过下一个组件（已处理）
                } else {
                    widget.setY(currentY);
                    currentY += widget.getHeight() + 10;
                }
            }
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            return children;
        }

        @Override
        public void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
        }
    }

    public static class ConfigWidget extends AbstractStringWidget {
        private String url;
        public ConfigWidget(int x, int y, int width, int height, Component message, Font font) {
            super(x, y, width, height, message, font);
        }
        public ConfigWidget(int x, int y, int width, int height, Component message, Font font, @Nullable String url) {
            this(x, y, width, height, message, font);
            this.url = url;
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.drawString(getFont(), this.getMessage(), this.getX() + 5,
                    this.getY() + (this.height - 8) / 2, 16777215, false);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (url != null) {
                ConfirmLinkScreen.confirmLinkNow(Minecraft.getInstance().screen, url);
            }
        }
    }

    public static class TextWidget extends AbstractWidget {
        private final Font font;

        public TextWidget(int x, int y, Component message, Font font) {
            super(x, y, font.width(message), font.lineHeight, message);
            this.font = font;
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.drawString(this.font, this.getMessage(), this.getX(), this.getY(), 16777215, false);
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
        }
    }

    public static class ConfigButton extends Button {
        public ConfigButton(int x, int y, int width, int height, Component message, OnPress onPress, CreateNarration createNarration) {
            super(x, y, width, height, message, onPress, createNarration);
        }

        @Override
        protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        }

        public static class Builder {
            private final ConfigBuilder.Entry entry;
            private int x;
            private int y;
            private int width;
            private int height;
            private final ConfigBuilder config;
            private final String key;
            public boolean value;
            public Builder(String key, ConfigBuilder cfg) {
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
                } else {
                    message = Component.translatable("screen.toneko.config.button.false");
                }
                return new ConfigButton(x, y, width, height, message, (btn) -> {
                    config.setBoolean(key, !value);
                    value = !value;
                    btn.setMessage(value ?
                            Component.translatable("screen.toneko.config.button.true") :
                            Component.translatable("screen.toneko.config.button.false"));
                }, ConfigButton.DEFAULT_NARRATION);
            }
        }
    }
}
