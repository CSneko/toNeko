package org.cneko.toneko.common.mod.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.cneko.toneko.common.api.TickTasks;
import org.cneko.toneko.common.mod.ai.actions.NekoActionParser;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.misc.Messaging;
import org.cneko.toneko.common.mod.util.TickTaskQueue;
import org.cneko.toneko.common.util.AIUtil;
import org.cneko.toneko.common.util.ConfigUtil;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 猫娘间聊天互动（对话链）。
 * <p>
 * 猫娘 A 发言（区域广播）后，若话中点到附近 16 格内其他猫娘的名字（昵称/实体名），
 * 被点名者中最近的一只猫娘 B 在 3 秒后像玩家一样走 {@link AIUtil#sendMessage} 回话：
 * prompt 以 A 为对话对象（B 会看到"正在和你说话的人叫A，她是一只猫娘"），
 * 历史进入 shared 会话并带 [猫娘A] 前缀，回复广播给区域内玩家并递归检查点名（链式）。
 * 3 秒延迟后再次校验：B 与 A 仍 ≤16 格、链未超轮数，任一不满足则取消本次回话。
 * <p>
 * 定向对话（玩家指定）：AI 输出 talk_to_neko 动作后，说话者走向目标猫娘，
 * 走到贴脸（≤3 格）才触发对方回话（超时 30 秒取消），回话链路与点名对话共用。
 * <p>
 * 终止条件：轮数达上限 / 无人被点名 / 点名者超范围 / 目标冷却中。
 * 必须在服务器主线程调用（Level.getEntities 为主线程操作）。
 */
public final class NekoTalkManager {
    /** 点名接话的有效范围（与区域 AI 触发 NEKO_AI_RANGE 一致） */
    private static final double TALK_RANGE = 16.0;
    private static final double TALK_RANGE_SQ = TALK_RANGE * TALK_RANGE;
    /** 猫娘间对话的广播范围（与区域聊天 AREA_RANGE 一致，区域内玩家围观） */
    private static final double BROADCAST_RANGE = 64.0;
    /** 听到名字后回话的延迟（3 秒，20 tps） */
    private static final int RESPOND_DELAY_TICKS = 60;
    /** 链状态 15 秒无接话视为结束（下一句另起新链） */
    private static final long CHAIN_TTL_MS = 15000;
    /** 定向对话：走到这个距离才算"走到面前开口说话"（1 格贴脸） */
    private static final double FACE_TO_FACE_SQ = 1.0 * 1.0;
    /** 定向对话：检查走向的轮询间隔（10 tick） */
    private static final int WALK_POLL_TICKS = 10;
    /** 定向对话：走过去的超时（30 秒），超时取消 */
    private static final int WALK_TIMEOUT_TICKS = 600;

    /** 链状态：随"最后说话的猫娘"流动（回话者接过时轮数已计入） */
    private record Chain(int rounds, long lastActive) {}

    /** 活跃链：key = 当前可继续接话的猫娘（它再发言即延续此链） */
    private static final Map<UUID, Chain> CHAINS = new ConcurrentHashMap<>();
    /** 每只猫娘上次接话时间（冷却，防高频互刷消耗 token） */
    private static final Map<UUID, Long> lastReplyTime = new ConcurrentHashMap<>();
    /** 已在等待回话的猫娘（避免同一目标被多次点名重复调度） */
    private static final Map<UUID, Boolean> pendingReplies = new ConcurrentHashMap<>();
    /** 定向对话请求：key = 说话者 UUID（talk_to_neko 动作设置，发言广播时消费）；TTL 兜底防残留 */
    private static final long DIRECTED_TTL_MS = 30000;
    private record DirectedRequest(UUID targetUuid, long time) {}
    private static final Map<UUID, DirectedRequest> directedPending = new ConcurrentHashMap<>();

    private NekoTalkManager() {}

    /**
     * 猫娘发言（区域广播）后的挂钩入口：先消费定向对话请求（玩家指定的目标），
     * 否则检查是否点到其他猫娘的名字并启动对话链。
     *
     * @param speaker 刚发言的猫娘
     * @param text    发言文本（广播的显示文本）
     */
    public static void onNekoSpeaks(NekoEntity speaker, String text) {
        if (!ConfigUtil.isNekoTalkEnabled()) return;
        if (text == null || text.isEmpty()) {
            // 无正文不发话：清掉未消费的定向请求
            directedPending.remove(speaker.getUUID());
            return;
        }
        Level level = speaker.level();
        if (level == null) return;

        // 定向对话优先：talk_to_neko 动作显式指定了目标猫娘（30 秒内有效，防动作后无正文导致的残留）
        DirectedRequest directed = directedPending.remove(speaker.getUUID());
        if (directed != null && System.currentTimeMillis() - directed.time() <= DIRECTED_TTL_MS) {
            Entity target = level instanceof ServerLevel sl ? sl.getEntity(directed.targetUuid()) : null;
            if (target instanceof NekoEntity n && n.isAlive()) {
                onNekoDirectedTalk(speaker, n, text);
                return; // 定向对话后不再走点名匹配，避免双触发
            }
            // 目标已失效：回退到点名匹配
        }

        long now = System.currentTimeMillis();

        // 链状态：说话者名下的旧链（15 秒内）延续，否则视为新链从 0 轮开始
        Chain chain = CHAINS.remove(speaker.getUUID());
        if (chain == null || now - chain.lastActive() > CHAIN_TTL_MS) {
            chain = new Chain(0, now);
        }
        // 已超轮数上限 → 链结束，不再接话
        int maxRounds = ConfigUtil.getNekoTalkRounds();
        if (maxRounds > 0 && chain.rounds() >= maxRounds) return;

        // 点名检测：16 格内被点到名字的猫娘（近到远），排除冷却中与已在等待回话的
        NekoEntity target = findNamedTarget(speaker, text, level);
        if (target == null) return;

        // 计划回话：轮数 +1，链状态流向回话者（它再发言即延续此链）
        CHAINS.put(target.getUUID(), new Chain(chain.rounds() + 1, now));
        pendingReplies.put(target.getUUID(), true);
        scheduleReply(speaker, target, text);
    }

    /** 16 格内被点到名字的猫娘（排除自己/冷却中/待回话），按距离近到远选最近一只 */
    private static NekoEntity findNamedTarget(NekoEntity speaker, String text, Level level) {
        AABB box = speaker.getBoundingBox().inflate(TALK_RANGE);
        // getEntities(except=speaker, ...)：自动排除说话者自己
        List<Entity> found = level.getEntities(speaker, box, e -> e instanceof NekoEntity n && n.isAlive());
        found.sort(Comparator.comparingDouble(e -> speaker.distanceToSqr(e.getX(), e.getY(), e.getZ())));

        long now = System.currentTimeMillis();
        int intervalMs = ConfigUtil.getNekoTalkInterval() * 1000;
        for (Entity e : found) {
            NekoEntity n = (NekoEntity) e;
            if (pendingReplies.containsKey(n.getUUID())) continue;
            if (intervalMs > 0) {
                Long last = lastReplyTime.get(n.getUUID());
                if (last != null && now - last < intervalMs) continue;
            }
            if (isNamed(n, text)) return n;
        }
        return null;
    }

    /** 文本里是否点了这只猫娘的名字（昵称优先，其次实体名） */
    private static boolean isNamed(NekoEntity neko, String text) {
        String nick = neko.getNickName();
        if (nick != null && !nick.isEmpty() && text.contains(nick)) return true;
        return text.contains(neko.getName().getString());
    }

    /** 3 秒后执行回话（TickTasks 在主线程 tick，回调天然在主线程） */
    private static void scheduleReply(NekoEntity speaker, NekoEntity target, String text) {
        TickTaskQueue queue = new TickTaskQueue();
        queue.addTask(RESPOND_DELAY_TICKS, () -> doReply(speaker, target, text));
        TickTasks.add(queue);
    }

    /** 延迟后的回话：先校验再调 AI，像玩家一样走现有链路 */
    private static void doReply(NekoEntity speaker, NekoEntity target, String text) {
        pendingReplies.remove(target.getUUID());
        // 说话者或回话者已死亡/移除 → 取消（没听到）
        if (!speaker.isAlive() || speaker.isRemoved()) return;
        if (!target.isAlive() || target.isRemoved()) return;
        // 范围校验：回话者与说话者仍 ≤16 格，超出取消（对方走远了，这句就不说了）
        if (speaker.distanceToSqr(target) > TALK_RANGE_SQ) return;
        // 轮数校验（防御：正常流程不会超，延迟期间链可能已被重置）
        Chain chain = CHAINS.get(target.getUUID());
        if (chain == null) return;
        int maxRounds = ConfigUtil.getNekoTalkRounds();
        if (maxRounds > 0 && chain.rounds() > maxRounds) return;

        // 冷却记录：本次接话后该猫娘进入冷却
        lastReplyTime.put(target.getUUID(), System.currentTimeMillis());

        // 像玩家一样回话：prompt 以说话者（猫娘）为对话对象，B 会看到"她是一只猫娘"
        String prompt = PromptRegistry.generatePrompt(target, speaker, ConfigUtil.getAIPrompt());
        String speakerName = Prompts.NEKO_NAME.getPrompt(speaker, null);
        // 历史前缀与玩家消息 [玩家名] 格式对齐，让模型区分猫娘与玩家发言
        String historyPrefix = "[猫娘" + speakerName + "] ";
        AIUtil.sendMessage(target.getAIStorageId(), speaker.getEntity().getUUID(), prompt, text,
                response -> {
                    // AI 回调在后台线程执行，切回服务器主线程再发消息
                    ServerLevel sl = (ServerLevel) target.level();
                    sl.getServer().execute(() -> {
                        if (!target.isAlive() || target.isRemoved()) return;
                        // 猫娘间对话不执行动作（无玩家目标），只清理 JSON 动作块取显示文本
                        String displayText = NekoActionParser.parse(response.getResponse()).cleanedText();
                        if (displayText == null || displayText.isEmpty()) return;
                        // 广播给区域内玩家（以回话猫娘为中心，与玩家区域消息范围一致）
                        Messaging.sendNekoChatInRange(target, target, displayText, BROADCAST_RANGE);
                        // 链继续：回话者的发言继续检查是否点到其他猫娘的名字
                        onNekoSpeaks(target, displayText);
                    });
                }, true, historyPrefix);
    }

    /**
     * 记录定向对话请求：talk_to_neko 动作执行时调用（目标猫娘已在 16 格内解析）。
     * 说话者本次发言广播时由 {@link #onNekoSpeaks} 消费：不再走点名匹配，直接让目标回话。
     */
    public static void markDirectedTalk(NekoEntity speaker, NekoEntity target) {
        directedPending.put(speaker.getUUID(), new DirectedRequest(target.getUUID(), System.currentTimeMillis()));
    }

    /**
     * 定向对话：玩家指定（talk_to_neko 动作）说话者去找目标猫娘说话。
     * 移动已由动作提交，这里轮询直到说话者贴近目标（≤3 格）再触发回话（"走到面前才开口"），
     * 超时 30 秒取消。回话与点名对话共用链路（3 秒反应延迟/范围/轮数/冷却/去重）。
     */
    private static void onNekoDirectedTalk(NekoEntity speaker, NekoEntity target, String text) {
        if (!ConfigUtil.isNekoTalkEnabled()) return;
        if (text == null || text.isEmpty()) return;
        if (!speaker.isAlive() || speaker.isRemoved() || !target.isAlive() || target.isRemoved()) return;
        long now = System.currentTimeMillis();

        // 链状态与轮数（与点名对话同一机制）
        Chain chain = CHAINS.remove(speaker.getUUID());
        if (chain == null || now - chain.lastActive() > CHAIN_TTL_MS) {
            chain = new Chain(0, now);
        }
        int maxRounds = ConfigUtil.getNekoTalkRounds();
        if (maxRounds > 0 && chain.rounds() >= maxRounds) return;
        // 目标冷却与去重
        int intervalMs = ConfigUtil.getNekoTalkInterval() * 1000;
        if (intervalMs > 0) {
            Long last = lastReplyTime.get(target.getUUID());
            if (last != null && now - last < intervalMs) return;
        }
        if (pendingReplies.containsKey(target.getUUID())) return;

        // 计划回话：轮数 +1，链流向目标；先走到贴脸再触发（3 秒反应延迟在 scheduleReply 内）
        CHAINS.put(target.getUUID(), new Chain(chain.rounds() + 1, now));
        pendingReplies.put(target.getUUID(), true);
        scheduleWalkToTalk(speaker, target, text);
    }

    /** 轮询说话者走向目标的进度：贴脸（≤3 格）后触发回话，超时或中途失效则取消 */
    private static void scheduleWalkToTalk(NekoEntity speaker, NekoEntity target, String text) {
        TickTaskQueue queue = new TickTaskQueue();
        AtomicInteger waited = new AtomicInteger(0);
        Runnable poll = new Runnable() {
            @Override
            public void run() {
                if (!speaker.isAlive() || speaker.isRemoved() || !target.isAlive() || target.isRemoved()) {
                    pendingReplies.remove(target.getUUID()); // 中途失效 → 取消
                    return;
                }
                if (speaker.distanceToSqr(target) <= FACE_TO_FACE_SQ) {
                    // 走到面前：触发回话（含 3 秒反应延迟与范围/轮数校验）
                    scheduleReply(speaker, target, text);
                    return;
                }
                if (waited.addAndGet(WALK_POLL_TICKS) >= WALK_TIMEOUT_TICKS) {
                    pendingReplies.remove(target.getUUID()); // 超时没走到 → 取消
                    return;
                }
                queue.addTask(WALK_POLL_TICKS, this); // 未走到：继续等
            }
        };
        queue.addTask(WALK_POLL_TICKS, poll);
        TickTasks.add(queue);
    }
}
