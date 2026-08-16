package org.cneko.toneko.common.mod.ai.proactive;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.ai.proactive.NekoProactiveManager.NekoProactiveTrigger;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.items.LegwearItem;
import org.cneko.toneko.common.mod.misc.Charm;
import org.cneko.toneko.common.mod.misc.LegwearUtil;
import org.cneko.toneko.common.mod.misc.ScentUtil;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.cneko.toneko.common.util.ConfigUtil;

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

        // 丝袜穿搭：附近玩家穿着丝袜时主动夸搭配（概率默认 0，配置开启）
        NekoProactiveManager.register(new NekoProactiveTrigger() {
            @Override
            public String getId() { return "legwear_talk"; }

            @Override
            public boolean canTrigger(NekoEntity neko, ServerPlayer player) {
                return LegwearItem.isLegwear(LegwearUtil.getWornLegwear(player));
            }

            @Override
            public String getMessage(NekoEntity neko, ServerPlayer player) {
                return "看到对方穿着丝袜/过膝袜，夸一夸她的搭配，聊聊丝袜的话题";
            }
        });

        // 脸红偷看：高魅力玩家附近猫娘害羞脸红、偷偷瞄对方（概率默认 0，配置开启）
        NekoProactiveManager.register(new NekoProactiveTrigger() {
            @Override
            public String getId() { return "legwear_blush"; }

            @Override
            public boolean canTrigger(NekoEntity neko, ServerPlayer player) {
                return Charm.isHighCharm(LegwearUtil.getWornLegwear(player));
            }

            @Override
            public String getMessage(NekoEntity neko, ServerPlayer player) {
                return "看到对方穿着很诱人的丝袜，害羞脸红，说话有点结巴，偷偷瞄对方的腿";
            }
        });

        // 气味嗅闻：附近玩家穿着有明显气味的丝袜时，猫娘红着脸偷偷嗅闻（概率默认 0，配置开启）
        NekoProactiveManager.register(new NekoProactiveTrigger() {
            @Override
            public String getId() { return "scent_sniff"; }

            @Override
            public boolean canTrigger(NekoEntity neko, ServerPlayer player) {
                return ScentUtil.isScenty(LegwearUtil.getWornLegwear(player));
            }

            @Override
            public String getMessage(NekoEntity neko, ServerPlayer player) {
                return "闻到对方丝袜上飘来的气味，红着脸偷偷嗅了嗅，小声嘟囔几句";
            }
        });

        // 气味×魅力复合：高气味且高魅力的玩家附近猫娘深呼吸+重度脸红（概率默认 0，配置开启）
        NekoProactiveManager.register(new NekoProactiveTrigger() {
            @Override
            public String getId() { return "scent_charm"; }

            @Override
            public boolean canTrigger(NekoEntity neko, ServerPlayer player) {
                ItemStack legwear = LegwearUtil.getWornLegwear(player);
                return Charm.isHighCharm(legwear) && ScentUtil.isScenty(legwear);
            }

            @Override
            public String getMessage(NekoEntity neko, ServerPlayer player) {
                return "闻到对方丝袜浓郁的气味，又看到那诱人的绝对领域，忍不住深吸一口气，脸涨得通红，说话结结巴巴";
            }
        });

        // 猫娘间闲聊：附近还有其他猫娘且玩家在围观时，主动开口和她们聊天。
        // 广播型发言：以猫娘为中心广播给区域内玩家，并进入对话链（被点名的猫娘 3 秒后接话）。
        NekoProactiveManager.register(new NekoProactiveTrigger() {
            @Override
            public String getId() { return "nekotalk"; }

            @Override
            public boolean canTrigger(NekoEntity neko, ServerPlayer player) {
                // 16 格内还需有另一只猫娘（点名接话的前提）
                return !EntityUtil.findNekoEntitiesInRange(neko, neko.level(), 16f).isEmpty();
            }

            @Override
            public String getMessage(NekoEntity neko, ServerPlayer player) {
                return "看到身边有其他猫娘，主动开口和她们聊聊天，可以叫其中一只的名字";
            }

            @Override
            public boolean broadcast() { return true; }
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

        // 死亡变幽灵发言：事件型触发器（不参与 tick 调度，canTrigger 恒 false）。
        // 猫娘死亡化作幽灵时触发一次 AI 发言（含死因）；概率 0 = 关闭
        // 先设默认概率再注册：register 对未配置的键默认写 0，这里保证默认开启（1.0）
        String deathChanceKey = NekoProactiveManager.chanceKey("death");
        if (!ConfigUtil.CONFIG.contains(deathChanceKey)) {
            ConfigUtil.CONFIG.set(deathChanceKey, 1.0f);
        }
        NekoProactiveManager.register(new NekoProactiveTrigger() {
            @Override
            public String getId() { return "death"; }

            @Override
            public boolean canTrigger(NekoEntity neko, ServerPlayer player) {
                return false; // 仅作为死亡发言的概率开关
            }

            @Override
            public String getMessage(NekoEntity neko, ServerPlayer player) {
                return "";
            }
        });

        // 玩家死亡发言：事件型触发器（不参与 tick 调度，canTrigger 恒 false）。
        // 玩家死亡时附近猫娘（主人优先）触发一次 AI 发言；概率 0 = 关闭
        String playerDeathKey = NekoProactiveManager.chanceKey("player_death");
        if (!ConfigUtil.CONFIG.contains(playerDeathKey)) {
            ConfigUtil.CONFIG.set(playerDeathKey, 1.0f); // 未配置过 → 默认 1.0
        }
        NekoProactiveManager.register(new NekoProactiveTrigger() {
            @Override
            public String getId() { return "player_death"; }

            @Override
            public boolean canTrigger(NekoEntity neko, ServerPlayer player) {
                return false; // 仅作为玩家死亡发言的概率开关
            }

            @Override
            public String getMessage(NekoEntity neko, ServerPlayer player) {
                return "";
            }
        });
    }
}
