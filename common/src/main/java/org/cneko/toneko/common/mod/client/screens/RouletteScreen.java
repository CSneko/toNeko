package org.cneko.toneko.common.mod.client.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import org.cneko.toneko.common.mod.client.music.ClientMusicPlayer;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class RouletteScreen extends Screen implements ClientMusicPlayer.NotePlayer{
    private static final int MAX_VISIBLE_OPTIONS = 7; // 1中心 + 3左 + 3右
    private static final int RADIUS = 60;
    private static final int OPTION_SIZE = 24;

    private final List<IRouletteAction> rouletteActions;
    private int selectedIndex;
    private long lastInputTime;

    private final ClientMusicPlayer clientMusicPlayer = new ClientMusicPlayer();

    public RouletteScreen(List<IRouletteAction> rouletteActions) {
        super(Component.empty());
        this.rouletteActions = rouletteActions;
        this.selectedIndex = 0; // 默认选中选项
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        final int centerX = this.width / 2;
        final int centerY = this.height / 2;

        // 绘制所有可见选项
        for (int offset = -3; offset <= 3; offset++) {
            int index = Math.floorMod(selectedIndex + offset, rouletteActions.size());
            double angle = Math.toRadians(90 - offset * 30);

            int x = centerX + (int)(Math.cos(angle) * RADIUS);
            int y = centerY - (int)(Math.sin(angle) * RADIUS);

            boolean isSelected = offset == 0;
            drawOption(guiGraphics, x, y, rouletteActions.get(index), isSelected);
        }

        // 绘制提示文字
        Component tip = Component.translatable("gui.toneko.roulette.tip");
        guiGraphics.drawCenteredString(this.font, tip, centerX, centerY - RADIUS - 30, 0xFFFFFF);

        // 绘制计数
        String count = (selectedIndex + 1) + "/" + rouletteActions.size();
        guiGraphics.drawCenteredString(this.font, count, centerX, centerY + RADIUS + 10, 0xFFFFFF);
    }

    private void drawOption(GuiGraphics guiGraphics, int x, int y, IRouletteAction action, boolean selected) {
        // 绘制背景
        int color = selected ? 0x8000FF00 : 0x80808080;
        guiGraphics.fill(x - OPTION_SIZE/2, y - OPTION_SIZE/2,
                x + OPTION_SIZE/2, y + OPTION_SIZE/2, color);

        // 绘制图标（需要实现纹理加载）
        //guiGraphics.blit(action.getIcon(), x - 8, y - 8, 0, 0, 16, 16, 16, 16);

        // 绘制名称
        Component name = action.getName();
        guiGraphics.drawCenteredString(font, name, x, y + OPTION_SIZE/2 + 2, 0xFFFFFF);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT) { // 左键
            navigate(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) { // 右键
            navigate(1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN){ // 上下键
            clientMusicPlayer.randomSwitchMusic();
        }
        if (keyCode == GLFW.GLFW_KEY_R){
            clientMusicPlayer.restart();
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) { // 空格或回车
            executeSelected();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // 实现 NotePlayer 接口
    @Override
    public void playNote(NoteBlockInstrument instrument, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(
                new SimpleSoundInstance(
                        instrument.getSoundEvent().value(),
                        SoundSource.RECORDS,
                        volume,
                        pitch,
                        SoundInstance.createUnseededRandom(),
                        Minecraft.getInstance().player.blockPosition()
                )
        );
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        clientMusicPlayer.tryPlayNextNote(this);
        navigate(deltaY > 0 ? -1 : 1);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // 左键点击
            executeSelected();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void navigate(int direction) {
        clientMusicPlayer.tryPlayNextNote(this);
        if (System.currentTimeMillis() - lastInputTime < 100) return;
        lastInputTime = System.currentTimeMillis();

        selectedIndex = Math.floorMod(selectedIndex + direction, rouletteActions.size());
    }

    private void executeSelected() {
        clientMusicPlayer.tryPlayNextNote(this);
        if (!rouletteActions.isEmpty()) {
            rouletteActions.get(selectedIndex).rouletteAction();
            this.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    public interface IRouletteAction {
        ResourceLocation getIcon();
        Component getName();
        void rouletteAction();
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new RouletteScreen(getRouletteActions()));
    }

    private record DefaultRouletteAction(ResourceLocation icon, Component name, Runnable action) implements IRouletteAction {
        @Override public ResourceLocation getIcon() { return icon; }
        @Override public Component getName() { return name; }
        @Override public void rouletteAction() { action.run(); }
    }

    private static List<IRouletteAction> getRouletteActions() {
        return List.of(
                new DefaultRouletteAction(
                        ResourceLocation.withDefaultNamespace("textures/item/barrier.png"),
                        Component.translatable("gui.toneko.roulette.option.close"),
                        () -> Minecraft.getInstance().setScreen(null)
                ),
                createAction("mob_effect/speed.png", "speed", "neko speed"),
                createAction("mob_effect/jump_boost.png", "jump", "neko jump"),
                createAction("mob_effect/night_vision.png", "vision", "neko vision"),
                createAction("item/leather.png", "lie", "neko lie"),
                createAction("item/pink_dye.png", "get_down", "neko getDown"),
                createAction("item/saddle.png", "ride", "neko ride")
        );
    }

    private static DefaultRouletteAction createAction(String texturePath, String optionKey, String command) {
        return new DefaultRouletteAction(
                ResourceLocation.withDefaultNamespace("textures/" + texturePath),
                Component.translatable("gui.toneko.roulette.option." + optionKey),
                () -> {
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.connection.sendUnsignedCommand(command);
                    }
                }
        );
    }

}