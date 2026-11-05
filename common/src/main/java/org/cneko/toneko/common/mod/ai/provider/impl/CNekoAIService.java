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
 * POST /?p=prompt&t=message&key=key&model=model&ver=1，body 与 msg 头携带历史 JSON。
 */
public class CNekoAIService extends AbstractAIService<GeminiConfig> {

    private static final Gson gson = new Gson();
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

        return HttpRequest.newBuilder(buildUri("/?" + query))
                .header("Content-Type", "application/json")
                .header("msg", toAsciiJson(jsonBody))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();
    }

    /**
     * 把 JSON 里的非 ASCII 字符转成 {@code \\uXXXX} 转义。
     *
     * <p>{@link HttpRequest.Builder#header} 只接受 ISO-8859-1 范围内的头值，遇到中文等字符会直接抛
     * {@link IllegalArgumentException}（JDK 行为），导致历史里只要出现一次中文，后续请求就全部失败。
     * HTTP 头本身也不适合承载 UTF-8 文本：旧实现把 UTF-8 字节直接塞进头里，服务端按 Latin-1 解码后
     * 历史就成了乱码（#118）。
     *
     * <p>转义后的串仍是合法 JSON，服务端 {@code JSON.parse} 会还原成同样的字符串，且全部字符都是 ASCII。
     */
    private static String toAsciiJson(String json) {
        StringBuilder sb = new StringBuilder(json.length() + 16);
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c < 0x80) {
                sb.append(c);
            } else {
                sb.append(String.format("\\u%04x", (int) c));
            }
        }
        return sb.toString();
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
