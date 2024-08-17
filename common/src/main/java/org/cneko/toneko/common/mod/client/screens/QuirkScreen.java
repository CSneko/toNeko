package org.cneko.toneko.common.mod.client.screens;

import net.minecraft.client.gui.screens.Screen;

import static org.cneko.toneko.common.mod.util.TextUtil.translatable;
public class QuirkScreen extends Screen{
    public Screen lastScreen;
    public QuirkScreen() {
        this(null);
    }
    public QuirkScreen(Screen lastScreen) {
        super(translatable("screen.toneko.quirk"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {

    }
}
