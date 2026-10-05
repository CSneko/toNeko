package org.cneko.toneko.common.mod.ai.provider.impl;

import com.google.gson.Gson;
import org.cneko.ai.core.AIException;
import org.cneko.ai.core.AIHistory;
import org.cneko.ai.core.AIRequest;
import org.cneko.ai.core.AIResponse;
import org.cneko.ai.providers.AbstractAIService;
import org.cneko.ai.providers.gemini.GeminiConfig;
import org.cneko.toneko.common.mod.ai.AIServiceConfig;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Custom CNeko AI service that communicates with chat.ai.cneko.org.
 * 基于 JDK HttpClient（NekoAI v0.2.0），协议与旧版一致：
 * POST /?p=prompt&t=message&key=key&model=model&ver=1，body 携带历史 JSON；
 * msg 头只是给老服务端的兼容副本，值不合法时会跳过（见 buildRequest 注释）。
 */
public class CNekoAIService extends AbstractAIService<GeminiConfig> {

    private static final Gson gson = new Gson();
    /** HTTP header 值的安全上限：超过这个长度没必要（也不可能）塞进 header */
    private static final int MAX_HEADER_VALUE_LENGTH = 2048;
    private final AIServiceConfig serviceConfig;

    public CNekoAIService(GeminiConfig config, AIServiceConfig serviceConfig) {
        super(config);
        this.serviceConfig = serviceConfig;
    }

    @Override
    protected HttpRequest buildRequest(AIRequest request) {
        AIHistory history = buildHistory(request);
        String jsonBody = history.toJson();

        String msg = request.getQuery().replace("&", "");
        String encodedPrompt = URLEncoder.encode(request.getPrompt() != null ? request.getPrompt() : "无提示词", StandardCharsets.UTF_8);
        String encodedMessage = URLEncoder.encode(msg, StandardCharsets.UTF_8);
        String encodedKey = URLEncoder.encode(serviceConfig.getApiKey(), StandardCharsets.UTF_8);
        String encodeModel = URLEncoder.encode(config.getModel(), StandardCharsets.UTF_8);
        String query = String.format("p=%s&t=%s&key=%s&model=%s&ver=1", encodedPrompt, encodedMessage, encodedKey, encodeModel);

        HttpRequest.Builder builder = HttpRequest.newBuilder(buildUri("/?" + query))
                .header("Content-Type", "application/json");
        // JDK 的 HttpRequest 会严格校验 header 值：含非 ASCII（中文人设/聊天记录）或控制字符时
        // 直接抛 IllegalArgumentException，请求还没发出去就失败，异常信息还会把整包 prompt
        // 带进日志刷屏。历史 JSON 里几乎必然有中文且动辄上万字节，所以只在值合法时才带 msg 头，
        // 用于兼容仍在读该头的老服务端；不合法时静默跳过，body 里已经有一份完整 JSON。
        if (isHeaderSafe(jsonBody)) {
            builder.header("msg", jsonBody);
        }
        return builder
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();
    }

    /** 值必须是可打印 ASCII 且长度可控，才允许作为 HTTP header 值 */
    private static boolean isHeaderSafe(String value) {
        if (value == null || value.isEmpty() || value.length() > MAX_HEADER_VALUE_LENGTH) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c <= 0x1F || c >= 0x7F) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected AIResponse parseResponse(AIRequest request, HttpResponse<String> response) throws AIException {
        try {
            CnekoResponse responseObj = gson.fromJson(response.body(), CnekoResponse.class);
            if (responseObj == null || responseObj.response == null) {
                throw new AIException(AIException.ErrorType.PARSE, "No response field in CNeko response", 200);
            }
            String responseText = responseObj.response.replace("\\n", "");
            return new AIResponse(responseText.trim(), 200);
        } catch (RuntimeException e) {
            throw new AIException(AIException.ErrorType.PARSE, "Response parsing error: " + e.getMessage(), 200, e);
        }
    }

    public static class CnekoResponse {
        public String response;
    }
}
