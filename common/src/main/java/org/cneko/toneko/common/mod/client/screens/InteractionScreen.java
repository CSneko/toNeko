package org.cneko.toneko.common.mod.client.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class InteractionScreen extends Screen implements INekoScreen{
    private NekoScreenRegistry.NekoScreenBuilder builder;
    private Screen lastScreen;
    private NekoEntity neko;
    protected int startY = 0;
    private List<TooltipWidget> tooltips;
    public InteractionScreen(Component title, NekoEntity neko, @Nullable Screen lastScreen) {
        super(title);
        this.lastScreen = lastScreen;
        this.neko = neko;
    }
    public InteractionScreen(Component title, NekoEntity neko,@Nullable Screen lastScreen, @NotNull NekoScreenRegistry.NekoScreenBuilder builder){
        super(title);
        this.builder = builder;
    }

    @Override
    public NekoEntity getNeko() {
        return neko;
    }

    public void setBuilder(NekoScreenRegistry.NekoScreenBuilder builder) {
        this.builder = builder;
    }

    public void init() {
        super.init();

        if (builder != null) {
            startY = builder.getStartY();
            // 仅在屏幕x轴70%外的屏幕中绘制
            int x = (int) (width * 0.7);
            int y;
            if (startY != 0) {
                y = startY;
            }else {
                y = (int) (this.height * 0.1);
            }
            int buttonWidth = (int)(this.width * 0.2);
            int buttonHeight = (int)(this.height * 0.06);
            int buttonBound = (int)(this.height * 0.13);
            int tooltipBound = (int)(this.height * 0.05);
            if (lastScreen != null){
                // 添加返回按钮
                addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> Minecraft.getInstance().setScreen(lastScreen)).size(buttonWidth,buttonHeight).pos(x,y).build());
                y += buttonBound;
            }
            tooltips = new ArrayList<>();
            for (NekoScreenRegistry.NekoScreenBuilder.WidgetFactory widget : this.builder.getWidgets()) {
                if (widget instanceof NekoScreenRegistry.NekoScreenBuilder.ButtonFactory buttonFactory) {
                    Button.Builder btnBuilder = buttonFactory.build(this);
                    Button button = btnBuilder.size(buttonWidth, buttonHeight).pos(x, y).build();
                    button.setTooltip(Tooltip.create(Component.translatable(button.getMessage() + ".des")));
                    addRenderableWidget(button);
                    y += buttonBound;
                }
                if (widget instanceof NekoScreenRegistry.NekoScreenBuilder.TooltipFactory tooltipFactory) {
                    tooltips.add(new TooltipWidget(x,y,tooltipFactory.build(this)));
                    y += tooltipBound;
                }
            }
        }

    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        try {
            if (tooltips != null){
                for (TooltipWidget tooltip : tooltips) {
                    guiGraphics.renderTooltip(this.font, tooltip.tooltip(), tooltip.x(), tooltip.y());
                }
            }
        }catch (Exception ignored){}
    }

    // 打开时不暂停游戏
    @Override
    public boolean isPauseScreen() {
        return false;
    }


    // 移除背景渲染
    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(lastScreen);
    }

    public record TooltipWidget(int x,int y,Component tooltip){}

}
