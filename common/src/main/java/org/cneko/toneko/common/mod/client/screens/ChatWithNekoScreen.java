package org.cneko.toneko.common.mod.client.screens;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.packets.interactives.ChatHistoryRequestPayload;
import org.cneko.toneko.common.mod.packets.interactives.ChatWithNekoPayload;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ChatWithNekoScreen extends Screen implements INekoScreen {
    private final NekoEntity neko;
    private EditBox textField;
    private double scrollAmount;
    private int contentHeight;

    private static final int MAX_HISTORY = 200;
    private static final int CHAT_PAD = 16;

    // Server-sourced history, persisted globally by neko UUID
    private static final Map<UUID, List<String>> HISTORY = new LinkedHashMap<>() {
        @Override protected boolean removeEldestEntry(Map.Entry<UUID, List<String>> e) { return size() > 50; }
    };

    public ChatWithNekoScreen(NekoEntity neko) {
        super(Component.translatable("screen.toneko.chat_with_neko.title", neko.getName()));
        this.neko = neko;
    }

    @Override public NekoEntity getNeko() { return neko; }

    public static void receiveHistory(UUID nekoUuid, List<String> messages) {
        List<String> h = HISTORY.computeIfAbsent(nekoUuid, k -> new ArrayList<>());
        h.clear();
        for (String msg : messages) {
            int colon = msg.indexOf(':');
            if (colon > 0) {
                String role = msg.substring(0, colon);
                String text = msg.substring(colon + 1);
                String prefix = role.equals("user") ? "§6> §f" : "§d< §f";
                h.add(prefix + text);
            }
        }
    }

    private List<String> history() {
        return HISTORY.computeIfAbsent(neko.getUUID(), k -> new ArrayList<>());
    }

    private void refreshHistory() {
        ClientPlayNetworking.send(new ChatHistoryRequestPayload(neko.getUUID().toString()));
    }

    @Override
    protected void init() {
        super.init();
        int centerX = width / 2;
        int inputY = height - 38;

        textField = new EditBox(font, centerX - 155, inputY, 230, 20, Component.empty());
        textField.setMaxLength(1000);
        addRenderableWidget(textField);

        addRenderableWidget(Button.builder(
                Component.translatable("screen.toneko.chat_with_neko.button.send"), b -> sendMessage())
                .size(60, 20).pos(centerX + 80, inputY).build());
        addRenderableWidget(Button.builder(
                Component.translatable("screen.toneko.chat_with_neko.button.end"), b -> onClose())
                .size(200, 20).pos(centerX - 100, inputY + 24).build());

        setFocused(textField);
        textField.setFocused(true);
        refreshHistory();
    }

    private void sendMessage() {
        String msg = textField.getValue().trim();
        if (msg.isEmpty()) return;
        ClientPlayNetworking.send(new ChatWithNekoPayload(neko.getUUID().toString(), msg));
        // Add to local display immediately (server response comes as system chat, not to this screen)
        List<String> h = history();
        h.add("§6> §f" + msg);
        if (h.size() > MAX_HISTORY) h.remove(0);
        textField.setValue("");
        textField.setFocused(true);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { sendMessage(); return true; }
        if (keyCode == 256) { onClose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        double maxScroll = Math.max(0, contentHeight - (height - 105));
        scrollAmount = Mth.clamp(scrollAmount - dy * 10, 0, maxScroll);
        return true;
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0x80000000);
        g.fill(0, 0, width, 30, 0xC0000000);
        g.drawCenteredString(font, getTitle(), width / 2, 8, 0xFFFFFF);

        int lineH = font.lineHeight;
        int chatX = CHAT_PAD;
        int chatY = 35;
        int chatW = width - CHAT_PAD * 2;
        int chatH = height - 80; // leave room for input row + buttons at bottom

        // Pre-calculate total content height (independent of scroll)
        int totalH = 0;
        for (String line : history()) {
            totalH += wrapLine(line, chatW - 4).size() * lineH;
        }
        contentHeight = totalH;

        // Clamp scroll
        double maxScroll = Math.max(0, contentHeight - chatH);
        scrollAmount = Mth.clamp(scrollAmount, 0, maxScroll);

        g.enableScissor(chatX, chatY, chatX + chatW, chatY + chatH);

        List<String> h = history();
        int renderY = chatY - (int) scrollAmount;
        for (String line : h) {
            List<String> wrapped = wrapLine(line, chatW - 4);
            for (String w : wrapped) {
                if (renderY + lineH > chatY && renderY < chatY + chatH) {
                    g.drawString(font, Component.literal(w), chatX + 2, renderY, 0xCCCCCC);
                }
                renderY += lineH;
            }
        }

        g.disableScissor();

        // Scrollbar
        if (contentHeight > chatH) {
            int sx = chatX + chatW - 3;
            int sh = Math.max(20, (int)((float) chatH / contentHeight * chatH));
            int sy = chatY + (int)(scrollAmount / Math.max(1, contentHeight) * chatH);
            g.fill(sx, sy, sx + 3, sy + sh, 0x80FFB6C1);
        }

        super.render(g, mx, my, pt);
    }

    /** Wrap text to fit within maxWidth, splitting on \n and word-wrapping long lines */
    private List<String> wrapLine(String text, int maxWidth) {
        List<String> result = new ArrayList<>();
        // Split on literal \n first
        String[] paragraphs = text.split("\\\\n|\\n");
        for (String para : paragraphs) {
            if (para.isEmpty()) {
                result.add("");
                continue;
            }
            // Word wrap
            StringBuilder current = new StringBuilder();
            for (int i = 0; i < para.length(); i++) {
                char c = para.charAt(i);
                current.append(c);
                // Check width periodically
                if (font.width(current.toString()) > maxWidth) {
                    // Find last space to break
                    int breakAt = current.length() - 2;
                    while (breakAt > 0 && current.charAt(breakAt) != ' ') breakAt--;
                    if (breakAt > 0) {
                        result.add(current.substring(0, breakAt));
                        current = new StringBuilder(current.substring(breakAt + 1));
                    } else {
                        // No space found, force break
                        current.setLength(current.length() - 1);
                        result.add(current.toString());
                        current = new StringBuilder(String.valueOf(c));
                    }
                }
            }
            if (!current.isEmpty()) result.add(current.toString());
        }
        return result.isEmpty() ? List.of("") : result;
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {}
}
