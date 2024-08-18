package org.cneko.toneko.fabric.client.screens;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.cneko.toneko.common.mod.packets.QuirkQueryPayload;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static org.cneko.toneko.common.mod.util.TextUtil.translatable;

public class QuirkScreen extends Screen{
    public Screen lastScreen;
    public List<String> quirks;
    public List<String> allQuirks;
    public QuirkScreen(@NotNull List<String> quirks,@NotNull List<String> allQuirks) {
        this(null,quirks,allQuirks);
    }
    public QuirkScreen(Screen lastScreen,@NotNull List<String> quirks,@NotNull List<String> allQuirks) {
        super(translatable("screen.toneko.quirk"));
        this.lastScreen = lastScreen;
        this.quirks = quirks;
        this.allQuirks = allQuirks;
    }

    @Override
    protected void init() {
        int buttonWidth = 100; // 每个按钮的宽度
        int buttonHeight = 20; // 每个按钮的高度
        int buttonSpacing = 10; // 按钮之间的间距
        int y = this.height / 2 - (buttonHeight * (allQuirks.size() + 1) + buttonSpacing * (allQuirks.size())) / 2; // 计算起始y坐标

        for (String quirk : allQuirks) {
            MutableComponent text = Component.translatable("quirk.toneko." + quirk);
            if (quirks.contains(quirk)) {
                // 如果已经包含，则将文字设置为绿色
                text.withStyle(ChatFormatting.GREEN);
            } else {
                text.withStyle(ChatFormatting.WHITE);
            }

            int x = (this.width - buttonWidth) / 2; // 居中显示按钮
            Button button = Button.builder(text, new OnQuirkPress(quirk, quirks))
                    .bounds(x, y, buttonWidth, buttonHeight)
                    .build();

            addRenderableWidget(button);
            y += buttonHeight + buttonSpacing; // 下一个按钮的位置
        }

        // 添加返回按钮
        int doneButtonX = (this.width - buttonWidth) / 2;
        int doneButtonY = y + buttonSpacing*2; // 放置在所有quirk按钮下方
        addRenderableWidget(Button.builder(translatable("gui.done"), (btn) -> {
            onClose();
        }).bounds(doneButtonX, doneButtonY, buttonWidth, buttonHeight).build());
    }

    @Override
    public void render(@NotNull GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        // 添加标题
        context.drawString(this.font, translatable("screen.toneko.quirk"), this.width / 2 - this.font.width(translatable("screen.toneko.quirk")) / 2, 20, 0xFFFFFFFF, true);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(lastScreen);
        // 向服务器发送quirk
        ClientPlayNetworking.getSender().sendPacket(new QuirkQueryPayload(quirks,allQuirks,true));
    }


    public static class OnQuirkPress implements Button.OnPress {
        public String id;
        public List<String> quirks;
        public OnQuirkPress(String id,List<String> quirks) {
            this.id = id;
            this.quirks = quirks;
        }

        @Override
        public void onPress(@NotNull Button button) {
            // 如果已经包含，则将文字设置为白色并且移除
            if (quirks.contains(id)){
                button.setMessage(Component.translatable("quirk.toneko."+id).withStyle(ChatFormatting.WHITE));
                quirks.remove(id);
            }else {
                // 如果没有包含，则将文字设置为绿色并且添加
                button.setMessage(Component.translatable("quirk.toneko."+id).withStyle(ChatFormatting.GREEN));
                quirks.add(id);
            }
        }
    }


}
