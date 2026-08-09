package org.cneko.toneko.common.util;

import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.cneko.toneko.common.Bootstrap.LOGGER;

/**
 * Player2 / Elefant 本地 TTS 服务封装（API v0.1.0）：
 * 端口发现（配置 → api.port 文件 → 4315）、/v1/tts/voices 声音列表、
 * /v1/tts/stop 打断、/v1/tts/speak 播放。
 * <p>
 * 播放由 Player2 App 完成（play_in_app=true），toNeko 只负责发请求。
 */
public final class TTSUtil {
    /** Player2 默认端口（端口被占用时 API 会自动换端口，见 api.port 文件） */
    public static final int DEFAULT_PORT = 4315;

    /** Player2 来源标记头：请求归属统计/开发者奖励计算（官方要求每个请求带上） */
    public static final String GAME_KEY_HEADER = "player2-game-key";

    /** Game Client ID（Player2 开发者后台注册的游戏标识，固定不可配置） */
    public static final String GAME_KEY = "019fe787-c2e6-7bb9-881d-e18b613497e4";

    /** 当前 Game Client ID（Player2 开发者后台注册的游戏标识） */
    public static String getGameKey() {
        return GAME_KEY;
    }

    /** /v1/tts/voices 返回的单个声音 */
    public record VoiceInfo(String id, String name, String language, String gender) {}

    /** Gson 映射 GET /v1/tts/voices 响应：{voices: [{id, name, language, gender}]} */
    private static class VoicesResponse {
        public List<VoiceInfo> voices = List.of();
    }

    /** POST /v1/tts/speak 请求体（字段与 Player2 v0.1.0 SingleTextToSpeechRequest 兼容） */
    public static class SpeakRequestBody {
        @SerializedName("play_in_app")
        public boolean playInApp = true;
        public double speed = 1;
        public String text = "";
        @SerializedName("voice_ids")
        public List<String> voiceIds = new java.util.ArrayList<>();
    }

    private static final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true); // 守护线程，不阻塞游戏退出
        return thread;
    });
    private static final int REQUEST_TIMEOUT = 60;

    private TTSUtil() {}

    /**
     * 解析 TTS 服务端口，优先级：
     * 1. 配置 ai.tts.port 非默认值（用户显式指定）
     * 2. Player2 api.port 文件（应用启动时写入实际端口，退出时删除）
     * 3. 默认 4315
     */
    public static int resolvePort() {
        String configured = ConfigUtil.getAITTSPort();
        if (configured != null && !configured.isEmpty()
                && !configured.equals(String.valueOf(DEFAULT_PORT))) {
            try {
                int port = Integer.parseInt(configured.trim());
                if (port >= 1 && port <= 65535) return port;
            } catch (NumberFormatException ignored) {
            }
        }
        return discoverPortFromFile().orElse(DEFAULT_PORT);
    }

    /** 从 Player2 的 api.port 文件发现实际端口（文件不存在/非法时返回 empty） */
    public static Optional<Integer> discoverPortFromFile() {
        Path file = portFilePath();
        if (file == null) return Optional.empty();
        try {
            String content = Files.readString(file).trim();
            if (content.isEmpty()) return Optional.empty();
            int port = Integer.parseInt(content);
            return (port >= 1 && port <= 65535) ? Optional.of(port) : Optional.empty();
        } catch (IOException | NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** 平台相关的 api.port 文件路径（Player2 文档） */
    private static Path portFilePath() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            return appData != null ? Paths.get(appData, "game.player2.client", "api.port") : null;
        }
        if (os.contains("mac")) {
            return Paths.get(System.getProperty("user.home"), "Library",
                    "Application Support", "game.player2.client", "api.port");
        }
        return Paths.get(System.getProperty("user.home"), ".config", "game.player2.client", "api.port");
    }

    /** TTS 服务基础地址：http://127.0.0.1:&lt;port&gt;（文档明确要求用 127.0.0.1 而非 localhost，避免 IPv6 冲突） */
    public static String baseUrl() {
        return "http://127.0.0.1:" + resolvePort();
    }

    /** Player2 请求统一携带的来源标记头（计入统计/活跃时长需要） */
    public static Map<String, String> player2Headers() {
        return Map.of(GAME_KEY_HEADER, getGameKey());
    }

    /**
     * 获取可用声音列表（GET /v1/tts/voices）。
     * 失败时 future 异常完成（HttpClient.HttpException）。
     */
    public static CompletableFuture<List<VoiceInfo>> fetchVoices() {
        return new HttpClient().sendGet(baseUrl() + "/v1/tts/voices", null, player2Headers(), VoicesResponse.class)
                .thenApply(resp -> resp.voices != null ? resp.voices : List.of());
    }

    /**
     * 停止当前播放的语音（POST /v1/tts/stop）。
     * fire-and-forget：Player2 未在播放时 stop 也可能报错，异常静默（仅 debug 日志）。
     */
    public static void stopTTS() {
        executor.submit(() -> {
            try {
                new HttpClient().sendPost(baseUrl() + "/v1/tts/stop", new Object(), player2Headers(), String.class)
                        .whenComplete((resp, ex) -> {
                            if (ex != null) {
                                LOGGER.debug("[TTS] stop failed: {}", ex.getMessage());
                            }
                        });
            } catch (Exception e) {
                LOGGER.debug("[TTS] stop failed: {}", e.toString());
            }
        });
    }

    /**
     * 播放语音（POST /v1/tts/speak，play_in_app=true 由 Player2 App 发声）。
     * 播放前先 stop 打断旧语音（后一条消息胜出）。60s 超时取消。
     */
    public static void playTTS(String text, String voice) {
        // 打断上一条未播完的语音
        stopTTS();
        executor.submit(() -> {
            try {
                SpeakRequestBody body = new SpeakRequestBody();
                body.text = text;
                body.voiceIds.add(voice);

                HttpClient client = new HttpClient();
                CompletableFuture<String> cf = client.sendPost(
                        baseUrl() + "/v1/tts/speak",
                        body,
                        player2Headers(),
                        String.class
                );
                cf.whenComplete((response, ex) -> {
                    if (ex != null) {
                        LOGGER.error("[TTS] speak failed: {}", ex.getMessage());
                    }
                    client.close();
                });
                cf.join();
            } catch (Exception e) {
                LOGGER.error("[TTS] unexpected error during speak: {}", e.toString());
            }
        });
    }
}
