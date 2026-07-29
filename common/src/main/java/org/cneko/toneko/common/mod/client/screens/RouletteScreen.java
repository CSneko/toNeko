package org.cneko.toneko.common.mod.client.screens;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import org.cneko.toneko.common.mod.client.events.ClientTickEvent;
import org.cneko.toneko.common.mod.client.music.ClientMusicPlayer;
import org.cneko.toneko.common.mod.packets.NekoStealthPayload;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class RouletteScreen extends Screen implements ClientMusicPlayer.NotePlayer {
    private static final int CAT_SIZE = 56;
    private static final int CAT_GAP = 20;
    private static final int ACT_SIZE = 48;
    private static final int ACT_GAP = 16;
    private static final int CAT_Y = 45;
    private static final int ACT_Y = 140;

    private final List<Category> categories;
    private int selectedCat;
    private int selectedAct;
    /** false = 焦点在上方分类，true = 焦点在下方操作 */
    private boolean focusActions;

    private final ClientMusicPlayer clientMusicPlayer = new ClientMusicPlayer();

    public RouletteScreen() {
        super(Component.empty());
        this.categories = buildCategories();
    }

    // ---- 分类与操作定义 ----

    private record Category(Component name, int color, ResourceLocation icon, List<Action> actions) {}
    private record Action(ResourceLocation icon, Component name, Runnable action) {}

    private static final ResourceLocation
            ICON_BUFFS  = ResourceLocation.withDefaultNamespace("textures/item/potion_bottle_drinkable.png"),
            ICON_POSES  = ResourceLocation.withDefaultNamespace("textures/item/leather.png"),
            ICON_RIDING = ResourceLocation.withDefaultNamespace("textures/item/saddle.png"),
            ICON_UTIL   = ResourceLocation.withDefaultNamespace("textures/item/leather_boots.png"),
            ICON_SYSTEM = ResourceLocation.withDefaultNamespace("textures/item/compass_00.png");

    private static ResourceLocation tex(String path) {
        return ResourceLocation.withDefaultNamespace("textures/" + path);
    }

    private static Action cmdAction(String texPath, String key, String cmd) {
        return new Action(tex(texPath),
                Component.translatable("gui.toneko.roulette.option." + key),
                () -> { var p = Minecraft.getInstance().player; if (p != null) p.connection.sendUnsignedCommand(cmd); });
    }

    private static List<Category> buildCategories() {
        return List.of(
            new Category(Component.translatable("gui.toneko.roulette.cat.buffs"),  0xFF55FF55, ICON_BUFFS, List.of(
                cmdAction("mob_effect/speed.png", "speed", "neko speed"),
                cmdAction("mob_effect/jump_boost.png", "jump", "neko jump"),
                cmdAction("mob_effect/night_vision.png", "vision", "neko vision"))),
            new Category(Component.translatable("gui.toneko.roulette.cat.poses"),  0xFF55FFFF, ICON_POSES, List.of(
                cmdAction("item/leather.png", "lie", "neko lie"),
                cmdAction("item/pink_dye.png", "get_down", "neko getDown"))),
            new Category(Component.translatable("gui.toneko.roulette.cat.riding"), 0xFFFFAA55, ICON_RIDING, List.of(
                cmdAction("item/saddle.png", "ride", "neko ride"),
                cmdAction("item/red_dye.png", "ride_head", "neko rideHead"))),
            new Category(Component.translatable("gui.toneko.roulette.cat.utility"),0xFFFF55FF, ICON_UTIL, List.of(
                new Action(tex("item/leather_boots.png"),
                    Component.translatable("gui.toneko.roulette.option.stealth"),
                    () -> { boolean s = !ClientTickEvent.isStealthActive();
                            ClientTickEvent.toggleStealth(s);
                            ClientPlayNetworking.send(new NekoStealthPayload(s)); }),
                new Action(tex("item/writable_book.png"),
                    Component.translatable("gui.toneko.roulette.option.chat_with_neko"),
                    () -> Minecraft.getInstance().tell(
                        () -> ClientTickEvent.openChatWithNearestNeko(Minecraft.getInstance()))))),
            new Category(Component.translatable("gui.toneko.roulette.cat.system"), 0xFFAAAAAA, ICON_SYSTEM, List.of(
                new Action(tex("item/compass_00.png"),
                    Component.translatable("gui.toneko.roulette.option.hub"),
                    () -> Minecraft.getInstance().tell(ToNekoHubScreen::open)),
                cmdAction("item/name_tag.png", "management", "toneko gui"),
                new Action(tex("item/barrier.png"),
                    Component.translatable("gui.toneko.roulette.option.close"),
                    () -> Minecraft.getInstance().setScreen(null))))
        );
    }

    // ---- 渲染 ----

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
        renderDarkBackground(g);
        int cx = this.width / 2;

        // ---- 标题 ----
        g.drawCenteredString(this.font, Component.translatable("gui.toneko.roulette.select_category"),
                cx, 12, 0xFFFFFF);

        // ---- 上方：分类行 ----
        renderRow(g, cx, CAT_Y, CAT_SIZE, CAT_GAP,
                categories.size(), selectedCat, focusActions, mx, my,
                (i, sel) -> {
                    Category c = categories.get(i);
                    boolean hover = isMouseOver(mx, my, itemX(cx, CAT_SIZE, CAT_GAP, categories.size(), i), CAT_Y, CAT_SIZE);
                    if (hover) { selectedCat = i; selectedAct = 0; }
                    drawItem(g, itemX(cx, CAT_SIZE, CAT_GAP, categories.size(), i), CAT_Y, CAT_SIZE,
                            c.name(), c.icon(), c.color(), sel, getEdgeAlpha(i, categories.size()));
                });

        // ---- 分隔提示 ----
        Component focusHint = focusActions
                ? Component.translatable("gui.toneko.roulette.focus_actions")
                : Component.translatable("gui.toneko.roulette.focus_categories");
        g.drawCenteredString(this.font, focusHint, cx, CAT_Y + CAT_SIZE / 2 + ACT_Y - ACT_SIZE / 2 - CAT_SIZE / 2,
                0x666666);
        int sepY = (CAT_Y + CAT_SIZE / 2 + ACT_Y - ACT_SIZE / 2) / 2;
        g.fill(cx - 60, sepY, cx + 60, sepY + 1, 0x44FFFFFF);

        // ---- 下方：操作行 ----
        Category cat = categories.get(selectedCat);
        List<Action> actions = cat.actions();
        if (!actions.isEmpty()) {
            g.drawCenteredString(this.font, cat.name(), cx, ACT_Y - 24, cat.color());
            renderRow(g, cx, ACT_Y, ACT_SIZE, ACT_GAP,
                    actions.size(), selectedAct, focusActions, mx, my,
                    (i, sel) -> {
                        Action a = actions.get(i);
                        boolean hover = isMouseOver(mx, my, itemX(cx, ACT_SIZE, ACT_GAP, actions.size(), i), ACT_Y, ACT_SIZE);
                        if (hover && focusActions) selectedAct = i;
                        drawItem(g, itemX(cx, ACT_SIZE, ACT_GAP, actions.size(), i), ACT_Y, ACT_SIZE,
                                a.name(), a.icon(), sel ? 0xFFFFFFFF : 0xFF888888, sel, getEdgeAlpha(i, actions.size()));
                    });
        }

        // ---- 底部提示 ----
        Component tip = Component.translatable("gui.toneko.roulette.tip");
        g.drawCenteredString(this.font, tip, cx, this.height - 20, 0x888888);
    }

    private void renderDarkBackground(GuiGraphics g) {
        g.fill(0, 0, this.width, this.height, 0xCC000000);
    }

    @FunctionalInterface
    private interface ItemRenderer {
        void render(int index, boolean selected);
    }

    private void renderRow(GuiGraphics g, int cx, int rowY, int size, int gap,
                           int count, int selected, boolean focused,
                           int mx, int my, ItemRenderer renderer) {
        for (int i = 0; i < count; i++) {
            renderer.render(i, focused && i == selected);
        }
    }

    private int itemX(int cx, int size, int gap, int count, int index) {
        int totalW = count * size + (count - 1) * gap;
        return cx - totalW / 2 + index * (size + gap) + size / 2;
    }

    private void drawItem(GuiGraphics g, int x, int y, int size, Component name,
                           ResourceLocation icon, int color, boolean selected, float alpha) {
        int a = (int) (alpha * 255);
        float scale = selected ? 1.12f : 1.0f;
        int s = (int) (size * scale);
        int half = s / 2;

        // 背景
        int bgColor = selected ? (a << 24) | (color & 0x00FFFFFF) : (a << 24) | 0x303030;
        g.fill(x - half, y - half, x + half, y + half, bgColor);
        // 选中边框
        if (selected) {
            g.renderOutline(x - half, y - half, s, s, (a << 24) | color);
        }

        // 图标
        int iconSz = (int) (24 * scale);
        g.setColor(1, 1, 1, alpha);
        g.blit(icon, x - iconSz / 2, y - iconSz / 2 - 6, 0, 0, iconSz, iconSz, iconSz, iconSz);
        g.setColor(1, 1, 1, 1);

        // 名称
        int textColor = selected ? (a << 24) | 0xFFFFFF : (a << 24) | 0xAAAAAA;
        g.drawCenteredString(this.font, name, x, y + half + 2, textColor);
    }

    private float getEdgeAlpha(int index, int total) {
        if (total <= 3) return 1.0f;
        float d = Math.abs(index - (total - 1) / 2.0f) / (total / 2.0f);
        return 1.0f - d * 0.6f;
    }

    private boolean isMouseOver(int mx, int my, int x, int y, int size) {
        int half = size / 2;
        return mx >= x - half && mx <= x + half && my >= y - half && my <= y + half;
    }

    // ---- 输入 ----

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A) { navigate(-1); return true; }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D) { navigate(1); return true; }
        if (keyCode == GLFW.GLFW_KEY_R) { clientMusicPlayer.restart(); return true; }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (focusActions) {
                focusActions = false;
                clientMusicPlayer.tryPlayNextNote(this);
            } else {
                this.onClose();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
            if (focusActions) {
                executeAction();
            } else {
                focusActions = true;
                selectedAct = 0;
                clientMusicPlayer.tryPlayNextNote(this);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            focusActions = !focusActions;
            if (focusActions) selectedAct = 0;
            clientMusicPlayer.tryPlayNextNote(this);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        clientMusicPlayer.tryPlayNextNote(this);
        navigate(dy > 0 ? -1 : 1);
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int cx = this.width / 2;
            // 检查是否点击了某个分类
            for (int i = 0; i < categories.size(); i++) {
                if (isMouseOver((int) mx, (int) my, itemX(cx, CAT_SIZE, CAT_GAP, categories.size(), i), CAT_Y, CAT_SIZE)) {
                    selectedCat = i;
                    focusActions = true;
                    selectedAct = 0;
                    clientMusicPlayer.tryPlayNextNote(this);
                    return true;
                }
            }
            // 检查是否点击了某个操作
            if (focusActions) {
                var acts = categories.get(selectedCat).actions();
                for (int i = 0; i < acts.size(); i++) {
                    if (isMouseOver((int) mx, (int) my, itemX(cx, ACT_SIZE, ACT_GAP, acts.size(), i), ACT_Y, ACT_SIZE)) {
                        selectedAct = i;
                        executeAction();
                        return true;
                    }
                }
            }
            // 点击空白处 → 切换焦点到操作
            focusActions = true;
            clientMusicPlayer.tryPlayNextNote(this);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private void navigate(int dir) {
        clientMusicPlayer.tryPlayNextNote(this);
        if (focusActions) {
            var acts = categories.get(selectedCat).actions();
            if (!acts.isEmpty()) selectedAct = Math.floorMod(selectedAct + dir, acts.size());
        } else {
            selectedCat = Math.floorMod(selectedCat + dir, categories.size());
        }
    }

    private void executeAction() {
        clientMusicPlayer.tryPlayNextNote(this);
        var acts = categories.get(selectedCat).actions();
        if (selectedAct < acts.size()) {
            acts.get(selectedAct).action().run();
            this.onClose();
        }
    }

    // ---- 音乐 ----

    @Override
    public void playNote(NoteBlockInstrument instrument, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(
                instrument.getSoundEvent().value(), SoundSource.RECORDS,
                volume, pitch, SoundInstance.createUnseededRandom(),
                Minecraft.getInstance().player.blockPosition()));
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float pt) {}
    public static void open() { Minecraft.getInstance().setScreen(new RouletteScreen()); }
}
