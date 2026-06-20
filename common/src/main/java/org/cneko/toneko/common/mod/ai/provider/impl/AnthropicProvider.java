package org.cneko.toneko.common.mod.ai.provider.impl;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import org.cneko.ai.NekoLogger;
import org.cneko.ai.core.AIHistory;
import org.cneko.ai.core.AIRequest;
import org.cneko.ai.core.AIResponse;
import org.cneko.ai.providers.AbstractNettyAIService;
import org.cneko.ai.providers.openai.OpenAIConfig;
import org.cneko.ai.util.FileStorageUtil;
import org.cneko.toneko.common.mod.ai.AIServiceConfig;
import org.cneko.toneko.common.mod.ai.provider.AIServiceProvider;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
        OpenAIConfig openAIConfig = new OpenAIConfig(config.getApiKey());
        openAIConfig.setModel(config.getModel());
        OpenAIProvider.applyCommon(openAIConfig, config, getDefaultHost());
        AnthropicService service = new AnthropicService(openAIConfig, config);
        return service.processRequest(request);
    }

    /**
     * Custom service for Anthropic Messages API.
     */
    static class AnthropicService extends AbstractNettyAIService<OpenAIConfig> {
        private static final Gson gson = new Gson();
        private static final String ANTHROPIC_VERSION = "2023-06-01";
        private final AIServiceConfig serviceConfig;

        public AnthropicService(OpenAIConfig config, AIServiceConfig serviceConfig) throws Exception {
            super(config);
            this.serviceConfig = serviceConfig;
        }

        @Override
        protected void initChannel(SocketChannel ch, AIRequest request, CompletableFuture<AIResponse> future) {
            configurePipeline(ch, new AnthropicHandler(request, future, serviceConfig));
        }

        @Override
        protected void sendRequest(Channel channel, AIRequest request) {
            AIHistory history = prepareHistory(request);

            // Build Anthropic-format messages from conversation history JSON
            List<AnthropicMessage> messages = new ArrayList<>();
            if (history != null) {
                // Parse AIHistory JSON to extract messages (Gemini or OpenAI format)
                messages.addAll(parseHistory(history.toJson()));
            }
            // Add the current user query
            messages.add(new AnthropicMessage("user", request.getQuery()));

            // Build request body
            AnthropicRequestBody body = new AnthropicRequestBody();
            body.model = config.getModel();
            body.maxTokens = 1024;
            body.messages = messages;
            if (request.getPrompt() != null && !request.getPrompt().isEmpty()) {
                body.system = request.getPrompt();
            }

            String jsonBody = gson.toJson(body);
            byte[] bodyBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            ByteBuf buf = Unpooled.wrappedBuffer(bodyBytes);

            FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1,
                    HttpMethod.POST,
                    config.getEndpoint(),
                    buf
            );
            httpRequest.headers()
                    .set(HttpHeaderNames.HOST, config.getHost())
                    .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                    .set(HttpHeaderNames.CONTENT_LENGTH, bodyBytes.length)
                    .set("x-api-key", serviceConfig.getApiKey())
                    .set("anthropic-version", ANTHROPIC_VERSION);

            channel.writeAndFlush(httpRequest);
        }

        /**
         * Parse AIHistory JSON (Gemini format: {"contents": [...]} or OpenAI format: {"messages": [...]})
         * and extract role/content pairs as AnthropicMessage list.
         */
        private static List<AnthropicMessage> parseHistory(String json) {
            List<AnthropicMessage> result = new ArrayList<>();
            try {
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                // Try Gemini format: contents[]
                if (root.has("contents")) {
                    JsonArray contents = root.getAsJsonArray("contents");
                    for (JsonElement e : contents) {
                        JsonObject entry = e.getAsJsonObject();
                        String role = entry.has("role") ? entry.get("role").getAsString() : "user";
                        if ("model".equals(role)) role = "assistant";
                        String text = "";
                        if (entry.has("parts")) {
                            JsonArray parts = entry.getAsJsonArray("parts");
                            if (!parts.isEmpty()) {
                                text = parts.get(0).getAsJsonObject().get("text").getAsString();
                            }
                        }
                        if (!text.isEmpty()) {
                            result.add(new AnthropicMessage(role, text));
                        }
                    }
                }
                // Try OpenAI format: messages[]
                else if (root.has("messages")) {
                    JsonArray msgs = root.getAsJsonArray("messages");
                    for (JsonElement e : msgs) {
                        JsonObject entry = e.getAsJsonObject();
                        String role = entry.get("role").getAsString();
                        String content = entry.get("content").getAsString();
                        result.add(new AnthropicMessage(role, content));
                    }
                }
            } catch (Exception ignored) {
                // If parsing fails, just use the current query without history
            }
            return result;
        }

        private static class AnthropicHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
            private final AIRequest request;
            private final CompletableFuture<AIResponse> future;
            private final AIServiceConfig serviceConfig;

            public AnthropicHandler(AIRequest request, CompletableFuture<AIResponse> future, AIServiceConfig serviceConfig) {
                this.request = request;
                this.future = future;
                this.serviceConfig = serviceConfig;
            }

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) {
                String content = response.content().toString(StandardCharsets.UTF_8);
                int code = response.status().code();
                if (code != HttpResponseStatus.OK.code()) {
                    // Try to extract error message from Anthropic error response
                    try {
                        JsonObject errorObj = JsonParser.parseString(content).getAsJsonObject();
                        if (errorObj.has("error")) {
                            JsonObject err = errorObj.getAsJsonObject("error");
                            String errMsg = err.has("message") ? err.get("message").getAsString() : content;
                            future.complete(new AIResponse("Anthropic Error: " + errMsg, code));
                            return;
                        }
                    } catch (Exception ignored) {}
                    future.complete(new AIResponse("API Error: " + content, code));
                    return;
                }

                try {
                    AnthropicResponseBody responseObj = gson.fromJson(content, AnthropicResponseBody.class);
                    StringBuilder responseText = new StringBuilder();
                    if (responseObj.content != null) {
                        for (AnthropicContentBlock block : responseObj.content) {
                            if ("text".equals(block.type) && block.text != null) {
                                responseText.append(block.text);
                            }
                        }
                    }

                    String finalText = responseText.toString().trim();
                    if (finalText.isEmpty()) {
                        future.complete(new AIResponse("[Claude returned empty response]", code));
                        return;
                    }

                    // Save conversation
                    try {
                        FileStorageUtil.saveConversation(
                                request.getSessionId(),
                                request.getUserId(),
                                request.getQuery(),
                                finalText
                        );
                    } catch (Exception e) {
                        NekoLogger.LOGGER.error("Error saving conversation: {}", e.getMessage());
                    }

                    future.complete(new AIResponse(finalText, code));
                } catch (Exception e) {
                    future.complete(new AIResponse("Response parsing error: " + e.getMessage(), code));
                }
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                future.complete(new AIResponse("Network error: " + cause.getMessage(), 400));
                ctx.close();
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
