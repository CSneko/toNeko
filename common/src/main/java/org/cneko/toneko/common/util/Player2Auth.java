package org.cneko.toneko.common.util;

import com.google.gson.GsonBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.cneko.ai.util.FileStorageUtil;
import org.cneko.toneko.common.mod.util.PlayerUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.cneko.toneko.common.Bootstrap.LOGGER;

/**
 * Player2 认证：本地 login/web/{game_client_id} 换取 p2Key（Bearer 凭据），
 * 未授权（Game not found）时走云端 device flow，把授权链接发给在线玩家。
 * <p>
 * p2Key 是请求计入用户统计（minutes/joule）的前提：官方链路为
 * 本地 App 识别已授权的游戏 → login/web 返回短时 p2Key →
 * 所有请求带 Authorization: Bearer &lt;p2Key&gt;。
 * 拿不到 p2Key 时请求仍可用（免费本地通道），只是不计入统计。
 * <p>
 * 骚扰防护（修复"没选 player2 也频繁弹验证 / 重启后又要验证"）：
 * <ul>
 *   <li>心跳/服务探测只允许安静模式（缓存 + 本地 login），绝不自动发起云端授权；</li>
 *   <li>已授权状态持久化到本地文件——重启后不再因为本地 login 一次失败就重新弹验证；</li>
 *   <li>device flow 冷却期内不重复发起（防止 App 被反复弹窗）。</li>
 * </ul>
 */
public final class Player2Auth {
    /** 云端 API 基址（device flow 端点所在） */
    private static final String CLOUD_API = "https://api.player2.game/v1";
    /** 授权链接有效时间：超过后重新发起 device flow */
    private static final long DEVICE_FLOW_TIMEOUT_SECONDS = 300;
    /** device flow 冷却时间：两次授权流程之间的最小间隔，防止频繁弹验证 */
    private static final long DEVICE_FLOW_COOLDOWN_MS = TimeUnit.MINUTES.toMillis(30);
    /** 已授权状态持久化文件（位于 AI 数据目录下） */
    private static final String AUTH_STATE_FILE = "player2_auth.json";

    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true); // 守护线程，不阻塞游戏退出
        return thread;
    });

    private static final com.google.gson.Gson GSON = new GsonBuilder().create();

    /** p2Key 进程内缓存（短时凭据；重启后重新获取——本地 login 毫秒级） */
    private static volatile String cachedP2Key;
    /** device flow 已发起标志：避免每次请求都重复触发授权 */
    private static final AtomicBoolean deviceFlowStarted = new AtomicBoolean(false);

    /** 持久化认证状态：authorized=云端授权曾成功过；lastDeviceFlowAt=上次发起时间（冷却用） */
    private static volatile boolean authorizedOnce = false;
    private static volatile long lastDeviceFlowAt = 0;
    /** 状态文件只在首次访问时读取 */
    private static volatile boolean stateLoaded = false;

    private Player2Auth() {}

    // ===== 持久化状态读写 =====

    private static Path stateFile() {
        try {
            return Path.of(FileStorageUtil.getBasePath(), AUTH_STATE_FILE);
        } catch (Exception e) {
            return null;
        }
    }

    /** 首次访问时从磁盘加载认证状态（进程内只读一次） */
    private static void ensureStateLoaded() {
        if (stateLoaded) return;
        synchronized (Player2Auth.class) {
            if (stateLoaded) return;
            loadState();
            stateLoaded = true;
        }
    }

    private static void loadState() {
        authorizedOnce = false;
        lastDeviceFlowAt = 0;
        Path file = stateFile();
        if (file == null || !Files.isRegularFile(file)) return;
        try {
            State s = GSON.fromJson(Files.readString(file), State.class);
            if (s != null) {
                authorizedOnce = s.authorized;
                lastDeviceFlowAt = s.lastDeviceFlowAt;
            }
        } catch (Exception e) {
            LOGGER.debug("[Player2] failed to read auth state: {}", e.getMessage());
        }
    }

    private static void saveState() {
        Path file = stateFile();
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(new State(authorizedOnce, lastDeviceFlowAt)));
        } catch (Exception e) {
            LOGGER.debug("[Player2] failed to save auth state: {}", e.getMessage());
        }
    }

    /** 持久化状态 DTO */
    private static class State {
        boolean authorized;
        long lastDeviceFlowAt;
        State(boolean authorized, long lastDeviceFlowAt) {
            this.authorized = authorized;
            this.lastDeviceFlowAt = lastDeviceFlowAt;
        }
    }

    // ===== 对外入口 =====

    /**
     * 获取当前可用的 p2Key：缓存 → 本地 login；都没有时异步发起 device flow 并返回 empty。
     * 仅限真正使用 Player2 发起 AI 请求的路径调用（{@code Player2Provider}）；
     * 心跳/探测等后台任务请用 {@link #getP2KeyQuiet()}。
     */
    public static Optional<String> getP2Key() {
        ensureStateLoaded();
        Optional<String> key = quietLookup();
        if (key.isPresent()) return key;
        startDeviceFlowIfNeeded();
        return Optional.empty();
    }

    /**
     * 安静模式获取 p2Key：只查缓存和本地 login，绝不触发云端 device flow。
     * 用于心跳、服务探测等后台任务——本地 login 失败（App 未启动/未授权等）
     * 不应该打扰玩家（弹验证），等真正需要发 AI 请求时再走完整流程。
     */
    public static Optional<String> getP2KeyQuiet() {
        return quietLookup();
    }

    /** 缓存 → 本地 login（成功则写入缓存） */
    private static Optional<String> quietLookup() {
        String cached = cachedP2Key;
        if (cached != null && !cached.isEmpty()) {
            return Optional.of(cached);
        }
        Optional<String> local = localLogin();
        local.ifPresent(k -> cachedP2Key = k);
        return local;
    }

    /**
     * 本地 login：POST /v1/login/web/{clientId}。
     * App 已授权该游戏时返回 p2Key；未授权（404 Game not found）或 App 未运行时返回 empty。
     */
    static Optional<String> localLogin() {
        try {
            var future = new HttpClient().sendPost(
                    TTSUtil.baseUrl() + "/v1/login/web/" + TTSUtil.getGameKey(),
                    new Object(),
                    String.class);
            String body = future.get(5, TimeUnit.SECONDS);
            // 响应形如 {"p2Key":"p2_xxx"}
            String key = extractP2Key(body);
            if (key != null) {
                // 本地能换到 key 说明游戏已在该 App 授权，落盘记住，重启后不再自动弹验证
                ensureStateLoaded();
                authorizedOnce = true;
                saveState();
            }
            return Optional.ofNullable(key);
        } catch (Exception e) {
            // 404 Game not found / 连接失败等
            LOGGER.debug("[Player2] local login failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** 从 login 响应 JSON 中提取 p2Key（避免引入额外 DTO） */
    private static String extractP2Key(String body) {
        if (body == null) return null;
        int idx = body.indexOf("\"p2Key\"");
        if (idx < 0) return null;
        int start = body.indexOf('"', idx + 8);
        int end = body.indexOf('"', start + 1);
        if (start < 0 || end < 0 || end <= start) return null;
        String key = body.substring(start + 1, end);
        return key.isEmpty() ? null : key;
    }

    /**
     * 云端 device flow（有门槛地发起）：
     * <ul>
     *   <li>曾经授权过（持久化标记）就不再自动发起——重启后只静默重试本地 login，
     *       避免"验证过了重启又要求验证"；</li>
     *   <li>冷却期内不重复发起，避免 Player2 App 被反复弹出验证请求。</li>
     * </ul>
     * 流程：/login/device/new 拿授权链接 → 通知在线玩家 → 按 interval 轮询 /login/device/token 直到拿到 p2Key。
     */
    private static void startDeviceFlowIfNeeded() {
        ensureStateLoaded();
        // 曾授权过：不再自动弹验证。用户想重新授权可手动操作（删除 player2_auth.json 或联系管理员）
        if (authorizedOnce) {
            LOGGER.debug("[Player2] already authorized before; skip auto device flow");
            return;
        }
        // 冷却期内不重复发起
        if (System.currentTimeMillis() - lastDeviceFlowAt < DEVICE_FLOW_COOLDOWN_MS) {
            return;
        }
        if (!deviceFlowStarted.compareAndSet(false, true)) {
            return;
        }
        lastDeviceFlowAt = System.currentTimeMillis();
        saveState();
        executor.submit(() -> {
            try {
                DeviceNewResponse flow = new HttpClient()
                        .sendPost(CLOUD_API + "/login/device/new",
                                Map.of("client_id", TTSUtil.getGameKey()),
                                DeviceNewResponse.class)
                        .get(30, TimeUnit.SECONDS);

                LOGGER.info("[Player2] 授权链接: {}", flow.verificationUriComplete);
                notifyPlayers(flow.verificationUriComplete);

                long intervalMs = Math.max(flow.interval, 5) * 1000L;
                long deadline = System.currentTimeMillis() + DEVICE_FLOW_TIMEOUT_SECONDS * 1000;
                while (System.currentTimeMillis() < deadline) {
                    Thread.sleep(intervalMs);
                    DeviceTokenResponse token = new HttpClient()
                            .sendPost(CLOUD_API + "/login/device/token",
                                    Map.of(
                                            "client_id", TTSUtil.getGameKey(),
                                            "device_code", flow.deviceCode,
                                            "grant_type", "urn:ietf:params:oauth:grant-type:device_code"),
                                    DeviceTokenResponse.class)
                            .get(30, TimeUnit.SECONDS);
                    if (token != null && token.p2Key != null && !token.p2Key.isEmpty()) {
                        cachedP2Key = token.p2Key;
                        authorizedOnce = true;
                        saveState();
                        LOGGER.info("[Player2] {} 已授权，AI 使用将计入统计", TTSUtil.getGameKey());
                        return;
                    }
                    // 未授权（authorization_pending 等）继续轮询
                }
                LOGGER.warn("[Player2] device flow 超时，玩家未在 {}s 内完成授权", DEVICE_FLOW_TIMEOUT_SECONDS);
            } catch (Exception e) {
                LOGGER.debug("[Player2] device flow failed: {}", e.getMessage());
            } finally {
                // 允许未来重试（受冷却时间限制）
                deviceFlowStarted.set(false);
            }
        });
    }

    /** 把授权链接发给所有在线玩家（可点击打开） */
    private static void notifyPlayers(String url) {
        Component message = Component.literal("[Player2] 模组 " + TTSUtil.getGameKey() + " 需要授权才能计入你的 AI 使用统计，请点击：")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal("打开授权页面")
                        .withStyle(Style.EMPTY
                                .withColor(ChatFormatting.AQUA)
                                .withUnderlined(true)
                                .withClickEvent(new ClickEvent.OpenUrl(java.net.URI.create(url)))));
        // 游戏内消息保持禁用（作者决定）：授权链接已记录在日志中，
        // 且 device flow 仅在真正使用 player2 发请求且从未授权时才会发起
        //PlayerUtil.getPlayerList().forEach(player -> player.sendSystemMessage(message));
    }

    /** POST /login/device/new 响应（camelCase 与云端一致） */
    private static class DeviceNewResponse {
        String deviceCode;
        String userCode;
        String verificationUri;
        String verificationUriComplete;
        long expiresIn;
        long interval;
    }

    /** POST /login/device/token 响应 */
    private static class DeviceTokenResponse {
        String p2Key;
    }
}
