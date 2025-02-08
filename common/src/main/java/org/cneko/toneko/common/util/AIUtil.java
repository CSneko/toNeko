package org.cneko.toneko.common.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import org.cneko.ai.NekoLogger;
import org.cneko.ai.core.AIHistory;
import org.cneko.ai.core.AIRequest;
import org.cneko.ai.core.AIResponse;
import org.cneko.ai.core.NetworkingProxy;
import org.cneko.ai.providers.AbstractNettyAIService;
import org.cneko.ai.providers.gemini.GeminiConfig;
import org.cneko.ai.providers.openai.OpenAIConfig;
import org.cneko.ai.providers.openai.OpenAIService;
import org.cneko.ai.util.FileStorageUtil;
import io.netty.handler.codec.http.*;
import org.cneko.ctlib.common.file.JsonConfiguration;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.cneko.toneko.common.Bootstrap.LOGGER;

public class AIUtil {
    public static final String API_URL = "https://chat.ai.cneko.org";
    private static final ExecutorService executor = Executors.newFixedThreadPool(100, r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true); // 将线程设为守护线程
        return thread;
    });
    private static final int MAX_MESSAGE_COUNT = 30;
    private static final int REQUEST_TIMEOUT = 60;

    public static void init(){
    }

    public static void sendMessage(UUID uuid,UUID userUuid, String prompt, String message, MessageCallback callback){
        var future = executor.submit(()->{
            try{
                // 获取AI模型
                var model = ConfigUtil.getAIModel();
                var key = ConfigUtil.getAIKey();
                var proxyIp = ConfigUtil.getAIProxyIp();
                var proxyPort = ConfigUtil.getAIProxyPort();
                var uuidStr = uuid.toString();
                var userUuidStr = userUuid.toString();
                // 判断是否使用代理
                boolean useProxy = ConfigUtil.isAIProxyEnabled();
                NetworkingProxy proxy = new NetworkingProxy(proxyIp, Integer.parseInt(proxyPort));
                if (model.contains("gemini")){
                    // gemini模型，使用CNekoAI的服务
                    var config = new GeminiConfig(key);
                    if (useProxy) {
                        config.setProxy(proxy);
                    }
                    config.setHost("chat.ai.cneko.org");
                    var service = new CNekoAIService(config);
                    AIResponse response = service.processRequest(new AIRequest(message,uuidStr,userUuidStr,prompt,FileStorageUtil.readConversation(uuidStr,userUuidStr)));
                    callback.execute(response);
                } else {
                    // 其他模型，使用groq的AI服务
                    var config = new OpenAIConfig(key);
                    if (useProxy) {
                        config.setProxy(proxy);
                    }
                    config.setModel(model);
                    config.setHost("api.groq.com");
                    config.setEndpoint("/openai/v1/chat/completions");
                    var service = new OpenAIService(config);
                    AIResponse response = service.processRequest(new AIRequest(message,uuidStr,userUuidStr,prompt,FileStorageUtil.readConversation(uuidStr,userUuidStr)));
                    callback.execute(response);
                }
            }catch (Exception e){
                LOGGER.warn("Failed to send message to AI service,{}",e.getMessage());
            }
        });

        // 设置超时机制
        executor.submit(() -> {
            try {
                future.get(REQUEST_TIMEOUT, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true); // 超时后取消任务
                LOGGER.warn("Message sending task timed out and was cancelled.");
            } catch (Exception e) {
                LOGGER.error("Unexpected error during message sending task.", e);
            }
        });
    }


    @FunctionalInterface
    public interface MessageCallback {
        void execute(AIResponse message);
    }

    public static class CNekoAIService extends AbstractNettyAIService<GeminiConfig> {
        public CNekoAIService(GeminiConfig config) throws Exception {
            super(config);
        }

        @Override
        protected void initChannel(SocketChannel ch, AIRequest request, CompletableFuture<AIResponse> future) {
            configurePipeline(ch, new CNekoAIService.CnekoHandler(request, future));
        }
        @Override
        protected void sendRequest(Channel channel, AIRequest request) {
            // 调用公共方法构造历史记录
            AIHistory history = prepareHistory(request);
            // Gemini 接口直接使用 AIHistory 的 JSON 表示
            String jsonBody = history.toJson();
            byte[] bodyBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            ByteBuf buf = Unpooled.wrappedBuffer(bodyBytes);

            // 删除掉url中的&
            String msg = request.getQuery().replace("&", "");
            // 构建查询参数
            String encodedPrompt = URLEncoder.encode(request.getPrompt() != null ? request.getPrompt() : "无提示词", StandardCharsets.UTF_8);
            String encodedMessage = URLEncoder.encode(msg, StandardCharsets.UTF_8);
            String encodedKey = URLEncoder.encode(ConfigUtil.getAIKey(), StandardCharsets.UTF_8);
            String query = String.format("p=%s&t=%s&key=%s&ver=1", encodedPrompt, encodedMessage, encodedKey);

            // 将 jsonBody 进行 URL 编码并放入请求头
            String encodedJsonBody = URLEncoder.encode(jsonBody, StandardCharsets.UTF_8);

            FullHttpRequest httpRequest = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1,
                    HttpMethod.POST,
                    "/?"+query,
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

                if (response.status().code() != HttpResponseStatus.OK.code()) {
                    future.complete(new AIResponse("API Error: " + content, false));
                    return;
                }

                try {
                    // 使用 GeminiResponse 类型替代 Map 解析
                    CnekoHandler.CnekoResponse responseObj = gson.fromJson(content, CnekoHandler.CnekoResponse.class);

                    // 获取文本内容
                    String responseText = responseObj.response;
                    // 删除句末的\n
                    responseText = responseText.replace("\\n", "");

                    // 保存会话记录（原有逻辑保持不变）
                    try {
                        FileStorageUtil.saveConversation(
                                request.getUserId(),
                                request.getSessionId(),
                                request.getQuery(),
                                responseText
                        );
                    } catch (Exception e) {
                        NekoLogger.LOGGER.error("Error saving conversation: {}", e.getMessage());
                    }

                    future.complete(new AIResponse(responseText.trim(), true));
                } catch (Exception e) {
                    future.complete(new AIResponse("Response parsing error: " + e.getMessage(), false));
                }
            }


            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                future.complete(new AIResponse("Network error: " + cause.getMessage(), false));
                ctx.close();
            }

            public static class CnekoResponse {
                public String response;
            }
        }
    }

    /*
    public static void sendMessage(UUID uuid,UUID userUuid, String prompt, String message, MessageCallback callback){
        var future = executor.submit(()->{
            try{
                FileUtil.CreatePath(PAST_MESSAGE_PATH + uuid + "/");
                String pastMessagePath = PAST_MESSAGE_PATH + uuid + "/" +userUuid + ".json";
                FileUtil.CreateFile(pastMessagePath);
                // 读取json
                String json = FileUtil.readStringFromFile(pastMessagePath);
                if (json.equalsIgnoreCase("")){
                    FileUtil.WriteFile(pastMessagePath, "{\"contents\":[]}");
                }
                JsonConfiguration j = JsonConfiguration.fromFile(Path.of(pastMessagePath));
                List<JsonConfiguration> contents = j.getJsonList("contents");
                // 检查是否超过了20
                if (contents.size() >= MAX_MESSAGE_COUNT){
                    // 删除第一个
                    contents.removeFirst();
                    // 重新构建json
                    j.set("contents", contents);
                }

                // 删除掉url中的&
                String  msg = message.replaceAll("&", "");
                // 构建查询参数
                String encodedPrompt = URLEncoder.encode(prompt, StandardCharsets.UTF_8);
                String encodedMessage = URLEncoder.encode(msg, StandardCharsets.UTF_8);
                String encodedKey = URLEncoder.encode(ConfigUtil.getAIKey(), StandardCharsets.UTF_8);
                String query = String.format("p=%s&t=%s&key=%s&ver=v1", encodedPrompt, encodedMessage, encodedKey);

                // 构建完整的url
                String url = API_URL + "?" + query;

                // 发送请求
                var post = new HttpPost.HttpPostObject(url,"","application/json");
                post.setHeaders(Map.of("msg",URLEncoder.encode(j.toString(), StandardCharsets.UTF_8)));
                post.connect();
                int statusCode = post.getStatusCode();
                if (statusCode == 200){
                    String response = post.getResponse();
                    JsonConfiguration resJson = JsonConfiguration.of(response);
                    // 读取返回消息
                    String resMsg = resJson.getString("response");
                    // 写入json
                    JsonConfiguration newJ = genNewJson(msg, resMsg);
                    contents.addAll(newJ.getJsonList("contents"));
                    j.set("contents", contents);
                    // 保存到文件
                    FileUtil.WriteFile(pastMessagePath, j.toString());
                    // 回调
                    callback.execute(resMsg.replace("\\n",""));
                }
            }catch (Exception e){
                LOGGER.error("Failed to send message", e);
            }
        });
        // 设置超时机制
        executor.submit(() -> {
            try {
                future.get(REQUEST_TIMEOUT, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true); // 超时后取消任务
                LOGGER.warn("Message sending task timed out and was cancelled.");
            } catch (Exception e) {
                LOGGER.error("Unexpected error during message sending task.", e);
            }
        });
    }

    private static @NotNull JsonConfiguration genNewJson(String msg, String resMsg) {
        String jsonString = """
                {"contents":[{
                    "role":"user",
                    "parts":[
                        {"text":"%s"}
                    ]
                },{
                    "role":"model",
                    "parts":[
                        {"text":"%s"}
                    ]
                }]}
                """;
        jsonString = String.format(jsonString, msg, resMsg);
        return new JsonConfiguration(jsonString);
    }
     */
}
