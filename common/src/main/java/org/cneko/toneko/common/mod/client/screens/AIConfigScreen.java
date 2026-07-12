package org.cneko.toneko.common.mod.client.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.cneko.toneko.common.mod.ai.provider.AIServiceProvider;
import org.cneko.toneko.common.mod.ai.provider.AIServiceProviderRegistry;
import org.cneko.toneko.common.util.ConfigUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

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
        list.addEntry(new StringEntry(leftX, rowW, "ai.prompt",
                Component.translatable("screen.toneko.config.key.ai.prompt"), ConfigUtil.getAIPrompt(), 2000));

        // Show think
        list.addEntry(new BoolEntry(leftX, rowW, "ai.show_think",
                Component.translatable("screen.toneko.config.key.ai.show_think")));

        // Chat prefix
        list.addEntry(new StringEntry(leftX, rowW, "ai.chat_prefix",
                Component.translatable("screen.toneko.config.key.ai.chat_prefix"), ConfigUtil.getAIChatPrefix(), 16));

        // Debug logging
        list.addEntry(new BoolEntry(leftX, rowW, "ai.debug",
                Component.translatable("screen.toneko.config.key.ai.debug")));

        // --- Section: TTS ---
        list.addEntry(new SectionEntry(leftX, rowW, "screen.toneko.ai_config.section.tts"));
        list.addEntry(new BoolEntry(leftX, rowW, "ai.tts.enable",
                Component.translatable("screen.toneko.config.key.ai.tts.enable")));
        list.addEntry(new StringEntry(leftX, rowW, "ai.tts.service",
                Component.translatable("screen.toneko.config.key.ai.tts.service"), ConfigUtil.CONFIG.getString("ai.tts.service"), 64));
        list.addEntry(new StringEntry(leftX, rowW, "ai.tts.voice",
                Component.translatable("screen.toneko.config.key.ai.tts.voice"), ConfigUtil.getAITTSVoice(), 64));

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
            g.drawCenteredString(font, Component.literal(p.getDisplayName() + " (" + p.getDefaultModel() + ")"),
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
}
