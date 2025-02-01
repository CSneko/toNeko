package org.cneko.toneko.common.mod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ToNekoKeyBindings {
    public static KeyMapping LIE_KEY;
    public static KeyMapping GET_DOWN_KEY;
    public static KeyMapping RIDE_KEY;
    public static KeyMapping QUIRK_KEY;
    public static KeyMapping SPEED_KEY;
    public static KeyMapping JUMP_KEY;
    public static KeyMapping VISION_KEY;

    public static void init(){
        LIE_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "key.toneko.lie", // The translation key of the keybinding's name
                        InputConstants.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                        GLFW.GLFW_KEY_I, // The keycode of the key
                        "key.toneko.lie.category" // The translation key of the keybinding's category.
                )
        );
        GET_DOWN_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "key.toneko.get_down", // The translation key of the keybinding's name
                        InputConstants.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                        GLFW.GLFW_KEY_O, // The keycode of the key
                        "key.toneko.lie.category" // The translation key of the keybinding's category.)
                )
        );
        RIDE_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "key.toneko.ride", // The translation key of the keybinding's name
                        InputConstants.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                        GLFW.GLFW_KEY_K, // The keycode of the key
                        "key.toneko.lie.category" // The translation key of the keybinding's category.)
                )
        );
        QUIRK_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "key.toneko.quirk", // The translation key of the keybinding's name
                        InputConstants.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                        GLFW.GLFW_KEY_J, // The keycode of the key
                        "key.toneko.lie.category" // The translation key of the keybinding's category.)
                )
        );
        SPEED_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "key.toneko.speed",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_UNKNOWN,
                        "key.toneko.lie.category"
                )
        );
        JUMP_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "key.toneko.jump",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_UNKNOWN,
                        "key.toneko.lie.category"
                )
        );
        VISION_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "key.toneko.vision",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_UNKNOWN,
                        "key.toneko.lie.category"
                )
        );
    }
}
