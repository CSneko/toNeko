package org.cneko.toneko.common.mod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ToNekoKeyBindings {
    /** 26.x：KeyMapping 分类改为强类型 Category */
    private static final net.minecraft.client.KeyMapping.Category TONEKO_CATEGORY =
            net.minecraft.client.KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("toneko", "main"));

    public static KeyMapping LIE_KEY;
    public static KeyMapping GET_DOWN_KEY;
    public static KeyMapping RIDE_KEY;
    public static KeyMapping QUIRK_KEY;
    public static KeyMapping SPEED_KEY;
    public static KeyMapping JUMP_KEY;
    public static KeyMapping VISION_KEY;
    public static KeyMapping RIDE_HEAD_KEY;
    public static KeyMapping ROULETTE_KEY;
    public static KeyMapping NEKO_INFO_KEY;
    public static KeyMapping DISMOUNT_PASSENGER_KEY;
    public static KeyMapping TONEKO_MANAGEMENT_KEY;
    public static KeyMapping HUB_KEY;
    public static KeyMapping CHAT_WITH_NEKO_KEY;
    public static KeyMapping MULTI_TOOL_RANGE_KEY;
    public static KeyMapping CLIMB_KEY;
    public static KeyMapping STEALTH_KEY;
    public static KeyMapping PULL_UP_LEGWEAR_KEY;
    public static KeyMapping GIFT_CONFIRM_KEY;
    public static KeyMapping STOMP_KEY;

    public static void init(){
        LIE_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.toneko.lie", // The translation key of the keybinding's name
                        InputConstants.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                        GLFW.GLFW_KEY_I, // The keycode of the key
                        TONEKO_CATEGORY // The translation key of the keybinding's category.
                )
        );
        GET_DOWN_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.toneko.get_down", // The translation key of the keybinding's name
                        InputConstants.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                        GLFW.GLFW_KEY_O, // The keycode of the key
                        TONEKO_CATEGORY // The translation key of the keybinding's category.)
                )
        );
        RIDE_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.toneko.ride", // The translation key of the keybinding's name
                        InputConstants.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                        GLFW.GLFW_KEY_K, // The keycode of the key
                        TONEKO_CATEGORY // The translation key of the keybinding's category.)
                )
        );
        QUIRK_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.toneko.quirk", // The translation key of the keybinding's name
                        InputConstants.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                        GLFW.GLFW_KEY_J, // The keycode of the key
                        TONEKO_CATEGORY // The translation key of the keybinding's category.)
                )
        );
        SPEED_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.toneko.speed",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_UNKNOWN,
                        TONEKO_CATEGORY
                )
        );
        JUMP_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.toneko.jump",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_UNKNOWN,
                        TONEKO_CATEGORY
                )
        );
        VISION_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.toneko.vision",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_UNKNOWN,
                        TONEKO_CATEGORY
                )
        );
        RIDE_HEAD_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.toneko.ride_head",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_UNKNOWN,
                        TONEKO_CATEGORY
                )
        );
        ROULETTE_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.toneko.roulette",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_Z,
                        TONEKO_CATEGORY
                )
        );
        NEKO_INFO_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.toneko.neko_info",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_U,
                        TONEKO_CATEGORY
                )
        );
        DISMOUNT_PASSENGER_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.toneko.dismount_passenger",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_G,
                        TONEKO_CATEGORY
                )
        );
        TONEKO_MANAGEMENT_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.toneko.management",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_UNKNOWN,
                        TONEKO_CATEGORY
                )
        );
        HUB_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.toneko.hub",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_UNKNOWN,
                        TONEKO_CATEGORY
                )
        );
        CHAT_WITH_NEKO_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.toneko.chat_with_neko",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_UNKNOWN,
                        TONEKO_CATEGORY
                )
        );
        MULTI_TOOL_RANGE_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.toneko.multi_tool_range",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_V,
                        TONEKO_CATEGORY
                )
        );
        CLIMB_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.toneko.climb",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_R,
                        TONEKO_CATEGORY
                )
        );
        STEALTH_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.toneko.stealth",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_UNKNOWN,
                        TONEKO_CATEGORY
                )
        );
        PULL_UP_LEGWEAR_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.toneko.pull_up_legwear",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_UNKNOWN,
                        TONEKO_CATEGORY
                )
        );
        GIFT_CONFIRM_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.toneko.gift_confirm",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_H,
                        TONEKO_CATEGORY
                )
        );
        STOMP_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.toneko.stomp",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_B,
                        TONEKO_CATEGORY
                )
        );
    }
}
