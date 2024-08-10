package org.cneko.toneko.fabric.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ToNekoKeyBindings {
    public static KeyMapping LIE_KEY;

    public static void init(){
        LIE_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "key.toneko.lie", // The translation key of the keybinding's name
                        InputConstants.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                        GLFW.GLFW_KEY_I, // The keycode of the key
                        "key.toneko.lie.category" // The translation key of the keybinding's category.
                )
        );
    }
}
