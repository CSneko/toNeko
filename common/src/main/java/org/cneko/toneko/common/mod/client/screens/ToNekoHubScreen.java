package org.cneko.toneko.common.mod.client.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Button;

import static org.cneko.toneko.common.mod.util.TextUtil.translatable;

public class ToNekoHubScreen extends Screen {
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 20;
    private static final int HORIZONTAL_GAP = 10;

    // Layout Y positions
    private static final int TITLE_Y = 15;
    private static final int SECTION1_HEADER_Y = 40;
    private static final int SECTION1_BUTTONS_Y = 58;
    private static final int SECTION2_HEADER_Y = 100;
    private static final int SECTION2_ROW1_Y = 118;
    private static final int SECTION2_ROW2_Y = 143;

    // Colors (matching NekoInfoScreen style)
    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final int SECTION_COLOR = 0x55FFFF;
    private static final int DIVIDER_COLOR = 0x44FFFFFF;

    protected ToNekoHubScreen() {
        super(translatable("screen.toneko.hub.title"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new ToNekoHubScreen());
    }

    @Override
    protected void init() {
        int cx = this.width / 2;

        // === Screens ===
        int screensStartX = centerRowStart(4);
        addButton(screensStartX, SECTION1_BUTTONS_Y, 0,
                translatable("screen.toneko.hub.neko_info"),
                btn -> NekoInfoScreen.open());
        addButton(screensStartX, SECTION1_BUTTONS_Y, 1,
                translatable("screen.toneko.hub.management"),
                btn -> sendCommand("toneko gui"));
        addButton(screensStartX, SECTION1_BUTTONS_Y, 2,
                translatable("screen.toneko.hub.quirks"),
                btn -> sendCommand("quirk gui"));
        addButton(screensStartX, SECTION1_BUTTONS_Y, 3,
                translatable("screen.toneko.hub.roulette"),
                btn -> RouletteScreen.open());

        // === Quick Actions (row 1: 3 buttons) ===
        int actionsRow1X = centerRowStart(3);
        addButton(actionsRow1X, SECTION2_ROW1_Y, 0,
                translatable("screen.toneko.hub.lie"),
                btn -> sendCommand("neko lie"));
        addButton(actionsRow1X, SECTION2_ROW1_Y, 1,
                translatable("screen.toneko.hub.get_down"),
                btn -> sendCommand("neko getDown"));
        addButton(actionsRow1X, SECTION2_ROW1_Y, 2,
                translatable("screen.toneko.hub.speed"),
                btn -> sendCommand("neko speed"));

        // === Quick Actions (row 2: 2 buttons) ===
        int actionsRow2X = centerRowStart(2);
        addButton(actionsRow2X, SECTION2_ROW2_Y, 0,
                translatable("screen.toneko.hub.jump"),
                btn -> sendCommand("neko jump"));
        addButton(actionsRow2X, SECTION2_ROW2_Y, 1,
                translatable("screen.toneko.hub.vision"),
                btn -> sendCommand("neko vision"));

        // Close button
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.back"),
                btn -> this.onClose()
        ).bounds(cx - BUTTON_WIDTH / 2, this.height - 35, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);

        int cx = this.width / 2;

        // Title
        drawCenteredString(guiGraphics, this.title, cx, TITLE_Y, TITLE_COLOR);

        // Divider below title
        int dividerWidth = 200;
        guiGraphics.fill(cx - dividerWidth / 2, TITLE_Y + 14, cx + dividerWidth / 2, TITLE_Y + 15, DIVIDER_COLOR);

        // === Screens section ===
        drawCenteredString(guiGraphics, translatable("screen.toneko.hub.section.screens"),
                cx, SECTION1_HEADER_Y, SECTION_COLOR);

        // === Quick Actions section ===
        drawCenteredString(guiGraphics, translatable("screen.toneko.hub.section.actions"),
                cx, SECTION2_HEADER_Y, SECTION_COLOR);
    }

    private void addButton(int startX, int startY, int col, Component label, Button.OnPress action) {
        int x = startX + col * (BUTTON_WIDTH + HORIZONTAL_GAP);
        this.addRenderableWidget(Button.builder(label, action)
                .bounds(x, startY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    private int centerRowStart(int buttonCount) {
        int totalWidth = buttonCount * BUTTON_WIDTH + (buttonCount - 1) * HORIZONTAL_GAP;
        return (this.width / 2) - (totalWidth / 2);
    }

    private void drawCenteredString(GuiGraphics guiGraphics, Component text, int x, int y, int color) {
        guiGraphics.drawString(this.font, text, x - this.font.width(text) / 2, y, color, false);
    }

    private void sendCommand(String command) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.connection.sendUnsignedCommand(command);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
