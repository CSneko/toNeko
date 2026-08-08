package org.cneko.toneko.common.mod.ai.proactive;

import net.minecraft.server.level.ServerPlayer;
import org.cneko.toneko.common.mod.ai.proactive.NekoProactiveManager.NekoProactiveTrigger;
import org.cneko.toneko.common.mod.entities.NekoEntity;

/**
 * 内置主动发言触发器（ModBootstrap 初始化时注册）。
 * 每个触发器的概率配置 ai.proactive.trigger.&lt;id&gt;.chance 在注册时自动生成，
 * 玩家可在配置界面或配置文件设置 0~1 的概率（0 = 不启用）。
 */
public class NekoProactiveTriggers {

    public static void init() {
        // 主人问候：主人在附近时主动打招呼
        NekoProactiveManager.register(new NekoProactiveTrigger() {
            @Override
            public String getId() { return "owner_hello"; }

            @Override
            public boolean canTrigger(NekoEntity neko, ServerPlayer player) {
                return neko.hasOwner(player.getUUID());
            }

            @Override
            public String getMessage(NekoEntity neko, ServerPlayer player) {
                return "看到主人来了，主动打个招呼，问问主人最近怎么样";
            }
        });

        // 夜晚关心：夜晚有玩家在附近时主动关心
        NekoProactiveManager.register(new NekoProactiveTrigger() {
            @Override
            public String getId() { return "night_watch"; }

            @Override
            public boolean canTrigger(NekoEntity neko, ServerPlayer player) {
                return !neko.level().isDay();
            }

            @Override
            public String getMessage(NekoEntity neko, ServerPlayer player) {
                return "看到对方这么晚还没睡，主动关心一下，劝他早点休息";
            }
        });

        // 空闲闲聊：没有仇恨目标且附近有玩家时主动找人聊天
        NekoProactiveManager.register(new NekoProactiveTrigger() {
            @Override
            public String getId() { return "idle_talk"; }

            @Override
            public boolean canTrigger(NekoEntity neko, ServerPlayer player) {
                return neko.getTarget() == null;
            }

            @Override
            public String getMessage(NekoEntity neko, ServerPlayer player) {
                return "有点无聊，主动找身边的人聊聊天";
            }
        });

        // 交配请求：事件型触发器（不参与 tick 调度，canTrigger 恒 false）。
        // 概率配置 > 0 时，玩家发起交配请求会先由 AI 猫娘决定是否同意
        // （回复中输出 allow_mate 动作视为同意），概率 = 0 时走原交配流程。
        NekoProactiveManager.register(new NekoProactiveTrigger() {
            @Override
            public String getId() { return "mate_request"; }

            @Override
            public boolean canTrigger(NekoEntity neko, ServerPlayer player) {
                return false; // 不参与主动发言调度，仅作为交配请求的概率开关
            }

            @Override
            public String getMessage(NekoEntity neko, ServerPlayer player) {
                return "";
            }
        });
    }
}
