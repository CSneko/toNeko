package org.cneko.toneko.common.mod.client.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.cneko.toneko.common.mod.ai.proactive.NekoProactiveManager;
import org.cneko.toneko.common.mod.ai.proactive.NekoProactiveManager.NekoProactiveTrigger;
import org.cneko.toneko.common.mod.ai.provider.AIServiceProvider;
import org.cneko.toneko.common.mod.ai.provider.AIServiceProviderRegistry;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.TTSUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Scrollable AI configuration screen covering ALL ai.* config keys.
 * 其余配置内容在ConfigScreen
 */
public class AIConfigScreen extends Screen {
    private final Screen lastScreen;
    private final List<AIServiceProvider> providers;
    private String currentProviderId;
    private int providerIndex;
    private AIConfigListWidget list;

    private static final int COLOR_ACCENT = 0xFFFF69B4;
    private static final int COLOR_SECTION = 0xFFFFB6C1;
    private static final int COLOR_LIST_BG = 0x60000000;

    public AIConfigScreen(Screen lastScreen) {
        super(Component.translatable("screen.toneko.ai_config.title"));
        this.lastScreen = lastScreen;
        this.providers = new ArrayList<>(AIServiceProviderRegistry.getAll());
        this.currentProviderId = ConfigUtil.getAIService();
        this.providerIndex = 0;
        for (int i = 0; i < providers.size(); i++) {
            if (providers.get(i).getProviderId().equalsIgnoreCase(currentProviderId)) {
                providerIndex = i; break;
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        int lw = (int)(width * 0.78);
        int lh = (int)(height * 0.72);
        int lx = (width - lw) / 2;
        int ly = (int)(height * 0.13);

        list = new AIConfigListWidget(lx, ly, lw, lh);
        addRenderableWidget(list);
        buildEntries();

        int btnY = ly + lh + 8;
        int cx = width / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(cx - 105, btnY, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.toneko.config.button.apply"), b -> apply())
                .bounds(cx + 5, btnY, 100, 20).build());
    }

    private void buildEntries() {
        list.clearEntries();
        int leftX = (width - list.getWidth()) / 2 + 8;
        int rowW = list.getWidth() - 16;

        // --- Enable AI ---
        list.addEntry(new BoolEntry(leftX, rowW, "ai.enable",
                Component.translatable("screen.toneko.config.key.ai.enable")));

        // --- Section: Provider ---
        list.addEntry(new SectionEntry(leftX, rowW, "screen.toneko.ai_config.section.provider"));

        // Provider switcher
        if (!providers.isEmpty()) {
            list.addEntry(new ProviderEntry(leftX, rowW, this));
        }

        // One-click switch to Player2 (auto-enables AI + TTS)
        list.addEntry(new Player2QuickEntry(leftX, rowW, this));

        // API Key
        list.addEntry(new StringEntry(leftX, rowW, "ai.key",
                Component.translatable("screen.toneko.ai_config.api_key"), ConfigUtil.getAIKey(), 256));

        // Model
        list.addEntry(new StringEntry(leftX, rowW, "ai.model",
                Component.translatable("screen.toneko.ai_config.model"), ConfigUtil.getAIModel(), 128));

        // Base URL
        list.addEntry(new StringEntry(leftX, rowW, "ai.base_url",
                Component.translatable("screen.toneko.ai_config.base_url"), ConfigUtil.getAIBaseUrl(), 256));

        // --- Section: Common ---
        list.addEntry(new SectionEntry(leftX, rowW, "screen.toneko.ai_config.section.common"));

        // Prompt (multiline)
        list.addEntry(new MultilineEntry(leftX, rowW, "ai.prompt",
                Component.translatable("screen.toneko.config.key.ai.prompt"), ConfigUtil.getAIPrompt(), 10000, 96));

        // Show think
        list.addEntry(new BoolEntry(leftX, rowW, "ai.show_think",
                Component.translatable("screen.toneko.config.key.ai.show_think")));

        // Chat prefix
        list.addEntry(new StringEntry(leftX, rowW, "ai.chat_prefix",
                Component.translatable("screen.toneko.config.key.ai.chat_prefix"), ConfigUtil.getAIChatPrefix(), 16));

        // Debug logging
        list.addEntry(new BoolEntry(leftX, rowW, "ai.debug",
                Component.translatable("screen.toneko.config.key.ai.debug")));

        // Max history
        list.addEntry(new StringEntry(leftX, rowW, "ai.max_history",
                Component.translatable("screen.toneko.config.key.ai.max_history"),
                ConfigUtil.CONFIG.getString("ai.max_history"), 8));

        // Cooldown
        list.addEntry(new StringEntry(leftX, rowW, "ai.cooldown",
                Component.translatable("screen.toneko.config.key.ai.cooldown"),
                ConfigUtil.CONFIG.getString("ai.cooldown"), 8));

        // Max concurrent nekos (area chat)
        list.addEntry(new StringEntry(leftX, rowW, "ai.max_concurrent_neko",
                Component.translatable("screen.toneko.config.key.ai.max_concurrent_neko"),
                ConfigUtil.CONFIG.getString("ai.max_concurrent_neko"), 8));

        // --- Section: Summary ---
        list.addEntry(new SectionEntry(leftX, rowW, "screen.toneko.ai_config.section.summary"));
        list.addEntry(new BoolEntry(leftX, rowW, "ai.summary.enable",
                Component.translatable("screen.toneko.config.key.ai.summary.enable")));
        list.addEntry(new StringEntry(leftX, rowW, "ai.summary.count",
                Component.translatable("screen.toneko.config.key.ai.summary.count"),
                ConfigUtil.CONFIG.getString("ai.summary.count"), 8));

        // --- Section: Actions ---
        list.addEntry(new SectionEntry(leftX, rowW, "screen.toneko.ai_config.section.actions"));
        list.addEntry(new BoolEntry(leftX, rowW, "ai.actions.enable",
                Component.translatable("screen.toneko.config.key.ai.actions.enable")));
        list.addEntry(new BoolEntry(leftX, rowW, "ai.actions.virtual_items",
                Component.translatable("screen.toneko.config.key.ai.actions.virtual_items")));
        list.addEntry(new StringEntry(leftX, rowW, "ai.actions.energy_cost",
                Component.translatable("screen.toneko.config.key.ai.actions.energy_cost"),
                ConfigUtil.CONFIG.getString("ai.actions.energy_cost"), 8));
        list.addEntry(new StringEntry(leftX, rowW, "ai.actions.diary.cooldown",
                Component.translatable("screen.toneko.config.key.ai.actions.diary.cooldown"),
                ConfigUtil.CONFIG.getString("ai.actions.diary.cooldown"), 8));
        list.addEntry(new StringEntry(leftX, rowW, "ai.actions.diary.max_entries",
                Component.translatable("screen.toneko.config.key.ai.actions.diary.max_entries"),
                ConfigUtil.CONFIG.getString("ai.actions.diary.max_entries"), 8));

        // --- Section: Proactive ---
        list.addEntry(new SectionEntry(leftX, rowW, "screen.toneko.ai_config.section.proactive"));
        list.addEntry(new BoolEntry(leftX, rowW, "ai.proactive.enable",
                Component.translatable("screen.toneko.config.key.ai.proactive.enable")));
        list.addEntry(new StringEntry(leftX, rowW, "ai.proactive.interval",
                Component.translatable("screen.toneko.config.key.ai.proactive.interval"),
                ConfigUtil.CONFIG.getString("ai.proactive.interval"), 8));
        // 已注册触发器的概率配置（动态生成，滑动条 0~1：0=禁用，1=必定触发）
        for (NekoProactiveTrigger trigger : NekoProactiveManager.getAll()) {
            list.addEntry(new SliderEntry(leftX, rowW, NekoProactiveManager.chanceKey(trigger.getId()),
                    Component.translatable("screen.toneko.config.key.ai.proactive.trigger." + trigger.getId())));
        }

        // --- Section: Neko Talk ---
        list.addEntry(new SectionEntry(leftX, rowW, "screen.toneko.ai_config.section.nekotalk"));
        list.addEntry(new BoolEntry(leftX, rowW, "ai.nekotalk.enable",
                Component.translatable("screen.toneko.config.key.ai.nekotalk.enable")));
        list.addEntry(new StringEntry(leftX, rowW, "ai.nekotalk.rounds",
                Component.translatable("screen.toneko.config.key.ai.nekotalk.rounds"),
                ConfigUtil.CONFIG.getString("ai.nekotalk.rounds"), 8));
        list.addEntry(new StringEntry(leftX, rowW, "ai.nekotalk.interval",
                Component.translatable("screen.toneko.config.key.ai.nekotalk.interval"),
                ConfigUtil.CONFIG.getString("ai.nekotalk.interval"), 8));

        // --- Section: Trigger Reactions ---
        list.addEntry(new SectionEntry(leftX, rowW, "screen.toneko.ai_config.section.trigger"));
        list.addEntry(new BoolEntry(leftX, rowW, "ai.trigger.enable",
                Component.translatable("screen.toneko.config.key.ai.trigger.enable")));
        list.addEntry(new SliderEntry(leftX, rowW, "ai.trigger.pet.chance",
                Component.translatable("screen.toneko.config.key.ai.trigger.pet.chance")));
        list.addEntry(new SliderEntry(leftX, rowW, "ai.trigger.hurt.chance",
                Component.translatable("screen.toneko.config.key.ai.trigger.hurt.chance")));

        // --- Section: TTS ---
        list.addEntry(new SectionEntry(leftX, rowW, "screen.toneko.ai_config.section.tts"));
        list.addEntry(new BoolEntry(leftX, rowW, "ai.tts.enable",
                Component.translatable("screen.toneko.config.key.ai.tts.enable")));
        list.addEntry(new StringEntry(leftX, rowW, "ai.tts.service",
                Component.translatable("screen.toneko.config.key.ai.tts.service"), ConfigUtil.CONFIG.getString("ai.tts.service"), 64));
        list.addEntry(new StringEntry(leftX, rowW, "ai.tts.port",
                Component.translatable("screen.toneko.config.key.ai.tts.port"), ConfigUtil.getAITTSPort(), 8));
        list.addEntry(new VoiceEntry(leftX, rowW));

        // --- Section: Proxy ---
        list.addEntry(new SectionEntry(leftX, rowW, "screen.toneko.ai_config.section.proxy"));
        list.addEntry(new BoolEntry(leftX, rowW, "ai.proxy.enable",
                Component.translatable("screen.toneko.config.key.ai.proxy.enable")));
        list.addEntry(new StringEntry(leftX, rowW, "ai.proxy.ip",
                Component.translatable("screen.toneko.config.key.ai.proxy.ip"), ConfigUtil.getAIProxyIp(), 64));
        list.addEntry(new StringEntry(leftX, rowW, "ai.proxy.port",
                Component.translatable("screen.toneko.config.key.ai.proxy.port"), ConfigUtil.getAIProxyPort(), 8));

    }

    private void switchProvider() {
        ConfigUtil.saveProviderConfig(currentProviderId);
        providerIndex = (providerIndex + 1) % providers.size();
        currentProviderId = providers.get(providerIndex).getProviderId();
        ConfigUtil.CONFIG.set("ai.service", currentProviderId);
        ConfigUtil.loadProviderConfig(currentProviderId);
        rebuildWidgets();
    }

    /**
     * 一键切换到 Player2：启用 AI + 切换服务商 + 启用 TTS + 模型留空（Player2 App 内自选），
     * 立即保存生效（模仿 apply()，但不关闭屏幕）。
     */
    private void switchToPlayer2() {
        // 保存当前 provider 的配置
        ConfigUtil.saveProviderConfig(currentProviderId);
        // 定位 Player2 在服务商列表中的位置（界面状态同步）
        for (int i = 0; i < providers.size(); i++) {
            if (providers.get(i).getProviderId().equalsIgnoreCase("player2")) {
                providerIndex = i;
                break;
            }
        }
        currentProviderId = "player2";
        ConfigUtil.CONFIG.set("ai.service", "player2");
        ConfigUtil.CONFIG.set("ai.enable", true);
        ConfigUtil.CONFIG.set("ai.tts.enable", true);
        ConfigUtil.CONFIG.set("ai.tts.service", "player2");
        // Player2 使用用户自选的默认模型，模型留空
        ConfigUtil.CONFIG.set("ai.model", "");
        ConfigUtil.loadProviderConfig("player2");
        ConfigUtil.CONFIG.save();
        ConfigUtil.load();
        rebuildWidgets();
    }

    private void apply() {
        ConfigUtil.saveProviderConfig(currentProviderId);
        ConfigUtil.CONFIG.set("ai.service", currentProviderId);
        ConfigUtil.CONFIG.save();
        ConfigUtil.load();
        onClose();
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(lastScreen);
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g, mx, my, pt);
        g.drawCenteredString(font, getTitle(), width / 2, 12, COLOR_ACCENT);
        // Provider indicator
        if (!providers.isEmpty()) {
            AIServiceProvider p = providers.get(providerIndex);
            String model = p.getDefaultModel();
            String label = model == null || model.isEmpty()
                    ? p.getDisplayName()
                    : p.getDisplayName() + " (" + model + ")";
            g.drawCenteredString(font, Component.literal(label),
                    width / 2, 24, 0xFFC0C0C0);
        }
        // Scroll hint
        g.drawCenteredString(font, Component.literal("▼ scroll ▼"),
                width / 2, height - 8, 0x40FFFFFF);
        super.render(g, mx, my, pt);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xC0100510);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ======================== List Widget ========================

    static class AIConfigListWidget extends AbstractContainerWidget {
        private final List<RowEntry> entries = new ArrayList<>();
        private double scrollAmount;
        private boolean dragging;
        private final int w, h;

        AIConfigListWidget(int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty());
            this.w = w; this.h = h;
        }

        void addEntry(RowEntry e) { entries.add(e); }
        void clearEntries() { entries.clear(); }
        int contentHeight() { return entries.stream().mapToInt(RowEntry::getHeight).sum(); }

        @Override
        public void renderWidget(@NotNull GuiGraphics g, int mx, int my, float pt) {
            g.fill(getX(), getY(), getX() + w, getY() + h, COLOR_LIST_BG);
            g.renderOutline(getX() - 1, getY() - 1, w + 2, h + 2, COLOR_ACCENT);
            g.enableScissor(getX() + 1, getY() + 1, getX() + w - 1, getY() + h - 1);
            int cy = getY() - (int) scrollAmount + 2;
            for (RowEntry e : entries) {
                if (cy + e.getHeight() > getY() && cy < getY() + h) {
                    e.render(g, getX(), cy, getX() + w, mx, my, pt);
                }
                cy += e.getHeight();
            }
            g.disableScissor();
            // Scrollbar
            int ch = contentHeight();
            if (ch > h) {
                int sw = 4, sx = getX() + w - sw - 2;
                int bh = Math.max(16, (int)((float)h / ch * h));
                int by = getY() + (int)(scrollAmount / ch * h);
                g.fill(sx, by, sx + sw, by + bh, (dragging ? 0xFFFF69B4 : 0xFFFFB6C1) | 0xFF000000);
            }
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double dx, double dy) {
            scrollAmount = Mth.clamp(scrollAmount - dy * 12, 0, Math.max(0, contentHeight() - h));
            return true;
        }

        @Override
        public boolean mouseClicked(double mx, double my, int btn) {
            if (!isMouseOver(mx, my)) return false;
            // Scrollbar drag
            int ch = contentHeight();
            if (ch > h && mx >= getX() + w - 10) { dragging = true; return true; }
            // Clear all entry focus
            for (RowEntry e : entries) e.setFocused(false);
            // Delegate to clicked entry
            double ly = my - getY() + scrollAmount - 2;
            int cy = 0;
            for (RowEntry e : entries) {
                if (ly >= cy && ly < cy + e.getHeight()) {
                    e.mouseClicked(mx, my, btn);
                    break;
                }
                cy += e.getHeight();
            }
            return true;
        }

        @Override
        public boolean mouseReleased(double mx, double my, int btn) { dragging = false; return super.mouseReleased(mx, my, btn); }
        @Override
        public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
            if (dragging) {
                int ch = contentHeight();
                if (ch > h) scrollAmount = Mth.clamp(my / h * ch, 0, ch - h);
                return true;
            }
            // 转发给聚焦的条目（如滑动条拖动）
            for (RowEntry e : entries) {
                if (e.isFocused() && e.mouseDragged(mx, my, btn, dx, dy)) return true;
            }
            return super.mouseDragged(mx, my, btn, dx, dy);
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() { return List.of(); }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            for (RowEntry e : entries) {
                if (e.isFocused() && e.keyPressed(keyCode, scanCode, modifiers)) return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char codePoint, int modifiers) {
            for (RowEntry e : entries) {
                if (e.isFocused() && e.charTyped(codePoint, modifiers)) return true;
            }
            return super.charTyped(codePoint, modifiers);
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput out) {}
    }

    // ======================== Row Entries ========================

    interface RowEntry {
        int getHeight();
        void render(GuiGraphics g, int lx, int ly, int rx, int mx, int my, float pt);
        default void mouseClicked(double mx, double my, int btn) {}
        default boolean mouseDragged(double mx, double my, int btn, double dx, double dy) { return false; }
        default boolean isFocused() { return false; }
        default void setFocused(boolean f) {}
        default boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }
        default boolean charTyped(char codePoint, int modifiers) { return false; }
    }

    /** Section header row */
    static class SectionEntry implements RowEntry {
        private final String key;
        private final int lx, rw;
        SectionEntry(int lx, int rw, String key) { this.lx = lx; this.rw = rw; this.key = key; }
        @Override public int getHeight() { return 24; }
        @Override
        public void render(GuiGraphics g, int lx, int ly, int rx, int mx, int my, float pt) {
            int cx = (lx + rx) / 2;
            g.hLine(cx - 200, cx + 200, ly + 6, COLOR_SECTION);
            g.drawCenteredString(Minecraft.getInstance().font, Component.translatable(key), cx, ly + 8, COLOR_SECTION);
        }
    }

    /** Boolean toggle row */
    static class BoolEntry implements RowEntry {
        private final String configKey;
        private final Component label;
        private boolean val;
        private final int lx, rw;
        BoolEntry(int lx, int rw, String configKey, Component label) {
            this.lx = lx; this.rw = rw; this.configKey = configKey; this.label = label;
            this.val = ConfigUtil.CONFIG.getBoolean(configKey);
        }
        @Override public int getHeight() { return 22; }
        @Override
        public void render(GuiGraphics g, int lx, int ly, int rx, int mx, int my, float pt) {
            boolean hover = mx >= lx && mx < rx && my >= ly && my < ly + getHeight();
            g.fill(lx, ly, rx, ly + getHeight(), hover ? 0x15FFFFFF : 0);
            String s = val ? "§a✔ " : "§c✘ ";
            g.drawString(Minecraft.getInstance().font, s + label.getString(), lx + 4, ly + 5, 0xFFFFFF);
        }
        @Override
        public void mouseClicked(double mx, double my, int btn) {
            val = !val;
            ConfigUtil.CONFIG.set(configKey, val);
        }
    }

    /** String input row */
    static class StringEntry implements RowEntry {
        private final String configKey;
        private final Component label;
        private final EditBox edit;
        private final int lx, rw;
        private boolean focused;
        StringEntry(int lx, int rw, String configKey, Component label, String value, int maxLen) {
            this.lx = lx; this.rw = rw; this.configKey = configKey; this.label = label;
            this.edit = new EditBox(Minecraft.getInstance().font, 0, 0, rw - 6, 16, Component.empty());
            edit.setMaxLength(maxLen);
            edit.setValue(value != null ? value : "");
            edit.setResponder(t -> ConfigUtil.CONFIG.set(configKey, t.trim()));
        }
        StringEntry(int lx, int rw, String configKey, Component label, String value, int maxLen, int height) {
            this(lx, rw, configKey, label, value, maxLen);
        }
        @Override public int getHeight() { return 40; }
        @Override public boolean isFocused() { return focused; }
        @Override public void setFocused(boolean f) { this.focused = f; edit.setFocused(f); }
        @Override
        public void render(GuiGraphics g, int lx, int ly, int rx, int mx, int my, float pt) {
            boolean hover = mx >= lx && mx < rx && my >= ly && my < ly + getHeight();
            g.fill(lx, ly, rx, ly + getHeight(), hover ? 0x15FFFFFF : 0);
            g.drawString(Minecraft.getInstance().font, label, lx + 4, ly + 4, COLOR_ACCENT);
            edit.setX(lx + 4);
            edit.setY(ly + 16);
            edit.render(g, mx, my, pt);
        }
        @Override
        public void mouseClicked(double mx, double my, int btn) {
            boolean hit = mx >= lx + 4 && mx <= lx + 4 + rw - 6
                    && my >= edit.getY() && my <= edit.getY() + 16;
            focused = hit;
            edit.setFocused(hit);
            if (hit) edit.mouseClicked(mx, my, btn);
        }
        @Override
        public boolean keyPressed(int kc, int sc, int md) { return focused && edit.keyPressed(kc, sc, md); }
        @Override
        public boolean charTyped(char cp, int md) { return focused && edit.charTyped(cp, md); }
    }

    /** 多行文本输入行（用于 prompt 等长文本编辑） */
    static class MultilineEntry implements RowEntry {
        private final String configKey;
        private final Component label;
        private final MultiLineEditBox edit;
        private final int lx, rw, height;
        private boolean focused;
        MultilineEntry(int lx, int rw, String configKey, Component label, String value, int maxLen, int height) {
            this.lx = lx; this.rw = rw; this.configKey = configKey; this.label = label;
            this.height = height;
            this.edit = new MultiLineEditBox(Minecraft.getInstance().font, 0, 0, rw - 6, height - 18,
                    Component.empty(), Component.empty());
            edit.setCharacterLimit(maxLen);
            edit.setValue(value != null ? value : "");
            edit.setValueListener(t -> ConfigUtil.CONFIG.set(configKey, t));
        }
        @Override public int getHeight() { return height; }
        @Override public boolean isFocused() { return focused; }
        @Override public void setFocused(boolean f) { this.focused = f; edit.setFocused(f); }
        @Override
        public void render(GuiGraphics g, int lx, int ly, int rx, int mx, int my, float pt) {
            boolean hover = mx >= lx && mx < rx && my >= ly && my < ly + getHeight();
            g.fill(lx, ly, rx, ly + getHeight(), hover ? 0x15FFFFFF : 0);
            g.drawString(Minecraft.getInstance().font, label, lx + 4, ly + 4, COLOR_ACCENT);
            edit.setX(lx + 4);
            edit.setY(ly + 16);
            edit.render(g, mx, my, pt);
        }
        @Override
        public void mouseClicked(double mx, double my, int btn) {
            boolean hit = mx >= lx + 4 && mx <= lx + 4 + rw - 6
                    && my >= edit.getY() && my <= edit.getY() + edit.getHeight();
            focused = hit;
            edit.setFocused(hit);
            if (hit) edit.mouseClicked(mx, my, btn);
        }
        @Override
        public boolean keyPressed(int kc, int sc, int md) { return focused && edit.keyPressed(kc, sc, md); }
        @Override
        public boolean charTyped(char cp, int md) { return focused && edit.charTyped(cp, md); }
    }

    /** 0~1 概率滑动条行（触发器概率配置） */
    static class SliderEntry implements RowEntry {
        private final String configKey;
        private final Component label;
        private final ProbabilitySlider slider;
        private final int lx, rw;
        private boolean focused;
        SliderEntry(int lx, int rw, String configKey, Component label) {
            this.lx = lx; this.rw = rw; this.configKey = configKey; this.label = label;
            float value = ConfigUtil.CONFIG.getFloat(configKey);
            this.slider = new ProbabilitySlider(configKey, 0, 0, rw - 6, 16, value);
        }
        @Override public int getHeight() { return 30; }
        @Override public boolean isFocused() { return focused; }
        @Override public void setFocused(boolean f) { this.focused = f; }
        @Override
        public void render(GuiGraphics g, int lx, int ly, int rx, int mx, int my, float pt) {
            boolean hover = mx >= lx && mx < rx && my >= ly && my < ly + getHeight();
            g.fill(lx, ly, rx, ly + getHeight(), hover ? 0x15FFFFFF : 0);
            g.drawString(Minecraft.getInstance().font, label, lx + 4, ly + 3, COLOR_ACCENT);
            slider.setX(lx + 4);
            slider.setY(ly + 14);
            slider.render(g, mx, my, pt);
        }
        @Override
        public void mouseClicked(double mx, double my, int btn) {
            focused = true;
            slider.mouseClicked(mx, my, btn);
        }
        @Override
        public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
            return focused && slider.mouseDragged(mx, my, btn, dx, dy);
        }
    }

    /** 概率滑动条：0~1，实时写入配置，显示百分比 */
    static class ProbabilitySlider extends AbstractSliderButton {
        private final String configKey;
        ProbabilitySlider(String configKey, int x, int y, int width, int height, float value) {
            super(x, y, width, height, Component.empty(), Math.max(0.0, Math.min(1.0, value)));
            this.configKey = configKey;
            updateMessage();
        }
        @Override
        protected void updateMessage() {
            float v = (float) this.value;
            String text = v <= 0.005f
                    ? "0% " + net.minecraft.client.resources.language.I18n.get("screen.toneko.ai_config.slider.disabled")
                    : String.format("%.0f%%", v * 100);
            setMessage(Component.literal(text));
        }
        @Override
        protected void applyValue() {
            ConfigUtil.CONFIG.set(configKey, (float) this.value);
        }
    }

    /** Provider switcher row */
    class ProviderEntry implements RowEntry {
        private final int lx, rw;
        ProviderEntry(int lx, int rw, AIConfigScreen screen) { this.lx = lx; this.rw = rw; }
        @Override public int getHeight() { return 28; }
        @Override
        public void render(GuiGraphics g, int lx, int ly, int rx, int mx, int my, float pt) {
            boolean hover = mx >= lx && mx < rx && my >= ly && my < ly + getHeight();
            g.fill(lx, ly, rx, ly + getHeight(), hover ? 0x15FFFFFF : 0);
            String name = providers.isEmpty() ? "N/A" : providers.get(providerIndex).getDisplayName();
            String txt = "◀  " + name + "  ▶";
            g.drawCenteredString(Minecraft.getInstance().font, Component.literal(txt),
                    (lx + rx) / 2, ly + 8, 0xFFFFFF);
        }
        @Override
        public void mouseClicked(double mx, double my, int btn) {
            switchProvider();
        }
    }

    /**
     * 一键切换到 Player2 的快捷按钮行（启用 AI + 切服务商 + 启用 TTS）。
     */
    static class Player2QuickEntry implements RowEntry {
        private final int lx, rw;
        private final AIConfigScreen screen;
        Player2QuickEntry(int lx, int rw, AIConfigScreen screen) { this.lx = lx; this.rw = rw; this.screen = screen; }
        @Override public int getHeight() { return 28; }
        @Override
        public void render(GuiGraphics g, int lx, int ly, int rx, int mx, int my, float pt) {
            boolean hover = mx >= lx && mx < rx && my >= ly && my < ly + getHeight();
            g.fill(lx, ly, rx, ly + getHeight(), hover ? 0x25FF69B4 : 0x10FFFFFF);
            String txt = "▶  " + net.minecraft.client.resources.language.I18n.get("screen.toneko.ai_config.use_player2") + "  ◀";
            g.drawCenteredString(Minecraft.getInstance().font, Component.literal(txt),
                    (lx + rx) / 2, ly + 8, 0xFF69B4);
        }
        @Override
        public void mouseClicked(double mx, double my, int btn) {
            screen.switchToPlayer2();
        }
    }

    /**
     * Player2 声音选择行：◀ 声音: <名> ▶ [刷新]。
     * 声音列表异步从 /v1/tts/voices 加载（构造时自动加载，render 线程轮询 future.isDone 避免竞态）；
     * 整行可切换：左 ◀ 上一个、中间/右 ▶ 下一个，切换后立即写入并保存 ai.tts.voice。
     * 渲染与点击统一使用构造的 this.lx/this.rw（内容区），与列表边界的 8px 内边距无关。
     */
    static class VoiceEntry implements RowEntry {
        private final int lx, rw;
        private final List<TTSUtil.VoiceInfo> voices = new ArrayList<>();
        private int index = -1;
        private CompletableFuture<List<TTSUtil.VoiceInfo>> pending;
        private boolean failed;

        private static final int REFRESH_W = 40;
        private static final int NAV_W = 20;

        VoiceEntry(int lx, int rw) {
            this.lx = lx;
            this.rw = rw;
            // 打开配置屏即自动加载声音列表
            refresh();
        }

        @Override public int getHeight() { return 28; }

        private int right() { return lx + rw; }

        @Override
        public void render(GuiGraphics g, int lx, int ly, int rx, int mx, int my, float pt) {
            // 异步加载完成检查（仅 render 线程修改 voices / index，避免竞态）
            if (pending != null && pending.isDone()) {
                try {
                    List<TTSUtil.VoiceInfo> loaded = pending.get();
                    voices.clear();
                    voices.addAll(loaded);
                    index = -1;
                    String current = ConfigUtil.getAITTSVoice();
                    for (int i = 0; i < voices.size(); i++) {
                        if (voices.get(i).id().equals(current)) { index = i; break; }
                    }
                    if (index < 0 && !voices.isEmpty()) index = 0;
                    failed = false;
                } catch (Exception e) {
                    failed = true;
                    voices.clear();
                    index = -1;
                }
                pending = null;
            }

            int R = right();
            boolean hover = mx >= this.lx && mx < R && my >= ly && my < ly + getHeight();
            g.fill(this.lx, ly, R, ly + getHeight(), hover ? 0x15FFFFFF : 0);
            g.drawString(Minecraft.getInstance().font, Component.literal(
                    net.minecraft.client.resources.language.I18n.get("screen.toneko.ai_config.tts.voice") + ": "),
                    this.lx + 4, ly + 8, COLOR_ACCENT);

            // 中间显示区（◀ 名字 ▶，或加载/失败/空状态）
            String display;
            if (failed) {
                display = "§c" + net.minecraft.client.resources.language.I18n.get("screen.toneko.ai_config.tts.voice.failed");
            } else if (pending != null) {
                display = net.minecraft.client.resources.language.I18n.get("screen.toneko.ai_config.tts.voice.loading");
            } else if (voices.isEmpty() && index < 0) {
                String configured = ConfigUtil.getAITTSVoice();
                display = configured.isEmpty()
                        ? "§7" + net.minecraft.client.resources.language.I18n.get("screen.toneko.ai_config.tts.voice.empty")
                        : "◀  " + configured + "  ▶";
            } else if (index >= 0 && !voices.isEmpty()) {
                TTSUtil.VoiceInfo v = voices.get(index);
                String name = v.name() != null && !v.name().isEmpty() ? v.name() : v.id();
                display = "◀  §f" + name + "§f  ▶";
            } else {
                display = "§7" + net.minecraft.client.resources.language.I18n.get("screen.toneko.ai_config.tts.voice.empty");
            }
            int midX = this.lx + 4 + Minecraft.getInstance().font.width(
                    net.minecraft.client.resources.language.I18n.get("screen.toneko.ai_config.tts.voice") + ": ");
            g.drawString(Minecraft.getInstance().font, Component.literal(display), midX, ly + 8, 0xFFFFFF);

            // 刷新按钮
            int bx = R - REFRESH_W - 2;
            boolean btnHover = mx >= bx && mx <= R - 2 && my >= ly + 5 && my <= ly + getHeight() - 5;
            g.fill(bx, ly + 5, R - 2, ly + getHeight() - 5, btnHover ? 0x55FFFFFF : 0x33FFFFFF);
            g.drawCenteredString(Minecraft.getInstance().font,
                    Component.literal(net.minecraft.client.resources.language.I18n.get("screen.toneko.ai_config.tts.voice.refresh")),
                    (bx + R - 2) / 2, ly + 9, 0xFFFFFF);
        }

        @Override
        public void mouseClicked(double mx, double my, int btn) {
            int R = right();
            // 刷新按钮
            if (mx >= R - REFRESH_W - 2 && mx <= R - 2) {
                refresh();
                return;
            }
            // 列表未加载/为空时不切换（但点刷新可以重新加载）
            if (voices.isEmpty() || index < 0) return;
            // 整行可切换：左 ◀ 上一个，中间及右侧（含 ▶）下一个
            if (mx >= lx + 4 && mx < lx + 4 + NAV_W) {
                index = (index - 1 + voices.size()) % voices.size();
            } else {
                index = (index + 1) % voices.size();
            }
            writeConfig();
        }

        /** 触发异步加载（Player2 未运行时不阻塞 UI，失败显示重试提示） */
        private void refresh() {
            if (pending != null) return;
            pending = TTSUtil.fetchVoices();
        }

        /** 写入并立即保存音色配置（不依赖"应用"按钮，切换即时生效且重启不丢失） */
        private void writeConfig() {
            ConfigUtil.CONFIG.set("ai.tts.voice", voices.get(index).id());
            ConfigUtil.CONFIG.save();
        }
    }
}
