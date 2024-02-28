package com.crystalneko.tonekofabric.client.screens;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Util;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.net.URI;
import java.net.URISyntaxException;

@Environment(EnvType.CLIENT)
public class InstalledOptifine extends Screen {
    Screen lastScreen;
    public InstalledOptifine(Screen screen) {
        super(Text.translatable("client.screen.InstalledOptifine"));
        lastScreen = screen;
    }

    public ButtonWidget installSodiumButton;
    public ButtonWidget installIrisButton;

    @Override
    protected void init() {
        installSodiumButton = ButtonWidget.builder(Text.translatable("client.screen.InstalledOptifine.button.sodium"), button -> {
                    try {
                        Util.getOperatingSystem().open(new URI("https://modrinth.com/mod/sodium")); //打开下载链接
                    } catch (URISyntaxException e) {
                        System.out.println(e.getMessage());
                    }
                })
                .dimensions(width / 2 - 205, height - 60, 200, 20)
                .build();
        installIrisButton = ButtonWidget.builder(Text.translatable("client.screen.InstalledOptifine.button.iris"), button -> {
                    try {
                        Util.getOperatingSystem().open(new URI("https://modrinth.com/mod/iris")); //打开下载链接
                    } catch (URISyntaxException e) {
                        System.out.println(e.getMessage());
                    }
                })
                .dimensions(width / 2 + 5, height - 60, 200, 20)
                .build();

        addDrawableChild(installSodiumButton);
        addDrawableChild(installIrisButton);
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        final MultilineText multilineText = MultilineText.create(textRenderer, Text.translatable("client.screen.InstalledOptifine.text"), width - 20);
        multilineText.drawWithShadow(context, 10, height / 2 - 20, 16, 0xffffff);
    }
    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(lastScreen);
        }
    }
}
