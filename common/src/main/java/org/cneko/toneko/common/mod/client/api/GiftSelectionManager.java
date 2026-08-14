package org.cneko.toneko.common.mod.client.api;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.cneko.toneko.common.mod.client.ToNekoKeyBindings;
import org.cneko.toneko.common.mod.packets.interactives.GiftItemPayload;

/**
 * 送礼选择模式（纯客户端）：
 * 点击「送礼」按钮后进入（界面同时关闭，回到游戏），
 * 用原版快捷栏选择（滚轮 / 数字键 1-9）移动光标，
 * 按「送礼确认键」送出当前选中的快捷栏槽位；
 * 打开任何界面（Esc 等）/ 8 秒超时 / 玩家无效时取消。
 */
public class GiftSelectionManager {
    /** 选择模式超时（毫秒） */
    private static final long TIMEOUT_MS = 8000;

    private static boolean active = false;
    private static String nekoUuid = null;
    private static long expireAt = 0;

    private GiftSelectionManager() {}

    public static boolean isActive() {
        return active;
    }

    /** 进入选择模式（调用前先关闭当前界面，让原版滚轮/数字键可用） */
    public static void activate(String nekoUuid) {
        active = true;
        GiftSelectionManager.nekoUuid = nekoUuid;
        expireAt = System.currentTimeMillis() + TIMEOUT_MS;
    }

    public static void cancel() {
        active = false;
        nekoUuid = null;
    }

    /** 每客户端 tick 调用：超时 / 打开任何界面 / 玩家无效时取消 */
    public static void tick(Minecraft client) {
        if (!active) return;
        if (client.player == null || client.level == null || client.screen != null
                || System.currentTimeMillis() >= expireAt) {
            cancel();
        }
    }

    /** 按「送礼确认键」：送出当前选中的快捷栏槽位；空槽提示并保持选择模式 */
    public static void confirm(Minecraft client) {
        if (!active || nekoUuid == null) return;
        if (client.player == null) {
            cancel();
            return;
        }
        int slot = client.player.getInventory().selected;
        if (client.player.getInventory().getItem(slot).isEmpty()) {
            client.player.displayClientMessage(Component.translatable("message.toneko.gift.empty_slot"), true);
            return;
        }
        ClientPlayNetworking.send(new GiftItemPayload(nekoUuid, slot));
        cancel();
    }

    /** HUD 提示文本（含确认键名） */
    public static Component hint() {
        return Component.translatable("message.toneko.gift.select",
                ToNekoKeyBindings.GIFT_CONFIRM_KEY.getTranslatedKeyMessage());
    }
}
