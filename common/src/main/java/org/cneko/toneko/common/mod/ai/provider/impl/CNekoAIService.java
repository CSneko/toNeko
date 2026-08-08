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
                .header("msg", jsonBody)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();
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
