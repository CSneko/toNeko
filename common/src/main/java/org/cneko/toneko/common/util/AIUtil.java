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
import org.cneko.ai.providers.gemini.GeminiService;
import org.cneko.ai.providers.openai.OpenAIConfig;
import org.cneko.ai.providers.openai.OpenAIService;
import org.cneko.ai.util.FileStorageUtil;
import io.netty.handler.codec.http.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    }

    public static void sendMessage(UUID uuid,UUID userUuid, String prompt, String message, MessageCallback callback){
        var future = executor.submit(()->{
            try{
                // 获取AI模型
                var s = ConfigUtil.getAIService();
                var model = ConfigUtil.getAIModel();
                var key = ConfigUtil.getAIKey();
                var proxyIp = ConfigUtil.getAIProxyIp();
                var proxyPort = ConfigUtil.getAIProxyPort();
                var uuidStr = uuid.toString();
                var userUuidStr = userUuid.toString();
                // 判断是否使用代理
                boolean useProxy = ConfigUtil.isAIProxyEnabled();
                NetworkingProxy proxy = null;
                if (proxyPort!=null&&!proxyIp.isEmpty()){
                    proxy = new NetworkingProxy(proxyIp, Integer.parseInt(proxyPort));
                }else useProxy =false;
                AIResponse response = null;
                if (s.equalsIgnoreCase("neko")){
                    // CNekoAI的服务
                    var config = new GeminiConfig(key);
                    if (useProxy) {
                        config.setProxy(proxy);
                    }
                    config.setModel(model);
                    config.setHost("chat.ai.cneko.org");
                    var service = new CNekoAIService(config);
                    response = service.processRequest(new AIRequest(message,uuidStr,userUuidStr,prompt,FileStorageUtil.readConversation(uuidStr,userUuidStr)));
                }
                if (s.equalsIgnoreCase("google")){
                    // Google的服务
                    var config = new GeminiConfig(key);
                    if (useProxy) {
                        config.setProxy(proxy);
                    }
                    config.setModel(model);
                    var service = new GeminiService(config);
                    response = service.processRequest(new AIRequest(message,uuidStr,userUuidStr,prompt,FileStorageUtil.readConversation(uuidStr,userUuidStr)));
                } else if (s.equalsIgnoreCase("openai")){
                    // OpenAI的服务
                    var config = new OpenAIConfig(key);
                    if (useProxy) {
                        config.setProxy(proxy);
                    }
                    config.setModel(model);
                    var service = new OpenAIService(config);
                    response = service.processRequest(new AIRequest(message,uuidStr,userUuidStr,prompt,FileStorageUtil.readConversation(uuidStr,userUuidStr)));
                } else if (s.equalsIgnoreCase("groq")) {
                    // Groq的服务
                    var config = new OpenAIConfig(key);
                    if (useProxy) {
                        config.setProxy(proxy);
                    }
                    config.setModel(model);
                    config.setHost("api.groq.com");
                    config.setEndpoint("/openai/v1/chat/completions");
                    var service = new OpenAIService(config);
                    response = service.processRequest(new AIRequest(message,uuidStr,userUuidStr,prompt,FileStorageUtil.readConversation(uuidStr,userUuidStr)));
                }else if(s.equalsIgnoreCase("siliconflow")){
                    // siliconflow的服务
                    var config = new OpenAIConfig(key);
                    if (useProxy) {
                        config.setProxy(proxy);
                    }
                    config.setModel(model);
                    config.setHost("api.siliconflow.cn");
                    config.setEndpoint("/v1/chat/completions");
                    var service = new OpenAIService(config);
                    response = service.processRequest(new AIRequest(message,uuidStr,userUuidStr,prompt,FileStorageUtil.readConversation(uuidStr,userUuidStr)));
                } else if (s.equalsIgnoreCase("elefant")) {
                    // elefant的服务
                    var config = new OpenAIConfig("");
                    config.setHost("127.0.0.1");
                    config.setPort(4315);
                    config.setEndpoint("/v1/chat/completions");
                    config.setTls(false);
                    var service = new OpenAIService(config);
                    response = service.processRequest(new AIRequest(message,uuidStr,userUuidStr,prompt,FileStorageUtil.readConversation(uuidStr,userUuidStr)));
                } else if(!s.isEmpty()){
                    // OpenAI格式
                    // 把字符串分解为域名，端口（如果有）和endpoint（如果有）
                    var config = new OpenAIConfig(key);
                    // 如果是http开头
                    if (s.startsWith("http://")){
                        s = s.substring("http://".length());
                        config.setTls(false);
                    }else if (s.startsWith("https://")){
                        s = s.substring("https://".length());
                        config.setTls(true);
                    }
                    // 分割主机（和端口）部分与路径部分
                    String[] hostPortAndPath = s.split("/", 2);
                    String hostPortSection = hostPortAndPath[0];
                    String endpoint = hostPortAndPath.length > 1 ? "/" + hostPortAndPath[1] : "/";

                    // 处理主机和端口
                    String host;
                    Integer port = null;
                    int colonIndex = hostPortSection.indexOf(':');
                    if (colonIndex != -1) {
                        // 分离主机和端口
                        host = hostPortSection.substring(0, colonIndex);
                        try {
                            port = Integer.parseInt(hostPortSection.substring(colonIndex + 1));
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Invalid port number in URL: " + s);
                        }
                    } else {
                        host = hostPortSection;
                    }

                    // 设置配置
                    config.setHost(host);
                    if (port != null) {
                        config.setPort(port);
                    }
                    config.setEndpoint(endpoint);
                    if (useProxy) {
                        config.setProxy(proxy);
                    }
                    config.setModel(model);
                    var service = new OpenAIService(config);
                    response = service.processRequest(new AIRequest(message,uuidStr,userUuidStr,prompt,FileStorageUtil.readConversation(uuidStr,userUuidStr)));
                } else {
                    LOGGER.warn("Unsupported AI service: {} ,please read the docs: https://s.cneko.org/toNekoAI",s);
                    callback.execute(new AIResponse("Unsupported AI service: {} ,please read the docs: https://s.cneko.org/toNekoAI",400));
                }
                if (response != null) {
                    if (!response.isSuccess()){
                        response.setResponse("服务器繁忙，请稍后再试。");
                    }
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
            String encodeModel = URLEncoder.encode(config.getModel(), StandardCharsets.UTF_8);
            String query = String.format("p=%s&t=%s&key=%s&model=%s&ver=1", encodedPrompt, encodedMessage, encodedKey, encodeModel);

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
                int code = response.status().code();
                if (code != HttpResponseStatus.OK.code()) {
                    future.complete(new AIResponse("API Error: " + content, code));
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
}
