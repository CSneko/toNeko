package org.cneko.toneko.common.mod.client.screens;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.packets.interactives.NekoMatePayload;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

import static org.cneko.toneko.common.mod.util.TextUtil.translatable;

public class NekoMateScreen extends Screen implements INekoScreen {
    private final NekoEntity neko;
    private @Nullable Screen lastScreen;
    private List<INeko> entities;
    public NekoMateScreen(NekoEntity neko, @NotNull List<INeko> entities, @Nullable Screen lastScreen) {
        super(Component.empty());
        this.neko = neko;
        this.lastScreen = lastScreen;
        this.entities = entities;
    }

    public static void open(NekoEntity neko, List<INeko> entities, Screen lastScreen) {
        Minecraft.getInstance().setScreen(new NekoMateScreen(neko, entities, lastScreen));
    }

    @Override
    public NekoEntity getNeko() {
        return neko;
    }

    @Override
    public void init() {
        super.init();
        int buttonWidth = 100; // 每个按钮的宽度
        int buttonHeight = 20; // 每个按钮的高度
        int buttonSpacing = 10; // 按钮之间的间距
        int y = this.height / 8; // 计算起始y坐标

        int count = 0;

        // 遍历实体列表，创建按钮
        for (INeko entity : entities) {
            // 如果不是猫娘，则跳过
            int x = (this.width / 3); // 一排显示3个按钮
            x = x*count + ( x - buttonWidth) / 2;
            Button button = Button.builder(entity.getEntity().getName(),(btn)->{
                if (entity.getEntity().isBaby()) {
                    int i = new Random().nextInt(13);
                    Minecraft.getInstance().player.sendSystemMessage(Component.translatable("message.toneko.neko.breed_fail_baby." + i));
                }else {
                    ClientPlayNetworking.send(new NekoMatePayload(neko.getUUID().toString(), entity.getEntity().getUUID().toString()));
                    onClose();
                }
            }).bounds(x, y, buttonWidth, buttonHeight).build();
            addRenderableWidget(button);
            count++;
            if (count == 3) {
                count = 0;
                y += buttonHeight + buttonSpacing; // 下一个按钮的位置
            }
        }
        // 添加返回按钮
        int doneButtonX = this.width / 2 - buttonWidth;
        int doneButtonY = y + buttonSpacing*4; // 放置在所有quirk按钮下方
        addRenderableWidget(Button.builder(translatable("gui.back"), (btn) -> {
            minecraft.setScreen(null);
        }).bounds(doneButtonX, doneButtonY, buttonWidth, buttonHeight)
                .size(buttonWidth*2, buttonHeight).build());
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
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 添加标题
        guiGraphics.drawString(this.font, translatable("screen.toneko.mate"), this.width / 2 - this.font.width(translatable("screen.toneko.mate")) / 2, 20, 0xFFFFFFFF, true);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(lastScreen);
    }
}
