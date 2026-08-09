package org.cneko.toneko.common.mod.ai.proactive;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.ai.actions.NekoActionExecutor;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.misc.Messaging;
import org.cneko.toneko.common.util.AIUtil;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 猫娘主动发言调度器（对齐动作注册表模式）。
 * 触发器通过 {@link #register(NekoProactiveTrigger)} 注册，
 * 注册时自动生成概率配置项（ai.proactive.trigger.&lt;id&gt;.chance，默认 0 = 不启用）。
 * 由 NekoEntity.slowTick 每秒检查一次：附近有玩家且触发器满足条件时按概率触发，
 * 猫娘通过正常 AI 流程生成消息发送给玩家。
 */
public class NekoProactiveManager {
    private static final Map<String, NekoProactiveTrigger> TRIGGERS = new LinkedHashMap<>();
    /** 每只猫娘上次主动发言时间（毫秒），防止频繁触发消耗 token */
    private static final Map<UUID, Long> lastProactiveTime = new ConcurrentHashMap<>();
    /** 主动消息查找玩家的半径 */
    private static final double PLAYER_RANGE_SQ = 16.0 * 16.0;

    private NekoProactiveManager() {}

    /** 注册触发器；id 已存在时覆盖。注册时自动生成概率配置项（默认 0） */
    public static void register(NekoProactiveTrigger trigger) {
        TRIGGERS.put(trigger.getId(), trigger);
        String key = chanceKey(trigger.getId());
        if (!ConfigUtil.CONFIG.contains(key)) {
            ConfigUtil.CONFIG.set(key, 0);
        }
    }

    public static Collection<NekoProactiveTrigger> getAll() {
        return TRIGGERS.values();
    }

    public static boolean hasTrigger(String id) {
        return TRIGGERS.containsKey(id);
    }

    /** 触发器概率配置键 */
    public static String chanceKey(String id) {
        return "ai.proactive.trigger." + id + ".chance";
    }

    /** 触发器概率（0 = 禁用，1 = 必定触发） */
    public static float getChance(String id) {
        return ConfigUtil.CONFIG.getFloat(chanceKey(id));
    }

    /**
     * 由 NekoEntity.slowTick 调用（服务端，约每秒一次）。
     * 每次最多触发一个触发器，触发后进入猫娘主动冷却。
     */
    public static void tick(NekoEntity neko) {
        if (!ConfigUtil.isAIProactiveEnabled()) return;

        // 猫娘主动冷却（防止频繁消耗 token）
        long now = System.currentTimeMillis();
        int intervalMs = ConfigUtil.getAIProactiveInterval() * 1000;
        if (intervalMs > 0) {
            Long last = lastProactiveTime.get(neko.getUUID());
            if (last != null && now - last < intervalMs) return;
        }

        // 找附近（同维度 16 格内）的玩家
        ServerPlayer target = findNearbyPlayer(neko);
        if (target == null) return;

        // 按注册顺序遍历触发器
        for (NekoProactiveTrigger trigger : TRIGGERS.values()) {
            float chance = getChance(trigger.getId());
            if (chance <= 0) continue;
            if (!trigger.canTrigger(neko, target)) continue;
            if (neko.getRandom().nextFloat() < chance) {
                lastProactiveTime.put(neko.getUUID(), now);
                fire(neko, target, trigger);
                return;
            }
        }
    }

    /** 找最近的在线玩家（同维度、存活） */
    private static ServerPlayer findNearbyPlayer(NekoEntity neko) {
        ServerPlayer nearest = null;
        for (Player player : neko.level().players()) {
            if (player.isAlive() && player instanceof ServerPlayer sp && neko.distanceToSqr(player) <= PLAYER_RANGE_SQ) {
                nearest = sp; // 循环内覆盖，越靠后越近
            }
        }
        return nearest;
    }

    /** 触发：让猫娘按正常 AI 流程生成消息并发送给玩家（动作解析同样生效） */
    private static void fire(NekoEntity neko, ServerPlayer player, NekoProactiveTrigger trigger) {
        // 主动发言有自己的间隔控制，跳过玩家冷却（避免被玩家自己的消息冷却拦截）
        // 历史保存用 [提示] 前缀，让模型区分"猫娘内心的提示"与真实玩家发言
        String hintPrefix = LanguageUtil.translatable("misc.toneko.ai.history.hint",
                new Object[]{player.getName().getString()});
        AIUtil.sendMessage(neko.getAIStorageId(), player.getUUID(),
                neko.generateAIPrompt(player), trigger.getMessage(neko, player), response -> {
            player.getServer().execute(() -> {
                if (!player.isAlive() || player.isRemoved()) return;
                // 主动发言回复走统一显示包（客户端按配置显示）
                String displayText = NekoActionExecutor.process(neko, player, response.getResponse());
                Messaging.sendNekoChat(player, neko, displayText);
            });
        }, true, hintPrefix);
    }

    /**
     * 主动发言触发器：检查触发条件并提供触发时发送给 AI 的消息内容。
     */
    public interface NekoProactiveTrigger {
        /** 唯一标识，用于概率配置键（ai.proactive.trigger.&lt;id&gt;.chance） */
        String getId();

        /** 是否满足触发条件（如：玩家是主人、夜晚、猫娘空闲等） */
        boolean canTrigger(NekoEntity neko, ServerPlayer player);

        /** 触发时发送给 AI 的消息内容（AI 会基于此生成猫娘说的话） */
        String getMessage(NekoEntity neko, ServerPlayer player);
    }
}
