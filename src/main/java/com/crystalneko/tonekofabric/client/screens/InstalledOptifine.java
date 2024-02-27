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
    public InstalledOptifine() {
        super(Text.translatable("client.screen.InstalledOptifine"));
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
                .dimensions(width / 2 - 205, 20, 200, 20)
                .build();
        installIrisButton = ButtonWidget.builder(Text.translatable("client.screen.InstalledOptifine.button.iris"), button -> {
                    try {
                        Util.getOperatingSystem().open(new URI("https://modrinth.com/mod/iris")); //打开下载链接
                    } catch (URISyntaxException e) {
                        System.out.println(e.getMessage());
                    }
                })
                .dimensions(width / 2 + 5, 20, 200, 20)
                .build();

        addDrawableChild(installSodiumButton);
        addDrawableChild(installIrisButton);
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
         final MultilineText multilineText = MultilineText.create(textRenderer, Text.translatable("client.screen.InstalledOptifine.text"), width - 20);
        // 对于 1.20 及以下的版本
        multilineText.drawWithShadow(context, 10, height / 2, 16, 0xffffff);
    }
}
