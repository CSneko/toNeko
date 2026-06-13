package org.cneko.toneko.common.mod.client.screens;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.cneko.toneko.common.mod.packets.ToNekoActionPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.cneko.toneko.common.mod.util.TextUtil.translatable;

public class ToNekoManagementScreen extends Screen {
    private static final int TAB_BUTTON_WIDTH = 80;
    private static final int TAB_BUTTON_HEIGHT = 20;
    private static final int TAB_BAR_Y = 30;
    private static final int CONTENT_START_Y = 55;
    private static final int LINE_HEIGHT = 14;

    // Data
    private boolean isNeko;
    private final List<RequestEntry> pendingRequests = new ArrayList<>();
    private final List<RequestEntry> outgoingRequests = new ArrayList<>();
    private final List<OwnedNekoEntry> ownedNekos = new ArrayList<>();
    private final List<OnlineNekoEntry> onlineNekos = new ArrayList<>();
    private final List<OwnerEntry> myOwners = new ArrayList<>();

    // Tab state
    private int activeTab = 0;
    private static final int TAB_REQUESTS = 0;
    private static final int TAB_OWNED_NEKOS = 1;
    private static final int TAB_BLOCKED = 2;
    private static final int TAB_INFO = 3;

    // Scroll
    private int scrollOffset = 0;

    // Selected neko for blocked words tab
    private int selectedNekoIndex = -1;

    // EditBox fields
    @Nullable private EditBox aliasEditBox;
    @Nullable private EditBox blockWordEditBox;
    @Nullable private EditBox replaceWordEditBox;
    private String blockMethod = "word";

    // Previous screen for back navigation
    @Nullable private final Screen previousScreen;

    public ToNekoManagementScreen(CompoundTag data, @Nullable Screen previousScreen) {
        super(translatable("screen.toneko.management.title"));
        this.previousScreen = previousScreen;
        parseData(data);
    }

    private void parseData(CompoundTag data) {
        isNeko = data.getBoolean("isNeko");

        pendingRequests.clear();
        ListTag prList = data.getList("pendingRequests", Tag.TAG_COMPOUND);
        for (int i = 0; i < prList.size(); i++) {
            CompoundTag t = prList.getCompound(i);
            pendingRequests.add(new RequestEntry(t.getUUID("uuid"), t.getString("name")));
        }

        outgoingRequests.clear();
        ListTag orList = data.getList("outgoingRequests", Tag.TAG_COMPOUND);
        for (int i = 0; i < orList.size(); i++) {
            CompoundTag t = orList.getCompound(i);
            outgoingRequests.add(new RequestEntry(t.getUUID("uuid"), t.getString("name")));
        }

        ownedNekos.clear();
        ListTag onList = data.getList("ownedNekos", Tag.TAG_COMPOUND);
        for (int i = 0; i < onList.size(); i++) {
            CompoundTag t = onList.getCompound(i);
            UUID uuid = t.getUUID("uuid");
            String name = t.getString("name");
            int xp = t.getInt("xp");
            List<String> aliases = new ArrayList<>();
            ListTag aliasList = t.getList("aliases", Tag.TAG_STRING);
            for (int j = 0; j < aliasList.size(); j++) {
                aliases.add(aliasList.getString(j));
            }
            List<BlockedWordEntry> blockedWords = new ArrayList<>();
            ListTag bwList = t.getList("blockedWords", Tag.TAG_COMPOUND);
            for (int j = 0; j < bwList.size(); j++) {
                CompoundTag bwTag = bwList.getCompound(j);
                blockedWords.add(new BlockedWordEntry(
                        bwTag.getString("block"),
                        bwTag.getString("replace"),
                        bwTag.getString("method")
                ));
            }
            ownedNekos.add(new OwnedNekoEntry(uuid, name, xp, aliases, blockedWords));
        }

        onlineNekos.clear();
        ListTag onlineList = data.getList("onlineNekos", Tag.TAG_COMPOUND);
        for (int i = 0; i < onlineList.size(); i++) {
            CompoundTag t = onlineList.getCompound(i);
            onlineNekos.add(new OnlineNekoEntry(t.getUUID("uuid"), t.getString("name")));
        }

        myOwners.clear();
        ListTag moList = data.getList("myOwners", Tag.TAG_COMPOUND);
        for (int i = 0; i < moList.size(); i++) {
            CompoundTag t = moList.getCompound(i);
            myOwners.add(new OwnerEntry(t.getUUID("uuid"), t.getString("name"), t.getInt("xp")));
        }
    }

    public void handleDataUpdate(CompoundTag data) {
        parseData(data);
        clearWidgets();
        init();
    }

    @Override
    protected void init() {
        clearWidgets();
        aliasEditBox = null;
        blockWordEditBox = null;
        replaceWordEditBox = null;

        // Tab bar
        String[] tabKeys = {
                "screen.toneko.management.tab.requests",
                "screen.toneko.management.tab.nekos",
                "screen.toneko.management.tab.blocked",
                "screen.toneko.management.tab.info"
        };
        int totalTabWidth = TAB_BUTTON_WIDTH * 4 + 4 * 3;
        int startX = (this.width - totalTabWidth) / 2;
        for (int i = 0; i < 4; i++) {
            final int tabIndex = i;
            int x = startX + i * (TAB_BUTTON_WIDTH + 4);
            this.addRenderableWidget(Button.builder(
                    Component.translatable(tabKeys[i]),
                    btn -> switchTab(tabIndex)
            ).bounds(x, TAB_BAR_Y, TAB_BUTTON_WIDTH, TAB_BUTTON_HEIGHT).build());
        }

        // Content based on active tab
        switch (activeTab) {
            case TAB_REQUESTS -> initRequestsTab();
            case TAB_OWNED_NEKOS -> initOwnedNekosTab();
            case TAB_BLOCKED -> initBlockedTab();
            case TAB_INFO -> {} // Info tab is static text, no widgets needed
        }

        // Bottom buttons
        int bottomY = this.height - 30;
        int cx = this.width / 2;
        // Back button
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.back"),
                btn -> onClose()
        ).bounds(cx - 105, bottomY, 100, 20).build());
        // Refresh button
        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.toneko.management.refresh"),
                btn -> sendAction("refresh", "", "", "", "")
        ).bounds(cx + 5, bottomY, 100, 20).build());
    }

    private void initRequestsTab() {
        int left = 20;
        int y = CONTENT_START_Y;

        // Incoming requests
        if (isNeko) {
            this.addRenderableWidget(Button.builder(
                    Component.translatable("screen.toneko.management.requests.incoming"),
                    btn -> {}
            ).bounds(left, y, 200, 20).build());
            y += 24;

            if (pendingRequests.isEmpty()) {
                // just render text, no widget
            } else {
                for (RequestEntry req : pendingRequests) {
                    this.addRenderableWidget(Button.builder(
                            Component.literal("  " + req.name),
                            btn -> {}
                    ).bounds(left + 10, y, 120, 20).build());
                    this.addRenderableWidget(Button.builder(
                            Component.translatable("misc.toneko.accept"),
                            btn -> {
                                sendAction("accept", req.uuid.toString(), "", "", "");
                                sendAction("refresh", "", "", "", "");
                            }
                    ).bounds(left + 140, y, 50, 20).build());
                    this.addRenderableWidget(Button.builder(
                            Component.translatable("misc.toneko.deny"),
                            btn -> {
                                sendAction("deny", req.uuid.toString(), "", "", "");
                                sendAction("refresh", "", "", "", "");
                            }
                    ).bounds(left + 195, y, 50, 20).build());
                    y += 24;
                }
            }
            y += 8;
        }

        // Outgoing requests
        if (!outgoingRequests.isEmpty()) {
            y += 4;
            this.addRenderableWidget(Button.builder(
                    Component.translatable("screen.toneko.management.requests.outgoing"),
                    btn -> {}
            ).bounds(left, y, 200, 20).build());
            y += 24;
            for (RequestEntry req : outgoingRequests) {
                this.addRenderableWidget(Button.builder(
                        Component.literal("  " + req.name + " - ").append(Component.translatable("screen.toneko.management.requests.pending")),
                        btn -> {}
                ).bounds(left + 10, y, 200, 20).build());
                y += 22;
            }
            y += 8;
        }

        // Send new request
        y += 4;
        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.toneko.management.requests.send_new"),
                btn -> {}
        ).bounds(left, y, 200, 20).build());
        y += 24;

        if (onlineNekos.isEmpty()) {
            // renders as text
        } else {
            int count = 0;
            for (OnlineNekoEntry neko : onlineNekos) {
                int nx = left + 10 + (count % 4) * 105;
                int ny = y + (count / 4) * 23;
                this.addRenderableWidget(Button.builder(
                        Component.literal(neko.name),
                        btn -> {
                            sendAction("send_request", neko.uuid.toString(), "", "", "");
                            sendAction("refresh", "", "", "", "");
                        }
                ).bounds(nx, ny, 100, 20).build());
                count++;
            }
        }
    }

    private void initOwnedNekosTab() {
        int left = 20;
        int y = CONTENT_START_Y;

        if (ownedNekos.isEmpty()) {
            return;
        }

        // Scroll handling
        int maxVisible = (this.height - CONTENT_START_Y - 60) / 75;
        int startIndex = Math.max(0, scrollOffset);
        int endIndex = Math.min(ownedNekos.size(), startIndex + maxVisible);

        for (int i = startIndex; i < endIndex; i++) {
            OwnedNekoEntry neko = ownedNekos.get(i);
            int rowY = y + (i - startIndex) * 85;

            // Neko name and XP
            this.addRenderableWidget(Button.builder(
                    Component.literal(neko.name + "  (XP: " + neko.xp + ")"),
                    btn -> {}
            ).bounds(left, rowY, 200, 20).build());

            // Aliases
            int aliasY = rowY + 22;
            final int nekoIndex = i;
            for (String alias : neko.aliases) {
                this.addRenderableWidget(Button.builder(
                        Component.literal(alias),
                        btn -> {}
                ).bounds(left + 10, aliasY, 80, 16).build());
                this.addRenderableWidget(Button.builder(
                        Component.literal("x"),
                        btn -> {
                            sendAction("remove_alias", neko.uuid.toString(), alias, "", "");
                            sendAction("refresh", "", "", "", "");
                        }
                ).bounds(left + 95, aliasY, 20, 16).build());
                aliasY += 18;
            }

            // Add alias input
            aliasEditBox = new EditBox(this.font, left + 10, aliasY, 100, 16, Component.literal("alias"));
            aliasEditBox.setMaxLength(40);
            this.addRenderableWidget(aliasEditBox);
            final EditBox capturedAlias = aliasEditBox;
            this.addRenderableWidget(Button.builder(
                    Component.translatable("screen.toneko.management.nekos.add_alias"),
                    btn -> {
                        String val = capturedAlias.getValue().trim();
                        if (!val.isEmpty()) {
                            sendAction("add_alias", neko.uuid.toString(), val, "", "");
                            sendAction("refresh", "", "", "", "");
                        }
                    }
            ).bounds(left + 115, aliasY, 60, 16).build());

            // Remove ownership button
            this.addRenderableWidget(Button.builder(
                    Component.translatable("screen.toneko.management.nekos.remove_owner"),
                    btn -> sendAction("remove_owner", neko.uuid.toString(), "", "", "")
            ).bounds(left, aliasY + 22, 120, 16).build());
        }
    }

    private void initBlockedTab() {
        int left = 20;
        int y = CONTENT_START_Y;

        if (ownedNekos.isEmpty()) {
            return;
        }

        // Neko selector buttons
        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.toneko.management.blocked.select_neko"),
                btn -> {}
        ).bounds(left, y, 100, 20).build());

        int count = 0;
        for (int i = 0; i < ownedNekos.size(); i++) {
            OwnedNekoEntry neko = ownedNekos.get(i);
            final int idx = i;
            this.addRenderableWidget(Button.builder(
                    Component.literal(neko.name),
                    btn -> {
                        selectedNekoIndex = idx;
                        clearWidgets();
                        init();
                    }
            ).bounds(left + 110 + count * 70, y, 65, 20).build());
            count++;
        }
        y += 28;

        if (selectedNekoIndex < 0 || selectedNekoIndex >= ownedNekos.size()) {
            return;
        }

        OwnedNekoEntry neko = ownedNekos.get(selectedNekoIndex);

        // Blocked words list
        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.toneko.management.blocked.title"),
                btn -> {}
        ).bounds(left, y, 150, 20).build());
        y += 24;

        if (neko.blockedWords.isEmpty()) {
            y += LINE_HEIGHT;
        } else {
            for (BlockedWordEntry bw : neko.blockedWords) {
                String display = bw.block + " -> " + bw.replace + " (" + bw.method + ")";
                this.addRenderableWidget(Button.builder(
                        Component.literal(display),
                        btn -> {}
                ).bounds(left + 10, y, 180, 16).build());
                this.addRenderableWidget(Button.builder(
                        Component.literal("x"),
                        btn -> {
                            sendAction("remove_block", neko.uuid.toString(), bw.block, "", "");
                            sendAction("refresh", "", "", "", "");
                        }
                ).bounds(left + 195, y, 20, 16).build());
                y += 20;
            }
        }

        y += 8;

        // Add new blocked word form
        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.toneko.management.blocked.add"),
                btn -> {}
        ).bounds(left, y, 120, 20).build());
        y += 24;

        // Block word input
        blockWordEditBox = new EditBox(this.font, left + 5, y, 70, 16, Component.literal("block"));
        blockWordEditBox.setMaxLength(40);
        this.addRenderableWidget(blockWordEditBox);

        // Replace word input
        replaceWordEditBox = new EditBox(this.font, left + 85, y, 70, 16, Component.literal("replace"));
        replaceWordEditBox.setMaxLength(40);
        this.addRenderableWidget(replaceWordEditBox);

        // Method toggle
        this.addRenderableWidget(Button.builder(
                Component.literal(blockMethod),
                btn -> {
                    blockMethod = blockMethod.equals("all") ? "word" : "all";
                    btn.setMessage(Component.literal(blockMethod));
                }
        ).bounds(left + 165, y, 40, 16).build());

        // Add button
        final EditBox capturedBlock = blockWordEditBox;
        final EditBox capturedReplace = replaceWordEditBox;
        this.addRenderableWidget(Button.builder(
                Component.literal("+"),
                btn -> {
                    String b = capturedBlock.getValue().trim();
                    String r = capturedReplace.getValue().trim();
                    if (!b.isEmpty() && !r.isEmpty()) {
                        sendAction("add_block", neko.uuid.toString(), b, r, blockMethod);
                        capturedBlock.setValue("");
                        capturedReplace.setValue("");
                        sendAction("refresh", "", "", "", "");
                    }
                }
        ).bounds(left + 215, y, 20, 16).build());
    }

    private void switchTab(int index) {
        activeTab = index;
        scrollOffset = 0;
        clearWidgets();
        init();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);

        // Tab indicator highlight
        String[] tabKeys = {
                "screen.toneko.management.tab.requests",
                "screen.toneko.management.tab.nekos",
                "screen.toneko.management.tab.blocked",
                "screen.toneko.management.tab.info"
        };
        int totalTabWidth = TAB_BUTTON_WIDTH * 4 + 4 * 3;
        int startX = (this.width - totalTabWidth) / 2;
        int activeX = startX + activeTab * (TAB_BUTTON_WIDTH + 4);
        guiGraphics.fill(activeX, TAB_BAR_Y + TAB_BUTTON_HEIGHT, activeX + TAB_BUTTON_WIDTH, TAB_BAR_Y + TAB_BUTTON_HEIGHT + 2, 0xFF55FFFF);

        // Render content text for tabs without many widgets
        int left = 20;
        int y = CONTENT_START_Y;

        switch (activeTab) {
            case TAB_REQUESTS -> {
                // Render static texts that aren't widgets
                if (isNeko && pendingRequests.isEmpty()) {
                    guiGraphics.drawString(this.font, translatable("screen.toneko.management.requests.no_pending"), left + 10, y + 24, 0xAAAAAA, false);
                }
                if (onlineNekos.isEmpty()) {
                    int ry = y;
                    if (isNeko) ry += 24 + (pendingRequests.isEmpty() ? 0 : pendingRequests.size() * 24) + 8;
                    if (!outgoingRequests.isEmpty()) ry += 4 + 24 + outgoingRequests.size() * 22 + 8;
                    ry += 4 + 24;
                    guiGraphics.drawString(this.font, translatable("screen.toneko.management.requests.no_online_nekos"), left + 10, ry, 0xFF5555, false);
                }
            }
            case TAB_OWNED_NEKOS -> {
                if (ownedNekos.isEmpty()) {
                    guiGraphics.drawString(this.font, translatable("screen.toneko.management.nekos.no_nekos"), left, y, 0xFF5555, false);
                }
            }
            case TAB_BLOCKED -> {
                if (ownedNekos.isEmpty()) {
                    guiGraphics.drawString(this.font, translatable("screen.toneko.management.blocked.no_nekos"), left, y, 0xFF5555, false);
                } else if (selectedNekoIndex >= 0 && selectedNekoIndex < ownedNekos.size()) {
                    OwnedNekoEntry neko = ownedNekos.get(selectedNekoIndex);
                    if (neko.blockedWords.isEmpty()) {
                        int by = y + 24 + 24;
                        guiGraphics.drawString(this.font, translatable("screen.toneko.management.blocked.no_words"), left + 10, by, 0xAAAAAA, false);
                    }
                }
            }
            case TAB_INFO -> {
                guiGraphics.drawString(this.font, translatable("screen.toneko.management.info.title"), left, y, 0x55FFFF, false);
                y += LINE_HEIGHT + 4;
                String[] infoLines = translatable("command.toneko.help").getString().split("\n");
                for (String line : infoLines) {
                    guiGraphics.drawString(this.font, line, left, y, 0xCCCCCC, false);
                    y += LINE_HEIGHT;
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (activeTab == TAB_OWNED_NEKOS && !ownedNekos.isEmpty()) {
            int maxVisible = (this.height - CONTENT_START_Y - 60) / 85;
            int maxScroll = Math.max(0, ownedNekos.size() - maxVisible);
            scrollOffset = Math.clamp(scrollOffset - (int) scrollY, 0, maxScroll);
            clearWidgets();
            init();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        if (previousScreen != null) {
            Minecraft.getInstance().setScreen(previousScreen);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void sendAction(String action, String targetUuid, String v1, String v2, String v3) {
        ClientPlayNetworking.send(new ToNekoActionPayload(action, targetUuid, v1, v2, v3));
    }

    // ---- Data classes ----

    private record RequestEntry(UUID uuid, String name) {}
    private record OnlineNekoEntry(UUID uuid, String name) {}
    private record OwnerEntry(UUID uuid, String name, int xp) {}

    private static class OwnedNekoEntry {
        final UUID uuid;
        final String name;
        final int xp;
        final List<String> aliases;
        final List<BlockedWordEntry> blockedWords;

        OwnedNekoEntry(UUID uuid, String name, int xp, List<String> aliases, List<BlockedWordEntry> blockedWords) {
            this.uuid = uuid;
            this.name = name;
            this.xp = xp;
            this.aliases = aliases;
            this.blockedWords = blockedWords;
        }
    }

    private record BlockedWordEntry(String block, String replace, String method) {}
}
