package org.cneko.toneko.common.util;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.cneko.ai.core.AIHistory;
import org.cneko.ai.core.AIRequest;
import org.cneko.ai.core.AIResponse;
import org.cneko.ai.util.FileStorageUtil;
import org.cneko.toneko.common.mod.ai.AIServiceConfig;
import org.cneko.toneko.common.mod.ai.provider.AIServiceProvider;
import org.cneko.toneko.common.mod.ai.provider.AIServiceProviderRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.cneko.toneko.common.Bootstrap.LOGGER;

public class AIUtil {
    private static final ExecutorService executor = Executors.newFixedThreadPool(100, r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true); // 将线程设为守护线程
        return thread;
    });
    private static final int MAX_MESSAGE_COUNT = 30;
    private static final int REQUEST_TIMEOUT = 60;

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
        result.endpoint = hostPortAndPath.length > 1 ? "/" + hostPortAndPath[1] : "/";

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

    public static void sendMessage(UUID uuid, UUID userUuid, String prompt, String message, MessageCallback callback){
        final boolean debug = ConfigUtil.isAIDebugEnabled();
        final long startTime = System.currentTimeMillis();
        final String msgSnippet = message.length() > 80 ? message.substring(0, 80) + "..." : message;

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
                            .prompt(prompt)
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
                            .prompt(prompt)
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

                String uuidStr = uuid.toString();
                String userUuidStr = userUuid.toString();

                AIHistory history = FileStorageUtil.readConversation(uuidStr, userUuidStr);
                AIRequest request = new AIRequest(message, uuidStr, userUuidStr, prompt, history);

                AIResponse response = provider.processRequest(serviceConfig, request);

                long elapsed = System.currentTimeMillis() - startTime;

                if (response != null) {
                    if (!response.isSuccess()){
                        if (debug) {
                            LOGGER.warn("[AI-DEBUG] <<< FAILED | provider={} code={} time={}ms response=\"{}\"",
                                    providerId, response.getCode(), elapsed,
                                    response.getResponse() != null ? response.getResponse().substring(0, Math.min(200, response.getResponse().length())) : "(null)");
                        }
                        response.setResponse("服务器繁忙，请稍后再试。");
                    } else if (debug) {
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
                } else {
                    LOGGER.warn("[AI-DEBUG] <<< NULL | provider={} time={}ms - AI provider returned null response", providerId, elapsed);
                    callback.execute(new AIResponse("AI service returned no response.", 500));
                }
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
