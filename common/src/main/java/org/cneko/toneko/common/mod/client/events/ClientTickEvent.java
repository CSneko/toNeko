package org.cneko.toneko.common.mod.client.events;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.api.TickTasks;
import org.cneko.toneko.common.mod.client.ToNekoKeyBindings;
import org.cneko.toneko.common.mod.client.api.ClientEntityPoseManager;
import org.cneko.toneko.common.mod.client.api.GiftSelectionManager;
import org.cneko.toneko.common.mod.client.screens.ChatWithNekoScreen;
import org.cneko.toneko.common.mod.client.screens.NekoInfoScreen;
import org.cneko.toneko.common.mod.client.screens.RouletteScreen;
import org.cneko.toneko.common.mod.client.screens.ToNekoHubScreen;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.items.NekoMultiToolItem;
import org.cneko.toneko.common.mod.abilities.ClimbWallHandler;
import org.cneko.toneko.common.mod.packets.NekoMultiToolModePayload;
import org.cneko.toneko.common.mod.packets.LegwearPullUpPayload;
import org.cneko.toneko.common.mod.packets.ClimbWallPayload;
import org.cneko.toneko.common.mod.packets.NekoStealthPayload;
import org.cneko.toneko.common.mod.packets.StompActionPayload;
import org.cneko.toneko.common.mod.packets.interactives.DismountPassengerPayload;
import org.cneko.toneko.common.mod.util.EntityUtil;

import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class ClientTickEvent {
    public static void init(){
        ClientTickEvents.START_CLIENT_TICK.register(ClientTickEvent::onTick);
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvent::processKeyInput);
    }

    public static void processKeyInput(Minecraft client) {
        while (ToNekoKeyBindings.LIE_KEY.consumeClick()) {
            client.player.connection.sendUnsignedCommand("neko lie");
        }
        while (ToNekoKeyBindings.GET_DOWN_KEY.consumeClick()) {
            client.player.connection.sendUnsignedCommand("neko getDown");
        }
        while (ToNekoKeyBindings.RIDE_KEY.consumeClick()) {
            client.player.connection.sendUnsignedCommand("neko ride");
        }
        while (ToNekoKeyBindings.QUIRK_KEY.consumeClick()) {
            client.player.connection.sendUnsignedCommand("quirk gui");
        }
        while (ToNekoKeyBindings.SPEED_KEY.consumeClick()) {
            client.player.connection.sendUnsignedCommand("neko speed");
        }
        while (ToNekoKeyBindings.JUMP_KEY.consumeClick()) {
            client.player.connection.sendUnsignedCommand("neko jump");
        }
        while (ToNekoKeyBindings.VISION_KEY.consumeClick()) {
            client.player.connection.sendUnsignedCommand("neko vision");
        }
        while (ToNekoKeyBindings.RIDE_HEAD_KEY.consumeClick()) {
            client.player.connection.sendUnsignedCommand("neko rideHead");
        }
        while (ToNekoKeyBindings.ROULETTE_KEY.consumeClick()) {
            RouletteScreen.open();
        }
        while (ToNekoKeyBindings.NEKO_INFO_KEY.consumeClick()) {
            NekoInfoScreen.open();
        }
        while (ToNekoKeyBindings.DISMOUNT_PASSENGER_KEY.consumeClick()) {
            var player = client.player;
            if (player != null && !player.getPassengers().isEmpty()) {
                ClientPlayNetworking.send(new DismountPassengerPayload());
            }
        }
        while (ToNekoKeyBindings.TONEKO_MANAGEMENT_KEY.consumeClick()) {
            client.player.connection.sendUnsignedCommand("toneko gui");
        }
        while (ToNekoKeyBindings.HUB_KEY.consumeClick()) {
            ToNekoHubScreen.open();
        }
        while (ToNekoKeyBindings.CHAT_WITH_NEKO_KEY.consumeClick()) {
            openChatWithNearestNeko(client);
        }
        while (ToNekoKeyBindings.MULTI_TOOL_RANGE_KEY.consumeClick()) {
            if (client.player != null && client.player.getMainHandItem().getItem() instanceof NekoMultiToolItem) {
                ClientPlayNetworking.send(new NekoMultiToolModePayload(0));
            }
        }
        // 猫爪爬墙：检测 R 键状态变化 + 方向变化
        boolean isClimbKeyDown = ToNekoKeyBindings.CLIMB_KEY.isDown();
        boolean canClimb = isClimbKeyDown && client.player != null
                && ClimbWallHandler.isAgainstWall(client.player);
        if (canClimb) {
            // 按住 R 且靠墙：检查方向输入 (W/空格=上, S/Shift=下)
            float verticalInput = getClimbVerticalInput(client);
            // R 刚按下 或 方向发生变化时发送更新
            if (!wasClimbKeyDown || verticalInput != lastSentVerticalInput) {
                lastSentVerticalInput = verticalInput;
                ClientPlayNetworking.send(new ClimbWallPayload(true, verticalInput));
            }
            // 客户端爬墙位移预测
            double speed;
            if (verticalInput > 0.5f) {
                speed = 0.12;
            } else if (verticalInput < -0.5f) {
                speed = -0.08;
            } else {
                speed = 0;
            }
            client.player.setPos(client.player.getX(), client.player.getY() + speed, client.player.getZ());
            client.player.fallDistance = 0;
            client.player.setDeltaMovement(0, 0, 0);
        } else if (wasClimbKeyDown) {
            // R 释放 或 离开墙壁：发送停止
            lastSentVerticalInput = 0;
            if (client.player != null) {
                ClientPlayNetworking.send(new ClimbWallPayload(false, 0));
            }
        }
        wasClimbKeyDown = isClimbKeyDown;
        // 猫娘潜行：切换开关
        while (ToNekoKeyBindings.STEALTH_KEY.consumeClick()) {
            wasStealthActive = !wasStealthActive;
            if (client.player != null) {
                ClientPlayNetworking.send(new NekoStealthPayload(wasStealthActive));
                client.player.displayClientMessage(
                        Component.translatable(wasStealthActive
                                ? "messages.toneko.stealth.enabled"
                                : "messages.toneko.stealth.disabled"),
                        true);
            }
        }
        // 提袜：把过膝袜袜口复位（服务端权威 + 冷却）
        while (ToNekoKeyBindings.PULL_UP_LEGWEAR_KEY.consumeClick()) {
            if (client.player != null) {
                ClientPlayNetworking.send(new LegwearPullUpPayload());
            }
        }
        // 送礼确认：送出当前选中的快捷栏槽位（送礼选择模式内）
        while (ToNekoKeyBindings.GIFT_CONFIRM_KEY.consumeClick()) {
            GiftSelectionManager.confirm(client);
        }
        // 玩味的踩：按住踩准星指向的生物，松开取消（潜行时踩脸，否则踩身体）
        {
            boolean stompDown = ToNekoKeyBindings.STOMP_KEY.isDown();
            if (stompDown && !wasStompKeyDown) {
                // 按下瞬间：寻找目标并开始踩
                wasStompKeyDown = true;
                if (client.player != null && client.level != null) {
                    // 与服务端 STOMP_MAX_DISTANCE 保持一致，避免客户端命中却被服务端静默拒绝
                    LivingEntity target = EntityUtil.findLookedAtEntity(client.player, client.level, 3.0);
                    if (target != null) {
                        boolean shift = client.options.keyShift.isDown();
                        String part = shift ? "face" : "body";
                        // 躺倒姿态暂定：脸朝上踩 = 仰面，否则趴着（后续可扩展）
                        String pose = shift ? "back" : "prone";
                        stompTargetUuid = target.getUUID().toString();
                        ClientPlayNetworking.send(new StompActionPayload(stompTargetUuid, part, pose, true));
                    } else {
                        stompTargetUuid = null;
                    }
                }
            } else if (!stompDown && wasStompKeyDown) {
                // 松开瞬间：取消踩
                wasStompKeyDown = false;
                if (client.player != null) {
                    String target = stompTargetUuid;
                    stompTargetUuid = null;
                    ClientPlayNetworking.send(new StompActionPayload(target == null ? "" : target, "body", "prone", false));
                }
            }
        }
    }

    public static boolean isStealthActive() {
        return wasStealthActive;
    }
    public static void toggleStealth(boolean active) {
        wasStealthActive = active;
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable(active
                            ? "messages.toneko.stealth.enabled"
                            : "messages.toneko.stealth.disabled"),
                    true);
        }
    }

    private static boolean wasClimbKeyDown = false;
    private static float lastSentVerticalInput = 0;
    private static boolean wasStealthActive = false;
    private static boolean wasStompKeyDown = false;
    private static String stompTargetUuid = null;

    /** 获取爬墙垂直方向：W 或 空格=向上(1), S 或 Shift=向下(-1), 无=悬挂(0) */
    private static float getClimbVerticalInput(Minecraft client) {
        if (client.options == null) return 0;
        boolean up = client.options.keyUp.isDown() || client.options.keyJump.isDown();
        boolean down = client.options.keyDown.isDown() || client.options.keyShift.isDown();
        if (up && !down) return 1.0f;
        if (down && !up) return -1.0f;
        return 0;
    }

    public static void openChatWithNearestNeko(Minecraft client) {
        if (client.player == null || client.level == null) return;

        // Try to find a neko the player is looking at
        LivingEntity lookedAt = EntityUtil.findLookedAtEntity(client.player, client.level, 16.0);
        if (lookedAt instanceof NekoEntity neko) {
            client.setScreen(new ChatWithNekoScreen(neko));
            return;
        }

        // Fallback: find the nearest neko in range
        NekoEntity nearest = EntityUtil.findNearestNekoEntity(client.player, client.level, 12.0f);
        if (nearest != null) {
            client.setScreen(new ChatWithNekoScreen(nearest));
        } else {
            if (client.player != null) {
                client.player.displayClientMessage(
                        Component.translatable("messages.toneko.chat.no_neko_nearby"), true);
            }
        }
    }


    private static int tick = 0;
    public static void onTick(Minecraft client) {
        TickTasks.executeDefaultClient();
        // 送礼选择模式：超时/打开界面时取消
        GiftSelectionManager.tick(client);
        // 寻找16格内的生物
        Player p = Minecraft.getInstance().player;
        if (p != null) {
            var entities = EntityUtil.getLivingEntitiesInRange(p,p.level(),16);
            tick++;
            if (tick==100){
                tick = 0;
                // 删除16格外实体的所有姿势
                ClientEntityPoseManager.poseMap.entrySet().removeIf(entry -> {
                    Entity entity = entry.getKey();
                    return entity == null || entity.distanceTo(p) > 16;
                });
            }
        }
    }
}
