package org.cneko.toneko.common.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.cneko.toneko.common.mod.util.PlayerUtil;

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
 */
public final class Player2Auth {
    /** 云端 API 基址（device flow 端点所在） */
    private static final String CLOUD_API = "https://api.player2.game/v1";
    /** 授权链接有效时间：超过后重新发起 device flow */
    private static final long DEVICE_FLOW_TIMEOUT_SECONDS = 300;

    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true); // 守护线程，不阻塞游戏退出
        return thread;
    });

    /** p2Key 进程内缓存（短时凭据；重启后重新获取——本地 login 毫秒级） */
    private static volatile String cachedP2Key;
    /** device flow 已发起标志：避免每次请求都重复触发授权 */
    private static final AtomicBoolean deviceFlowStarted = new AtomicBoolean(false);

    private Player2Auth() {}

    /**
     * 获取当前可用的 p2Key：缓存 → 本地 login/web；都没有时异步发起 device flow 并返回 empty。
     * 每次返回的 p2Key 直接用于 Authorization: Bearer &lt;p2Key&gt;。
     */
    public static Optional<String> getP2Key() {
        String cached = cachedP2Key;
        if (cached != null && !cached.isEmpty()) {
            return Optional.of(cached);
        }
        Optional<String> local = localLogin();
        if (local.isPresent()) {
            cachedP2Key = local.get();
            return local;
        }
        startDeviceFlowIfNeeded();
        return Optional.empty();
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
     * 云端 device flow（仅首次发起）：
     * /login/device/new 拿授权链接 → 通知在线玩家 → 按 interval 轮询 /login/device/token 直到拿到 p2Key。
     */
    private static void startDeviceFlowIfNeeded() {
        if (!deviceFlowStarted.compareAndSet(false, true)) {
            return;
        }
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
                        LOGGER.info("[Player2] {} 已授权，AI 使用将计入统计", TTSUtil.getGameKey());
                        return;
                    }
                    // 未授权（authorization_pending 等）继续轮询
                }
                LOGGER.warn("[Player2] device flow 超时，玩家未在 {}s 内完成授权", DEVICE_FLOW_TIMEOUT_SECONDS);
            } catch (Exception e) {
                LOGGER.debug("[Player2] device flow failed: {}", e.getMessage());
            } finally {
                // 允许未来重试（玩家可能之后才授权）
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
                                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))));
        PlayerUtil.getPlayerList().forEach(player -> player.sendSystemMessage(message));
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
