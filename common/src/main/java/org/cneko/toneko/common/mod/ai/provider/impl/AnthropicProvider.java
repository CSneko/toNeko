package org.cneko.toneko.common.mod.ai.provider.impl;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.cneko.ai.core.AIException;
import org.cneko.ai.core.AIHistory;
import org.cneko.ai.core.AIRequest;
import org.cneko.ai.core.AIResponse;
import org.cneko.ai.providers.AbstractAIService;
import org.cneko.ai.providers.openai.OpenAIConfig;
import org.cneko.toneko.common.mod.ai.AIServiceConfig;
import org.cneko.toneko.common.mod.ai.provider.AIServiceProvider;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Anthropic Claude provider.
 * Uses the Anthropic Messages API (not OpenAI-compatible).
 * Auth via x-api-key header, requires anthropic-version header.
 */
public class AnthropicProvider implements AIServiceProvider {

    @Override
    public String getProviderId() { return "claude"; }

    @Override
    public String getDisplayName() { return "Anthropic Claude"; }

    @Override
    public boolean isOpenAICompatible() { return false; }

    @Override
    public boolean requiresApiKey() { return true; }

    @Override
    public String getDefaultHost() { return "api.anthropic.com"; }

    @Override
    public int getDefaultPort() { return 443; }

    @Override
    public String getDefaultEndpoint() { return "/v1/messages"; }

    @Override
    public boolean isDefaultTls() { return true; }

    @Override
    public String getDefaultModel() { return "claude-sonnet-4-20250514"; }

    @Override
    public AIResponse processRequest(AIServiceConfig config, AIRequest request) throws Exception {
        OpenAIConfig openAIConfig = OpenAIProvider.applyCommon(
                new OpenAIConfig(config.getApiKey()).withModel(config.getModel()), config, getDefaultHost());
        AnthropicService service = new AnthropicService(openAIConfig);
        return service.processRequest(request);
    }

    /**
     * Custom service for Anthropic Messages API, based on JDK HttpClient (NekoAI v0.2.0).
     */
    static class AnthropicService extends AbstractAIService<OpenAIConfig> {
        private static final Gson gson = new Gson();
        private static final String ANTHROPIC_VERSION = "2023-06-01";

        public AnthropicService(OpenAIConfig config) {
            super(config);
        }

        @Override
        protected HttpRequest buildRequest(AIRequest request) {
            // 从 AIHistory 提取消息（强类型遍历，无需 JSON 解析）
            List<AnthropicMessage> messages = new ArrayList<>();
            AIHistory history = request.getHistory();
            if (history != null && history.getContents() != null) {
                for (AIHistory.Content content : history.getContents()) {
                    String role;
                    switch (content.getRole()) {
                        case USER: role = "user"; break;
                        case MODEL: role = "assistant"; break;
                        default: continue; // SYSTEM 角色通过顶层 system 字段传递
                    }
                    String text = content.getParts() != null
                            ? content.getParts().stream()
                                    .map(AIHistory.Content.Part::getText)
                                    .reduce("", String::concat)
                            : "";
                    if (text.isEmpty()) continue;
                    // Anthropic 要求 user/assistant 角色交替，合并连续相同角色的消息
                    if (!messages.isEmpty() && messages.get(messages.size() - 1).role.equals(role)) {
                        messages.get(messages.size() - 1).content += "\n" + text;
                    } else {
                        messages.add(new AnthropicMessage(role, text));
                    }
                }
            }
            // 添加当前用户 query（若历史最后一条是 user 也会被合并）
            messages.add(new AnthropicMessage("user", request.getQuery()));

            AnthropicRequestBody body = new AnthropicRequestBody();
            body.model = resolveModel(request);
            body.maxTokens = 1024;
            body.messages = messages;
            if (request.getPrompt() != null && !request.getPrompt().isEmpty()) {
                body.system = request.getPrompt();
            }

            String jsonBody = gson.toJson(body);
            return HttpRequest.newBuilder(buildUri(config.getEndpoint()))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", config.getApiKey())
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();
        }

        @Override
        protected AIResponse parseResponse(AIRequest request, HttpResponse<String> response) throws AIException {
            try {
                AnthropicResponseBody responseObj = gson.fromJson(response.body(), AnthropicResponseBody.class);
                StringBuilder responseText = new StringBuilder();
                if (responseObj != null && responseObj.content != null) {
                    for (AnthropicContentBlock block : responseObj.content) {
                        if ("text".equals(block.type) && block.text != null) {
                            responseText.append(block.text);
                        }
                    }
                }

                String finalText = responseText.toString().trim();
                if (finalText.isEmpty()) {
                    throw new AIException(AIException.ErrorType.PARSE, "Claude returned empty response", 200);
                }
                return new AIResponse(finalText, 200);
            } catch (RuntimeException e) {
                throw new AIException(AIException.ErrorType.PARSE, "Response parsing error: " + e.getMessage(), 200, e);
            }
        }

        // --- Anthropic API data classes ---

        static class AnthropicRequestBody {
            String model;
            @SerializedName("max_tokens")
            int maxTokens;
            List<AnthropicMessage> messages;
            String system;
        }

        static class AnthropicMessage {
            String role;
            String content;

            public AnthropicMessage(String role, String content) {
                this.role = role;
                this.content = content;
            }
        }

        static class AnthropicResponseBody {
            List<AnthropicContentBlock> content;
        }

        static class AnthropicContentBlock {
            String type;
            String text;
        }
    }
}
