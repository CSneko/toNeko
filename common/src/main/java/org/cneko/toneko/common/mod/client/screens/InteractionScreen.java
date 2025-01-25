package org.cneko.toneko.common.mod.client.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InteractionScreen extends Screen implements INekoScreen{
    private NekoScreenBuilder builder;
    private List<TooltipWidget> tooltips;
    public Screen lastScreen;
    private final NekoEntity neko;
    protected int startY = 0;
    public InteractionScreen(Component title, NekoEntity neko, @Nullable Screen lastScreen) {
        super(title);
        this.lastScreen = lastScreen;
        this.neko = neko;
    }
    public InteractionScreen(Component title, NekoEntity neko,@Nullable Screen lastScreen, @NotNull NekoScreenBuilder builder){
        super(title);
        this.builder = builder;
        this.lastScreen = lastScreen;
        this.neko = neko;
    }

    @Override
    public NekoEntity getNeko() {
        return neko;
    }

    public void setBuilder(NekoScreenBuilder builder) {
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
            int buttonBound = (int)(this.height * 0.1);
            int tooltipBound = (int)(this.height * 0.1);
            tooltips = new ArrayList<>();
            for (NekoScreenBuilder.WidgetFactory widget : this.builder.getWidgets()) {
                if (widget instanceof NekoScreenBuilder.ButtonFactory buttonFactory) {
                    Button.Builder btnBuilder = buttonFactory.build(this);
                    Button button = btnBuilder.size(buttonWidth, buttonHeight).pos(x, y).build();
                    if (button.getMessage() instanceof MutableComponent component && component.getContents() instanceof TranslatableContents t) {
                        // 判断语言文件中有没有描述
                        if (!Objects.equals(t.getFallback(), t.getKey())) {
                            button.setTooltip(Tooltip.create(Component.translatable(t.getKey() + ".des")));
                        }
                    }
                    addRenderableWidget(button);
                    y += buttonBound;
                }
                if (widget instanceof NekoScreenBuilder.TooltipFactory tooltipFactory) {
                    tooltips.add(new TooltipWidget(x,y,tooltipFactory));
                    y += tooltipBound;
                }
            }
        }

    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        try {
            if (tooltips != null) {
                for (TooltipWidget tooltip : tooltips) {
                    guiGraphics.renderTooltip(this.font, tooltip.tooltip().build(this), tooltip.x(), tooltip.y());
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

    public record TooltipWidget(int x, int y, NekoScreenBuilder.TooltipFactory tooltip){}

}
