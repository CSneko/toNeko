package org.cneko.toneko.common.mod.client.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class RouletteScreen extends Screen {
    private static final int MAX_VISIBLE_OPTIONS = 7; // 1中心 + 3左 + 3右
    private static final int RADIUS = 60;
    private static final int OPTION_SIZE = 24;

    private final List<IRouletteAction> rouletteActions;
    private int selectedIndex;
    private long lastInputTime;

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
        guiGraphics.blit(action.getIcon(), x - 8, y - 8, 0, 0, 16, 16, 16, 16);

        // 绘制名称
        Component name = action.getName();
        guiGraphics.drawCenteredString(font, name, x, y + OPTION_SIZE/2 + 2, 0xFFFFFF);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_MOUSE_BUTTON_LEFT) { // 左键
            navigate(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_MOUSE_BUTTON_RIGHT) { // 右键
            navigate(1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) { // 空格或回车
            executeSelected();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
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
        if (System.currentTimeMillis() - lastInputTime < 100) return;
        lastInputTime = System.currentTimeMillis();

        selectedIndex = Math.floorMod(selectedIndex + direction, rouletteActions.size());
    }

    private void executeSelected() {
        if (!rouletteActions.isEmpty()) {
            rouletteActions.get(selectedIndex).rouletteAction();
            this.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public interface IRouletteAction {
        ResourceLocation getIcon();
        Component getName();
        void rouletteAction();
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new RouletteScreen(getRouletteActions()));
    }

    private static List<RouletteScreen.IRouletteAction> getRouletteActions() {
        return List.of(
                new RouletteScreen.IRouletteAction() {
                    @Override
                    public ResourceLocation getIcon() {
                        return ResourceLocation.withDefaultNamespace("textures/item/barrier.png");
                    }

                    @Override
                    public Component getName() {
                        return Component.translatable("gui.toneko.roulette.option.close");
                    }

                    @Override
                    public void rouletteAction() {
                        Minecraft.getInstance().setScreen(null);
                    }
                },
                new RouletteScreen.IRouletteAction() {
                    @Override
                    public ResourceLocation getIcon() {
                        return ResourceLocation.withDefaultNamespace("textures/mob_effect/speed.png");
                    }

                    @Override
                    public Component getName() {
                        return Component.translatable("gui.toneko.roulette.option.speed");
                    }

                    @Override
                    public void rouletteAction() {
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.connection.sendUnsignedCommand("neko speed");
                        }
                    }
                },
                new RouletteScreen.IRouletteAction() {
                    @Override
                    public ResourceLocation getIcon() {
                        return ResourceLocation.withDefaultNamespace("textures/mob_effect/jump_boost.png");
                    }

                    @Override
                    public Component getName() {
                        return Component.translatable("gui.toneko.roulette.option.jump");
                    }

                    @Override
                    public void rouletteAction() {
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.connection.sendUnsignedCommand("neko jump");
                        }
                    }
                },
                new RouletteScreen.IRouletteAction() {
                    @Override
                    public ResourceLocation getIcon() {
                        return ResourceLocation.withDefaultNamespace("textures/mob_effect/night_vision.png");
                    }

                    @Override
                    public Component getName() {
                        return Component.translatable("gui.toneko.roulette.option.vision");
                    }

                    @Override
                    public void rouletteAction() {
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.connection.sendUnsignedCommand("neko vision");
                        }
                    }
                },
                new RouletteScreen.IRouletteAction() {
                    @Override
                    public ResourceLocation getIcon() {
                        return ResourceLocation.withDefaultNamespace("textures/item/leather.png");
                    }

                    @Override
                    public Component getName() {
                        return Component.translatable("gui.toneko.roulette.option.lie");
                    }

                    @Override
                    public void rouletteAction() {
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.connection.sendUnsignedCommand("neko lie");
                        }
                    }
                },
                new RouletteScreen.IRouletteAction() {
                    @Override
                    public ResourceLocation getIcon() {
                        return ResourceLocation.withDefaultNamespace("textures/item/pink_dye.png");
                    }

                    @Override
                    public Component getName() {
                        return Component.translatable("gui.toneko.roulette.option.get_down");
                    }

                    @Override
                    public void rouletteAction() {
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.connection.sendUnsignedCommand("neko getDown");
                        }
                    }
                },
                new RouletteScreen.IRouletteAction() {
                    @Override
                    public ResourceLocation getIcon() {
                        return ResourceLocation.withDefaultNamespace("textures/item/saddle.png");
                    }

                    @Override
                    public Component getName() {
                        return Component.translatable("gui.toneko.roulette.option.ride");
                    }

                    @Override
                    public void rouletteAction() {
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.connection.sendUnsignedCommand("neko ride");
                        }
                    }
                }
        );
    }
}