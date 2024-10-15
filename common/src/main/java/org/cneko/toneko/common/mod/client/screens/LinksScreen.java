package org.cneko.toneko.common.mod.client.screens;

import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class LinksScreen extends InteractionScreen implements INekoScreen{
    private final NekoEntity neko;
    public static final String GITHUB_LINK = "https://github.com/CSneko/toNeko";
    public static final String MODRINTH_LINK = "https://modrinth.com/mod/tonekomod";
    public static final String DISCORD_LINK = "https://discord.gg/hQ6Mm7wtt4";
    public static final String BILIBILI_LINK = "https://space.bilibili.com/3461580710742160";
    public LinksScreen(@Nullable Screen lastScreen, @NotNull NekoEntity neko) {
        super(Component.empty(), lastScreen,(screen)-> getButtonBuilders(neko));
        this.neko = neko;
    }
    public static Map<String, Button.Builder> getButtonBuilders(NekoEntity neko) {
        Map<String, Button.Builder> builders = new LinkedHashMap<>();

        builders.put("screen.toneko.links.button.github",Button.builder(Component.translatable("screen.toneko.links.button.github"),(btn)-> Util.getPlatform().openUri(GITHUB_LINK)));
        builders.put("screen.toneko.links.button.modrinth",Button.builder(Component.translatable("screen.toneko.links.button.modrinth"),(btn)-> Util.getPlatform().openUri(MODRINTH_LINK)));
        builders.put("screen.toneko.links.button.discord",Button.builder(Component.translatable("screen.toneko.links.button.discord"),(btn)-> Util.getPlatform().openUri(DISCORD_LINK)));
        builders.put("screen.toneko.links.button.bilibili",Button.builder(Component.translatable("screen.toneko.links.button.bilibili"),(btn)-> Util.getPlatform().openUri(BILIBILI_LINK)));
        return builders;
    }

    @Override
    public NekoEntity getNeko() {
        return neko;
    }
}
