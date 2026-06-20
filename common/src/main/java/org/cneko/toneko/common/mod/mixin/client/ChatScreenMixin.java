package org.cneko.toneko.common.mod.mixin.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.cneko.toneko.common.mod.packets.interactives.ChatModePayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {

    @Unique private static boolean areaChatMode;

    // Button bounds
    @Unique private int btnX, btnY, btnW = 40, btnH = 14;

    protected ChatScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        // Position button at top-right of chat input area
        btnX = width - btnW - 6;
        btnY = height - 30;
    }

    @Unique
    private void toggleMode() {
        areaChatMode = !areaChatMode;
        ClientPlayNetworking.send(new ChatModePayload(areaChatMode));
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mx, double my, int button, CallbackInfoReturnable<Boolean> cir) {
        if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
            toggleMode();
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics g, int mx, int my, float pt, CallbackInfo ci) {
        btnX = width - btnW - 6;
        btnY = height - 30;
        boolean hover = mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH;

        int bg = hover ? 0x40FFFFFF : 0x20000000;
        g.fill(btnX, btnY, btnX + btnW, btnY + btnH, bg);
        g.renderOutline(btnX, btnY, btnW, btnH, areaChatMode ? 0xFF55FF55 : 0xFFAAAAAA);

        String label = areaChatMode ? "§a区域" : "§7全服";
        g.drawCenteredString(font, Component.literal(label), btnX + btnW / 2, btnY + 3, 0xFFFFFF);
    }
}
