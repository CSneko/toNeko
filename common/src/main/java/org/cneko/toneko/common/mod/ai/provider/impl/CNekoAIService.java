package org.cneko.toneko.common.mod.ai.provider.impl;

import com.google.gson.Gson;
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
import org.cneko.ai.providers.gemini.GeminiConfig;
import org.cneko.ai.util.FileStorageUtil;
import org.cneko.toneko.common.mod.ai.AIServiceConfig;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.cneko.toneko.common.Bootstrap.LOGGER;

/**
 * Custom CNeko AI service that communicates with chat.ai.cneko.org.
 * Extracted from AIUtil to standalone file.
 */
public class CNekoAIService extends AbstractNettyAIService<GeminiConfig> {

    private static final Gson gson = new Gson();
    private final AIServiceConfig serviceConfig;

    public CNekoAIService(GeminiConfig config, AIServiceConfig serviceConfig) throws Exception {
        super(config);
        this.serviceConfig = serviceConfig;
    }

    @Override
    protected void initChannel(SocketChannel ch, AIRequest request, CompletableFuture<AIResponse> future) {
        configurePipeline(ch, new CnekoHandler(request, future));
    }

    @Override
    protected void sendRequest(Channel channel, AIRequest request) {
        AIHistory history = prepareHistory(request);
        String jsonBody = history.toJson();
        byte[] bodyBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = Unpooled.wrappedBuffer(bodyBytes);

        String msg = request.getQuery().replace("&", "");
        String encodedPrompt = URLEncoder.encode(request.getPrompt() != null ? request.getPrompt() : "无提示词", StandardCharsets.UTF_8);
        String encodedMessage = URLEncoder.encode(msg, StandardCharsets.UTF_8);
        String encodedKey = URLEncoder.encode(serviceConfig.getApiKey(), StandardCharsets.UTF_8);
        String encodeModel = URLEncoder.encode(config.getModel(), StandardCharsets.UTF_8);
        String query = String.format("p=%s&t=%s&key=%s&model=%s&ver=1", encodedPrompt, encodedMessage, encodedKey, encodeModel);

        String encodedJsonBody = URLEncoder.encode(jsonBody, StandardCharsets.UTF_8);

        FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.POST,
                "/?" + query,
                buf
        );
        httpRequest.headers()
                .set(HttpHeaderNames.HOST, config.getHost())
                .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                .set(HttpHeaderNames.CONTENT_LENGTH, buf.readableBytes())
                .set("msg", jsonBody);

        channel.writeAndFlush(httpRequest);
    }

    private static class CnekoHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
        private final AIRequest request;
        private final CompletableFuture<AIResponse> future;

        public CnekoHandler(AIRequest request, CompletableFuture<AIResponse> future) {
            this.request = request;
            this.future = future;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) {
            String content = response.content().toString(StandardCharsets.UTF_8);
            int code = response.status().code();
            if (code != HttpResponseStatus.OK.code()) {
                future.complete(new AIResponse("API Error: " + content, code));
                return;
            }

            try {
                CnekoResponse responseObj = gson.fromJson(content, CnekoResponse.class);
                String responseText = responseObj.response;
                responseText = responseText.replace("\\n", "");

                try {
                    FileStorageUtil.saveConversation(
                            request.getSessionId(),
                            request.getUserId(),
                            request.getQuery(),
                            responseText
                    );
                } catch (Exception e) {
                    NekoLogger.LOGGER.error("Error saving conversation: {}", e.getMessage());
                }

                future.complete(new AIResponse(responseText.trim(), code));
            } catch (Exception e) {
                future.complete(new AIResponse("Response parsing error: " + e.getMessage(), code));
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            future.complete(new AIResponse("Network error: " + cause.getMessage(), 400));
            ctx.close();
        }

        public static class CnekoResponse {
            public String response;
        }
    }
}
