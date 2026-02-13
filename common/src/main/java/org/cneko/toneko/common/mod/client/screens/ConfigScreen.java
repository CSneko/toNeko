package org.cneko.toneko.common.mod.client.screens;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import org.cneko.toneko.common.util.ConfigBuilder;
import org.cneko.toneko.common.util.ConfigUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ConfigScreen extends Screen {
    private final Screen lastScreen;
    private ConfigListWidget configList;
    private final List<ConfigNode> configTree;

    // == 配色常量 (可爱粉色系) ==
    private static final int COLOR_PINK_ACCENT = 0xFFFF69B4; // 亮粉色
    private static final int COLOR_PINK_SOFT = 0xFFFFB6C1;   // 浅粉色
    private static final int COLOR_BG_TOP = 0xC0200510;      // 背景渐变上 (深粉紫)
    private static final int COLOR_BG_BOTTOM = 0xD0100520;   // 背景渐变下 (深紫)
    private static final int COLOR_LIST_BG = 0x60000000;     // 列表半透明黑底
    private static final int COLOR_LIST_BORDER = 0xFFFF69B4; // 列表边框

    public ConfigScreen(Screen lastScreen) {
        super(Component.translatable("screen.toneko.config.title"));
        this.lastScreen = lastScreen;
        this.configTree = buildConfigTree();
    }

    public ConfigScreen() {
        this(null);
    }

    @Override
    public void init() {
        super.init();

        // 1. 列表区域
        int listWidth = (int) (this.width * 0.75);
        int listHeight = (int) (this.height * 0.65);
        int listX = (this.width - listWidth) / 2;
        int listY = (int) (this.height * 0.15);

        this.configList = new ConfigListWidget(listX, listY, listWidth, listHeight);
        addRenderableWidget(this.configList);

        rebuildList();

        // 2. 底部按钮
        int btnWidth = 100;
        int btnHeight = 20;
        int btnY = (int) (this.height * 0.88);
        int center = this.width / 2;

        addRenderableWidget(Button.builder(Component.translatable("screen.toneko.config.button.quit"), btn -> onClose())
                .bounds(center - btnWidth - 10, btnY, btnWidth, btnHeight)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("screen.toneko.config.button.apply"), btn -> {
                    ConfigUtil.CONFIG.save();
                    ConfigUtil.load();
                })
                .bounds(center + 10, btnY, btnWidth, btnHeight)
                .build());
    }

    private void rebuildList() {
        // 保存当前滚动的距离，重建后恢复
        double oldScroll = configList != null ? configList.getScrollAmount() : 0;

        configList.clearEntries();
        for (ConfigNode node : configTree) {
            addNodeToUI(node, 0);
        }
        configList.addEntry(new RandomTextEntry(configList.width));

        configList.setScrollAmount(oldScroll);
    }

    private void addNodeToUI(ConfigNode node, int indent) {
        if (node.isGroupHeader()) {
            configList.addEntry(new GroupHeaderEntry(node, indent, configList.width, this::rebuildList));
            if (!node.collapsed) {
                for (ConfigNode child : node.children) {
                    addNodeToUI(child, indent + 12);
                }
            }
        } else if (node.fullKey != null) {
            configList.addEntry(new ConfigSettingEntry(node, indent, configList.width));
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        Component title = Component.translatable("screen.toneko.config.title")
                .withStyle(ChatFormatting.BOLD, ChatFormatting.LIGHT_PURPLE);
        guiGraphics.drawCenteredString(this.font, title, this.width / 2, 20, 0xFFFFFF);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    /**
     * 关键修复：Screen 必须显式处理点击并设置焦点
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 如果点击被子组件（列表）处理了
        if (super.mouseClicked(mouseX, mouseY, button)) {
            // 找到那个被点击的组件，并设为 Screen 的焦点
            // 这步至关重要，否则 Screen 不会将键盘事件发给 List
            for (GuiEventListener child : this.children()) {
                if (child.isMouseOver(mouseX, mouseY)) {
                    this.setFocused(child);
                    break;
                }
            }
            return true;
        }
        // 点击了空白处，清除焦点
        this.setFocused(null);
        return false;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(lastScreen);
    }

    // ================== 数据结构 ==================
    private List<ConfigNode> buildConfigTree() {
        List<ConfigNode> roots = new ArrayList<>();
        List<String> keys = ConfigUtil.CONFIG_BUILDER.getKeys();
        for (String key : keys) {
            insertNode(roots, key.split("\\."), 0, key);
        }
        return roots;
    }

    private void insertNode(List<ConfigNode> nodes, String[] parts, int index, String fullKey) {
        String part = parts[index];
        ConfigNode node = nodes.stream().filter(n -> n.name.equals(part)).findFirst().orElse(null);
        if (node == null) {
            node = new ConfigNode(part);
            nodes.add(node);
        }
        if (index == parts.length - 1) {
            node.fullKey = fullKey;
            node.entry = ConfigUtil.CONFIG_BUILDER.get(fullKey);
        } else {
            if (node.fullKey != null) return;
            insertNode(node.children, parts, index + 1, fullKey);
        }
    }

    public static class ConfigNode {
        String name;
        @Nullable String fullKey;
        @Nullable ConfigBuilder.Entry entry;
        List<ConfigNode> children = new ArrayList<>();
        boolean collapsed = false;
        public ConfigNode(String name) { this.name = name; }
        public boolean isGroupHeader() { return entry == null && !children.isEmpty(); }
    }

    // ================== 核心 UI 组件：ConfigListWidget ==================

    private static class ConfigListWidget extends AbstractContainerWidget {
        private final List<Entry> entries = new ArrayList<>();
        private double scrollAmount = 0;
        private boolean isDraggingScrollbar = false;
        private final int width, height;

        public ConfigListWidget(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty());
            this.width = width;
            this.height = height;
        }

        public void addEntry(Entry entry) { entries.add(entry); }
        public void clearEntries() { entries.clear(); }
        private int getContentHeight() { return entries.stream().mapToInt(Entry::getHeight).sum(); }
        public double getScrollAmount() { return scrollAmount; }
        public void setScrollAmount(double amount) {
            this.scrollAmount = Mth.clamp(amount, 0, Math.max(0, getContentHeight() - this.height));
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, COLOR_LIST_BG);
            guiGraphics.renderOutline(getX() - 1, getY() - 1, width + 2, height + 2, COLOR_LIST_BORDER);

            guiGraphics.enableScissor(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1);

            int currentY = getY() - (int) scrollAmount + 4;
            for (Entry entry : entries) {
                if (currentY + entry.getHeight() > getY() && currentY < getY() + height) {
                    entry.render(guiGraphics, getX(), currentY, mouseX, mouseY, partialTick);
                }
                currentY += entry.getHeight();
            }

            guiGraphics.disableScissor();

            int contentHeight = getContentHeight();
            if (contentHeight > height) {
                int scrollbarWidth = 4;
                int scrollbarX = getX() + width - scrollbarWidth - 4;
                int barHeight = Math.max(20, (int) ((float) height / contentHeight * height));
                int barY = getY() + (int) ((float) scrollAmount / contentHeight * height);

                int color = isDraggingScrollbar ? COLOR_PINK_ACCENT : COLOR_PINK_SOFT;
                guiGraphics.fill(scrollbarX, barY, scrollbarX + scrollbarWidth, barY + barHeight, color | 0xFF000000);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            // 1. 滚动条逻辑
            int contentHeight = getContentHeight();
            if (contentHeight > height) {
                int scrollbarX = getX() + width - 10;
                if (mouseX >= scrollbarX && mouseX <= getX() + width) {
                    isDraggingScrollbar = true;
                    return true;
                }
            }

            // 2. 内容逻辑 (关键修复：焦点管理)
            if (isMouseOver(mouseX, mouseY)) {
                double localY = mouseY - getY() + scrollAmount - 4;
                int currentY = 0;

                // 先清除所有子项的焦点，防止多个输入框同时闪烁
                for (Entry entry : entries) {
                    entry.setFocused(false);
                }

                for (Entry entry : entries) {
                    if (localY >= currentY && localY < currentY + entry.getHeight()) {
                        // 转换坐标并传递点击
                        if (entry.mouseClicked(mouseX, mouseY, button)) {
                            // 如果子项处理了点击（例如输入框被点了），将焦点设给它
                            this.setFocused(entry);
                            entry.setFocused(true);
                            return true;
                        }
                    }
                    currentY += entry.getHeight();
                }
            }

            // 如果点到了列表背景空白处，清除焦点
            this.setFocused(null);
            return false; // 注意：这里返回 false 允许父 Screen 处理清除焦点
        }

        // 必须实现：将键盘事件转发给当前 Focused 的 Entry
        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            GuiEventListener focused = getFocused();
            if (focused != null && focused.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char codePoint, int modifiers) {
            GuiEventListener focused = getFocused();
            if (focused != null && focused.charTyped(codePoint, modifiers)) {
                return true;
            }
            return super.charTyped(codePoint, modifiers);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            isDraggingScrollbar = false;
            return super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (isDraggingScrollbar && getContentHeight() > height) {
                double ratio = (double) getContentHeight() / height;
                setScrollAmount(scrollAmount + dragY * ratio);
                return true;
            }
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            if (getContentHeight() > height) {
                setScrollAmount(scrollAmount - scrollY * 20);
                return true;
            }
            return false;
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            // 这里返回空或者仅返回 entries 并不足以自动处理焦点，因为我们重写了逻辑
            // 但为了兼容性，返回所有子项的 children
            List<GuiEventListener> all = new ArrayList<>();
            for(Entry e : entries) all.addAll(e.children());
            return all;
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {}
    }

    // ================== Entry 组件 ==================

    private abstract static class Entry implements GuiEventListener {
        public abstract void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY, float partialTick);
        public abstract int getHeight();
        public abstract List<? extends GuiEventListener> children();

        // 用于手动管理焦点状态
        public abstract void setFocused(boolean focused);
    }

    private static class GroupHeaderEntry extends Entry {
        private final ConfigNode node;
        private final int indent;
        private final Runnable onToggle;
        private final int width;

        public GroupHeaderEntry(ConfigNode node, int indent, int width, Runnable onToggle) {
            this.node = node;
            this.indent = indent;
            this.width = width;
            this.onToggle = onToggle;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY, float partialTick) {
            String arrow = node.collapsed ? "▶ " : "▼ ";
            MutableComponent text = Component.literal(arrow).withStyle(ChatFormatting.LIGHT_PURPLE)
                    .append(Component.translatable("screen.toneko.config.group." + node.name).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

            if (mouseY >= y && mouseY < y + getHeight() && mouseX >= x && mouseX <= x + width) {
                guiGraphics.fill(x + 2, y, x + width - 2, y + getHeight(), 0x15FFFFFF);
            }

            guiGraphics.drawString(Minecraft.getInstance().font, text, x + 5 + indent, y + 6, 0xFFFFFF, false);
            guiGraphics.hLine(x + 10, x + width - 10, y + getHeight() - 1, 0x40FF69B4);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            node.collapsed = !node.collapsed;
            onToggle.run();
            Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }

        @Override public boolean isFocused() { return false; }
        @Override public void setFocused(boolean focused) {}
        @Override public int getHeight() { return 24; }
        @Override public List<? extends GuiEventListener> children() { return List.of(); }
    }

    private static class ConfigSettingEntry extends Entry {
        private final ConfigNode node;
        private final int indent;
        private final int rowWidth;
        private final List<AbstractWidget> widgets = new ArrayList<>();
        private final AbstractWidget inputWidget;
        private final String url;
        private int lastRenderX, lastRenderY;

        public ConfigSettingEntry(ConfigNode node, int indent, int rowWidth) {
            this.node = node;
            this.indent = indent;
            this.rowWidth = rowWidth;
            this.url = node.entry.url();

            int inputWidth = 140;

            if (node.entry.type() == ConfigBuilder.Entry.Types.BOOLEAN) {
                boolean currentVal = ConfigUtil.CONFIG.getBoolean(node.fullKey);
                inputWidget = Button.builder(getBoolText(currentVal), btn -> {
                    boolean newVal = !ConfigUtil.CONFIG.getBoolean(node.fullKey);
                    ConfigUtil.CONFIG.set(node.fullKey, newVal);
                    btn.setMessage(getBoolText(newVal));
                }).bounds(0, 0, inputWidth, 20).build();
            } else {
                EditBox editBox = new EditBox(Minecraft.getInstance().font, 0, 0, inputWidth, 20, Component.nullToEmpty(node.fullKey));
                editBox.setMaxLength(1000);
                editBox.setValue(ConfigUtil.CONFIG.getString(node.fullKey));
                editBox.setResponder(text -> ConfigUtil.CONFIG.set(node.fullKey, text));
                editBox.setBordered(true);
                // 默认颜色修正
                editBox.setTextColor(0xFFFFFF);
                inputWidget = editBox;
            }
            widgets.add(inputWidget);
        }

        private Component getBoolText(boolean val) {
            return val ? Component.literal("✅ ").append(Component.translatable("screen.toneko.config.button.true")).withStyle(ChatFormatting.GREEN)
                    : Component.literal("❌ ").append(Component.translatable("screen.toneko.config.button.false")).withStyle(ChatFormatting.RED);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY, float partialTick) {
            this.lastRenderX = x;
            this.lastRenderY = y;

            Component label = Component.translatable("screen.toneko.config.key." + node.fullKey);
            int labelColor = (url != null && isMouseOverLabel(mouseX, mouseY)) ? COLOR_PINK_ACCENT : 0xE0E0E0;

            guiGraphics.drawString(Minecraft.getInstance().font, label, x + 10 + indent, y + 6, labelColor, false);

            int inputX = x + rowWidth - inputWidget.getWidth() - 10;
            inputWidget.setX(inputX);
            inputWidget.setY(y);
            inputWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        private boolean isMouseOverLabel(double mouseX, double mouseY) {
            if (url == null) return false;
            int width = Minecraft.getInstance().font.width(Component.translatable("screen.toneko.config.key." + node.fullKey));
            return mouseX >= lastRenderX + 10 + indent && mouseX <= lastRenderX + 10 + indent + width
                    && mouseY >= lastRenderY && mouseY <= lastRenderY + 20;
        }

        // 核心修复：点击时显式设置输入框焦点
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            // 1. 检查是否点击了链接
            if (url != null && isMouseOverLabel(mouseX, mouseY)) {
                ConfirmLinkScreen.confirmLinkNow(Minecraft.getInstance().screen, url);
                return true;
            }

            // 2. 转发给输入控件 (EditBox 或 Button)
            if (inputWidget.mouseClicked(mouseX, mouseY, button)) {
                // 如果是输入框，强制设为焦点
                if (inputWidget instanceof EditBox) {
                    ((EditBox) inputWidget).setFocused(true);
                }
                return true;
            } else {
                // 如果点到了 Entry 的其他区域，取消输入框的焦点
                if (inputWidget instanceof EditBox) {
                    ((EditBox) inputWidget).setFocused(false);
                }
            }
            return false;
        }

        // 核心修复：手动管理焦点状态
        @Override
        public void setFocused(boolean focused) {
            if (inputWidget instanceof EditBox) {
                ((EditBox) inputWidget).setFocused(focused);
            }
        }

        @Override
        public boolean isFocused() {
            return inputWidget.isFocused();
        }

        // 键盘事件转发
        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return inputWidget.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char codePoint, int modifiers) {
            return inputWidget.charTyped(codePoint, modifiers);
        }

        @Override public int getHeight() { return 26; }
        @Override public List<? extends GuiEventListener> children() { return widgets; }
    }

    private static class RandomTextEntry extends Entry {
        private final int width;
        private final Button randomBtn;
        private Component currentText;
        private static final int MAX_RANDOM = 29;

        public RandomTextEntry(int width) {
            this.width = width;
            updateText();
            this.randomBtn = Button.builder(Component.translatable("screen.toneko.config.button.random"), btn -> updateText())
                    .bounds(0, 0, 80, 20).build();
        }

        private void updateText() {
            int r = new Random().nextInt(MAX_RANDOM);
            currentText = Component.translatable("screen.toneko.config.random." + r)
                    .withStyle(ChatFormatting.ITALIC, ChatFormatting.LIGHT_PURPLE);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY, float partialTick) {
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, currentText, x + width / 2, y + 8, 0xFFFFFF);
            randomBtn.setX(x + (width - randomBtn.getWidth()) / 2);
            randomBtn.setY(y + 25);
            randomBtn.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override public boolean mouseClicked(double mouseX, double mouseY, int button) { return randomBtn.mouseClicked(mouseX, mouseY, button); }
        @Override public void setFocused(boolean focused) { randomBtn.setFocused(focused); }
        @Override public boolean isFocused() { return randomBtn.isFocused(); }
        @Override public int getHeight() { return 55; }
        @Override public List<? extends GuiEventListener> children() { return List.of(randomBtn); }
    }
}