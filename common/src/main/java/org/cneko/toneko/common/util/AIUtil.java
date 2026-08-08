package org.cneko.toneko.common.util;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.cneko.ai.core.AIException;
import org.cneko.ai.core.AIHistory;
import org.cneko.ai.core.AIHistory.Content;
import org.cneko.ai.core.AIRequest;
import org.cneko.ai.core.AIResponse;
import org.cneko.ai.util.FileStorageUtil;
import org.cneko.toneko.common.mod.ai.AIServiceConfig;
import org.cneko.toneko.common.mod.ai.actions.NekoActionExecutor;
import org.cneko.toneko.common.mod.ai.provider.AIServiceProvider;
import org.cneko.toneko.common.mod.ai.provider.AIServiceProviderRegistry;
import org.cneko.toneko.common.mod.util.PlayerUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.cneko.toneko.common.Bootstrap.LOGGER;

public class AIUtil {
    private static final ExecutorService executor = Executors.newFixedThreadPool(100, r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true); // 将线程设为守护线程
        return thread;
    });
    private static final int REQUEST_TIMEOUT = 60;
    /** 记录每个玩家上次发起 AI 请求的时间（毫秒），用于频率限制 */
    private static final Map<UUID, Long> lastRequestTime = new ConcurrentHashMap<>();
    /**
     * 共享会话 ID：所有玩家共用一份猫娘历史（记忆互通），
     * 历史消息以 [说话人] 前缀标注，模型能区分谁说了什么。
     */
    public static final String SESSION_ID = "shared";

    public static void init(){
        // 向elefant发送一个简单的get请求
        if (!ConfigUtil.isAIEnabled()) {
            executor.submit(() -> {
                HttpClient client = new HttpClient();
                var response = client.sendGet("http://localhost:4315/v1/health",null, String.class);
                response.whenComplete((response1, throwable) -> {
                    boolean canUseElefant = throwable == null;
                    if (canUseElefant) {
                        ConfigUtil.CONFIG.set("ai.service", "custom");
                        ConfigUtil.CONFIG.set("ai.enable", true);
                        ConfigUtil.CONFIG.set("ai.tts.enable", true);
                        ConfigUtil.CONFIG.set("ai.tts.service", "player2");
                        ConfigUtil.CONFIG.save();
                        LOGGER.info("Found Elefant running, set AI to Custom (localhost:4315)");
                    }
                });
                response.join();
            });
        }

    }

    /**
     * Map legacy service names to new provider IDs.
     */
    private static String resolveProviderId(String service) {
        if (service == null || service.isEmpty()) return null;

        // Legacy aliases
        if (service.equalsIgnoreCase("elefant") || service.equalsIgnoreCase("player2")) {
            return "custom";
        }
        // If it's a URL, use the custom provider
        if (service.startsWith("http://") || service.startsWith("https://")) {
            return "custom";
        }
        // If the provider is registered directly, use it
        if (AIServiceProviderRegistry.hasProvider(service)) {
            return service.toLowerCase();
        }
        // Not found
        return null;
    }

    /**
     * Parse a legacy custom URL into host, port, endpoint, and tls components.
     * endpoint 语义与 OpenAI SDK 一致：路径为空或 "/" 时用默认端点，
     * 其他路径自动拼接 "/chat/completions"（除非已是完整端点）。
     */
    private static ParsedUrl parseLegacyUrl(String url) {
        ParsedUrl result = new ParsedUrl();
        if (url.startsWith("http://")) {
            url = url.substring("http://".length());
            result.tls = false;
        } else if (url.startsWith("https://")) {
            url = url.substring("https://".length());
            result.tls = true;
        }
        String[] hostPortAndPath = url.split("/", 2);
        String hostPortSection = hostPortAndPath[0];
        String path = hostPortAndPath.length > 1 ? "/" + hostPortAndPath[1] : "";
        path = path.replaceAll("/+$", "");
        if (path.isEmpty() || path.equals("/")) {
            result.endpoint = "/v1/chat/completions";
        } else if (path.endsWith("/chat/completions") || path.endsWith("/messages") || path.endsWith("/generateContent")) {
            result.endpoint = path; // 完整端点，原样使用
        } else {
            result.endpoint = path + "/chat/completions";
        }

        int colonIndex = hostPortSection.indexOf(':');
        if (colonIndex != -1) {
            result.host = hostPortSection.substring(0, colonIndex);
            try {
                result.port = Integer.parseInt(hostPortSection.substring(colonIndex + 1));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid port number in URL: " + url);
            }
        } else {
            result.host = hostPortSection;
            result.port = result.tls ? 443 : 80;
        }
        return result;
    }

    private static class ParsedUrl {
        String host;
        int port;
        String endpoint;
        boolean tls;
    }

    /**
     * 猫娘实体的 UUID 可能变化（重新生成、存档迁移等），持久 AI 存储 ID 保持不变。
     * 首次为猫娘生成持久 ID 时，把旧路径（当前 UUID）下的聊天记录迁移到新路径，
     * 避免历史对话丢失。目录结构：ai/data/&lt;猫娘存储ID&gt;/&lt;玩家UUID&gt;.json
     */
    public static void migrateNekoStorage(String oldStorageId, String newStorageId) {
        try {
            Path base = Paths.get(FileStorageUtil.getBasePath());
            Path oldDir = base.resolve(oldStorageId);
            Path newDir = base.resolve(newStorageId);
            if (Files.isDirectory(oldDir) && !Files.isDirectory(newDir)) {
                Files.move(oldDir, newDir);
                LOGGER.info("Migrated AI chat history from {} to {}", oldStorageId, newStorageId);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to migrate AI chat history from {} to {}: {}", oldStorageId, newStorageId, e.getMessage());
        }
    }

    /**
     * @param nekoStorageId 猫娘的持久存储 ID（使用 {@code neko.getAIStorageId()}），
     *                      而非实体 UUID —— 实体 UUID 变化时聊天记录仍能对应
     */
    public static void sendMessage(String nekoStorageId, UUID userUuid, String prompt, String message, MessageCallback callback){
        sendMessage(nekoStorageId, userUuid, prompt, message, callback, false, null);
    }

    /**
     * @param ignoreCooldown 跳过冷却检查：用于同一条玩家消息触发的多只猫娘批次
     *                       （第一只正常检查并计时，其余跳过，避免同批请求被冷却拦截）
     */
    public static void sendMessage(String nekoStorageId, UUID userUuid, String prompt, String message, MessageCallback callback, boolean ignoreCooldown){
        sendMessage(nekoStorageId, userUuid, prompt, message, callback, ignoreCooldown, null);
    }

    /**
     * @param ignoreCooldown 跳过冷却检查（见重载说明）
     * @param historyPrefix  保存历史时的自定义前缀（替代默认的 [说话人] 前缀）。
     *                       用于猫娘主动发言/内部触发消息，例如 "[提示]对Steve："，
     *                       让模型在历史中区分"提示"与真实玩家发言；null 使用默认前缀
     */
    public static void sendMessage(String nekoStorageId, UUID userUuid, String prompt, String message, MessageCallback callback, boolean ignoreCooldown, String historyPrefix){
        // 同一玩家的请求冷却（防刷屏消耗API额度），配置为0或负数时禁用
        if (!ignoreCooldown) {
            long cooldownMs = ConfigUtil.getAICooldown() * 1000L;
            if (cooldownMs > 0) {
                long now = System.currentTimeMillis();
                Long last = lastRequestTime.get(userUuid);
                if (last != null && now - last < cooldownMs) {
                    callback.execute(new AIResponse(LanguageUtil.translatable("misc.toneko.ai.cooldown"), 429));
                    return;
                }
                lastRequestTime.put(userUuid, now);
            }
        }

        final boolean debug = ConfigUtil.isAIDebugEnabled();
        final long startTime = System.currentTimeMillis();
        final String msgSnippet = message.length() > 80 ? message.substring(0, 80) + "..." : message;
        // 启用 AI 动作时，把动作说明拼到 prompt 末尾（让模型知道可以输出 JSON 动作）
        final String fullPrompt = ConfigUtil.isAIActionsEnabled()
                ? prompt + "\n" + NekoActionExecutor.actionGuide()
                : prompt;
        // 说话人名字（共享会话历史标注用）：入口在主线程解析，玩家离线时用 UUID 缩写兜底
        String resolvedSpeaker;
        try {
            net.minecraft.world.entity.player.Player speaker = PlayerUtil.getPlayerByUUID(userUuid);
            resolvedSpeaker = speaker != null ? speaker.getName().getString() : userUuid.toString().substring(0, 8);
        } catch (Exception e) {
            resolvedSpeaker = userUuid.toString().substring(0, 8);
        }
        final String speakerName = resolvedSpeaker;

        var future = executor.submit(()->{
            try{
                String rawService = ConfigUtil.getAIService();
                String providerId = resolveProviderId(rawService);

                if (providerId == null) {
                    LOGGER.warn("Unsupported AI service: {}, please read the docs: https://s.cneko.org/toNekoAI", rawService);
                    callback.execute(new AIResponse("Unsupported AI service: " + rawService + ", please read the docs: https://s.cneko.org/toNekoAI", 400));
                    return;
                }

                AIServiceProvider provider = AIServiceProviderRegistry.get(providerId);
                if (provider == null) {
                    LOGGER.warn("AI provider not found: {}", providerId);
                    callback.execute(new AIResponse("AI provider not found: " + providerId, 400));
                    return;
                }

                // Build config
                AIServiceConfig serviceConfig;
                if (rawService.startsWith("http://") || rawService.startsWith("https://")) {
                    ParsedUrl parsed = parseLegacyUrl(rawService);
                    serviceConfig = AIServiceConfig.builder("custom")
                            .apiKey(ConfigUtil.getAIKey())
                            .model(ConfigUtil.getAIModel())
                            .host(parsed.host)
                            .port(parsed.port)
                            .endpoint(parsed.endpoint)
                            .tls(parsed.tls)
                            .prompt(fullPrompt)
                            .showThink(ConfigUtil.isAIShowThink())
                            .build();
                    provider = AIServiceProviderRegistry.get("custom");
                } else {
                    serviceConfig = ConfigUtil.buildAIServiceConfig(providerId);
                    serviceConfig = AIServiceConfig.builder(providerId)
                            .apiKey(serviceConfig.getApiKey())
                            .model(serviceConfig.getModel())
                            .host(serviceConfig.getHost())
                            .port(serviceConfig.getPort())
                            .endpoint(serviceConfig.getEndpoint())
                            .tls(serviceConfig.isTls())
                            .proxy(serviceConfig.getProxy())
                            .prompt(fullPrompt)
                            .showThink(ConfigUtil.isAIShowThink())
                            .build();
                }

                if (debug) {
                    String keyPreview = serviceConfig.getApiKey().isEmpty() ? "(none)"
                            : serviceConfig.getApiKey().substring(0, Math.min(8, serviceConfig.getApiKey().length())) + "***";
                    LOGGER.info("[AI-DEBUG] >>> REQUEST | provider={} model={} host={}:{} endpoint={} tls={} key={} msg({}c)=\"{}\"",
                            providerId, serviceConfig.getModel(),
                            serviceConfig.getHost(), serviceConfig.getPort(), serviceConfig.getEndpoint(),
                            serviceConfig.isTls(), keyPreview, msgSnippet.length(), msgSnippet);
                }

                String uuidStr = nekoStorageId;
                // 共享会话：所有玩家共用一份历史（记忆互通），消息带说话人前缀
                String userUuidStr = SESSION_ID;

                // NekoAI v0.2.0: AIRequest 只承载对话内容，会话存储由调用方负责
                AIHistory history = FileStorageUtil.readConversation(uuidStr, userUuidStr);
                // 长对话自动总结：历史达到最大会话长度时，把最早的对话总结为一条摘要（保留关键信息）
                history = summarizeHistory(uuidStr, userUuidStr, history);
                // 按配置的最大会话长度裁剪历史（保留最近N条），并写回文件控制存储大小
                history = trimHistory(uuidStr, userUuidStr, history);
                AIRequest request = new AIRequest.Builder()
                        .query(message)
                        .prompt(fullPrompt)
                        .history(history)
                        .build();

                AIResponse response = provider.processRequest(serviceConfig, request);

                long elapsed = System.currentTimeMillis() - startTime;

                if (response == null) {
                    LOGGER.warn("[AI-DEBUG] <<< NULL | provider={} time={}ms - AI provider returned null response", providerId, elapsed);
                    callback.execute(new AIResponse("AI service returned no response.", 500));
                    return;
                }

                if (!response.isSuccess()){
                    if (debug) {
                        LOGGER.warn("[AI-DEBUG] <<< FAILED | provider={} code={} time={}ms response=\"{}\"",
                                providerId, response.getCode(), elapsed,
                                response.getResponse() != null ? response.getResponse().substring(0, Math.min(200, response.getResponse().length())) : "(null)");
                    }
                    callback.execute(new AIResponse("服务器繁忙，请稍后再试。", response.getCode()));
                    return;
                }

                // 保存对话历史（统一由调用方落盘，所有 provider 一致）
                // 主动发言/内部触发消息用自定义前缀（如 [提示]对X：），普通玩家消息用 [说话人] 前缀
                String historyMsg = historyPrefix != null
                        ? historyPrefix + message
                        : "[" + speakerName + "] " + message;
                FileStorageUtil.saveConversation(uuidStr, userUuidStr, historyMsg, response.getResponse());

                if (debug) {
                    String respPreview = response.getResponse();
                    if (respPreview != null && respPreview.length() > 150) {
                        respPreview = respPreview.substring(0, 150) + "...";
                    }
                    LOGGER.info("[AI-DEBUG] <<< SUCCESS | provider={} code={} time={}ms resp({}c)=\"{}\"",
                            providerId, response.getCode(), elapsed,
                            response.getResponse() != null ? response.getResponse().length() : 0,
                            respPreview);
                }
                callback.execute(response);
            }catch (AIException e){
                // 库层统一错误：超时/认证/限流/网络/解析等
                long elapsed = System.currentTimeMillis() - startTime;
                LOGGER.warn("[AI-DEBUG] <<< AI ERROR | type={} code={} time={}ms error=\"{}\"",
                        e.getType(), e.getStatusCode(), elapsed, e.getMessage());
                if (debug) {
                    LOGGER.error("[AI-DEBUG] AI exception details:", e);
                }
                callback.execute(new AIResponse("服务器繁忙，请稍后再试。",
                        e.getStatusCode() != 0 ? e.getStatusCode() : 500));
            }catch (Exception e){
                long elapsed = System.currentTimeMillis() - startTime;
                LOGGER.warn("[AI-DEBUG] <<< EXCEPTION | time={}ms error=\"{}\"", elapsed, e.toString());
                if (debug) {
                    LOGGER.error("[AI-DEBUG] Exception details:", e);
                }
                callback.execute(new AIResponse("AI request failed: " + e.getMessage(), 500));
            }
        });

        // 设置超时机制
        executor.submit(() -> {
            try {
                future.get(REQUEST_TIMEOUT, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                LOGGER.warn("[AI-DEBUG] <<< TIMEOUT | exceeded {}s for msg=\"{}\"", REQUEST_TIMEOUT, msgSnippet);
            } catch (Exception e) {
                LOGGER.error("Unexpected error during message sending task.", e);
            }
        });
    }


    /**
     * 长对话自动总结：当历史条数达到最大会话长度（ai.max_history）时，
     * 把最早的 N 条（ai.summary.count）对话发送给 AI 总结为一条摘要，
     * 替换进历史并写回文件。例如 max_history=70、count=50 时：
     * 70 条历史 → 1 条摘要 + 保留后 20 条。
     * 总结失败时静默跳过，不阻塞本次请求。
     */
    private static AIHistory summarizeHistory(String nekoStorageId, String userUuidStr, AIHistory history) {
        if (!ConfigUtil.isAISummaryEnabled() || history == null) return history;
        int maxHistory = ConfigUtil.getAIMaxHistory();
        int count = ConfigUtil.getAISummaryCount();
        List<Content> contents = history.getContents();
        if (maxHistory <= 0 || count < 2 || contents.size() < maxHistory) return history;

        List<Content> toSummarize = new ArrayList<>(contents.subList(0, Math.min(count, contents.size())));
        String summary = requestSummary(toSummarize);
        if (summary == null || summary.isEmpty()) return history;

        // 新历史 = 摘要 + 剩余对话
        List<Content> remaining = contents.size() > count
                ? new ArrayList<>(contents.subList(count, contents.size()))
                : new ArrayList<>();
        List<Content> newContents = new ArrayList<>();
        newContents.add(Content.create(Content.Role.USER,
                LanguageUtil.translatable("misc.toneko.ai.summary.prefix") + summary));
        newContents.addAll(remaining);
        history.setContents(newContents);

        // 写回存储文件
        try {
            Path file = Paths.get(FileStorageUtil.getBasePath(), nekoStorageId, userUuidStr + ".json");
            Files.writeString(file, history.toJson());
            LOGGER.info("[AI-SUMMARY] summarized {} messages for {}", count, userUuidStr);
        } catch (IOException e) {
            LOGGER.warn("Failed to save summarized AI history for {}: {}", userUuidStr, e.getMessage());
        }
        return history;
    }

    /**
     * 请求 AI 对指定对话片段生成摘要（使用当前配置的 provider，不保存到会话历史）。
     */
    private static String requestSummary(List<Content> toSummarize) {
        try {
            String providerId = resolveProviderId(ConfigUtil.getAIService());
            if (providerId == null) return null;
            AIServiceProvider provider = AIServiceProviderRegistry.get(providerId);
            if (provider == null) return null;
            AIServiceConfig serviceConfig = ConfigUtil.buildAIServiceConfig(providerId);

            // 把对话片段按角色拼成文本
            StringBuilder sb = new StringBuilder();
            for (Content c : toSummarize) {
                String role = c.getRole() == Content.Role.MODEL
                        ? LanguageUtil.translatable("misc.toneko.ai.summary.role.neko")
                        : LanguageUtil.translatable("misc.toneko.ai.summary.role.player");
                String text = c.getParts() != null
                        ? c.getParts().stream().map(Content.Part::getText).reduce("", String::concat)
                        : "";
                sb.append(role).append(": ").append(text).append('\n');
            }

            AIRequest request = new AIRequest.Builder()
                    .query(LanguageUtil.translatable("misc.toneko.ai.summary.prompt") + "\n" + sb)
                    .build();
            AIResponse response = provider.processRequest(serviceConfig, request);
            if (response != null && response.isSuccess()) {
                return response.getResponse().trim();
            }
            LOGGER.warn("[AI-SUMMARY] request failed: code={}",
                    response != null ? response.getCode() : "null");
        } catch (Exception e) {
            LOGGER.warn("[AI-SUMMARY] exception: {}", e.toString());
        }
        return null;
    }

    /**
     * 按配置的最大会话长度裁剪历史：保留最近 N 条消息，超出部分移除，
     * 并把裁剪结果写回存储文件（同时控制请求体与文件大小）。
     * 配置为 0 或负数时不裁剪。
     */
    private static AIHistory trimHistory(String nekoStorageId, String userUuidStr, AIHistory history) {
        int maxCount = ConfigUtil.getAIMaxHistory();
        if (maxCount <= 0 || history == null) return history;
        List<Content> contents = history.getContents();
        if (contents.size() <= maxCount) return history;

        contents.subList(0, contents.size() - maxCount).clear();
        try {
            Path file = Paths.get(FileStorageUtil.getBasePath(), nekoStorageId, userUuidStr + ".json");
            Files.writeString(file, history.toJson());
        } catch (IOException e) {
            LOGGER.warn("Failed to trim AI history for {}: {}", userUuidStr, e.getMessage());
        }
        return history;
    }


    @FunctionalInterface
    public interface MessageCallback {
        void execute(AIResponse message);
    }

    public static void playTTS(String text, String voice) {
        var future = executor.submit(() -> {
            try {
                // Elefant的tts
                var body = new ElefantTTSRequestBody();
                body.text = text;
                body.voiceIds.add(voice);

                HttpClient client = new HttpClient();
                CompletableFuture<String> cf = client.sendPost(
                        "http://127.0.0.1:4315/v1/tts/speak",
                        body,
                        String.class
                );

                cf.whenComplete((response, ex) -> {
                    if (ex != null) {
                        LOGGER.error("Request failed: {}", ex.getMessage());
                    }
                    client.close();
                });

                cf.join();
            } catch (Exception e) {
                LOGGER.error("Unexpected error during message sending task.", e);
            }
        });

        // 设置超时机制
        executor.submit(() -> {
            try {
                future.get(REQUEST_TIMEOUT, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                LOGGER.warn("TTS sending task timed out and was cancelled.");
            } catch (Exception e) {
                LOGGER.error("Unexpected error during message sending task.", e);
            }
        });
    }

    private static class ElefantTTSRequestBody{
        public static final Gson gson = new Gson();
        @SerializedName("play_in_app")
        private boolean playInApp = true;
        @SerializedName("speed")
        private int speed = 1;
        @SerializedName("text")
        private String text = "";
        @SerializedName("voice_ids")
        private List<String> voiceIds = new ArrayList<>();
    }
}
