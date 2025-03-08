package org.cneko.toneko.common.util;

import com.google.gson.Gson;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.CharsetUtil;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;

public class HttpClient {
    private final Gson gson;
    private final EventLoopGroup group;
    private final boolean autoClose;

    public HttpClient() {
        this(new Gson());
    }

    public HttpClient(Gson gson) {
        this(gson, new NioEventLoopGroup(), true);
    }

    public HttpClient(Gson gson, EventLoopGroup group, boolean autoClose) {
        this.gson = gson;
        this.group = group;
        this.autoClose = autoClose;
    }

    public void close() {
        if (autoClose) {
            group.shutdownGracefully();
        }
    }

    public <T> CompletableFuture<T> sendPost(String url, Object body, Class<T> responseType) {
        CompletableFuture<T> future = new CompletableFuture<>();

        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort() == -1 ? (scheme.equals("https") ? 443 : 80) : uri.getPort();

            // 配置SSL
            boolean ssl = "https".equalsIgnoreCase(scheme);
            SslContext sslContext = null;
            if (ssl) {
                sslContext = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build();
            }

            // 创建Bootstrap
            Bootstrap bootstrap = new Bootstrap();
            SslContext finalSslContext = sslContext;
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            if (finalSslContext != null) {
                                p.addLast(finalSslContext.newHandler(ch.alloc(), host, port));
                            }
                            p.addLast(new HttpClientCodec());
                            p.addLast(new HttpObjectAggregator(1048576));
                            p.addLast(new HttpClientHandler((CompletableFuture<Object>) future, gson, responseType));
                        }
                    });

            // 构建请求
            String json = gson.toJson(body);
            ByteBuf content = Unpooled.copiedBuffer(json, CharsetUtil.UTF_8);
            FullHttpRequest request = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1, HttpMethod.POST, uri.getRawPath(), content);
            request.headers()
                    .set(HttpHeaderNames.HOST, host)
                    .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
                    .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                    .set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());

            // 连接并发送请求
            ChannelFuture connectFuture = bootstrap.connect(host, port);
            connectFuture.addListener((ChannelFutureListener) f -> {
                if (f.isSuccess()) {
                    f.channel().writeAndFlush(request);
                } else {
                    future.completeExceptionally(f.cause());
                }
            });

        } catch (URISyntaxException | SSLException e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    private static class HttpClientHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
        private final CompletableFuture<Object> future;
        private final Gson gson;
        private final Class<?> responseType;

        public HttpClientHandler(CompletableFuture<Object> future, Gson gson, Class<?> responseType) {
            this.future = future;
            this.gson = gson;
            this.responseType = responseType;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
            try {
                String content = msg.content().toString(CharsetUtil.UTF_8);

                // 处理非200状态码
                if (msg.status().code() != HttpResponseStatus.OK.code()) {
                    future.completeExceptionally(new HttpException(msg.status(), content));
                    return;
                }

                // 直接返回字符串内容
                if (responseType == String.class) {
                    future.complete(content);
                }
                // 处理原始字节数组
                else if (responseType == byte[].class) {
                    byte[] bytes = new byte[msg.content().readableBytes()];
                    msg.content().readBytes(bytes);
                    future.complete(bytes);
                }
                // 其他类型使用Gson转换
                else {
                    Object result = gson.fromJson(content, responseType);
                    future.complete(result);
                }
            } catch (Exception e) {
                future.completeExceptionally(e);
            } finally {
                ctx.close();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            future.completeExceptionally(cause);
            ctx.close();
        }
    }

    public static class HttpException extends RuntimeException {
        private final HttpResponseStatus status;
        private final String responseBody;

        public HttpException(HttpResponseStatus status, String responseBody) {
            super("HTTP Error " + status.code() + ": " + status.reasonPhrase());
            this.status = status;
            this.responseBody = responseBody;
        }

        // 获取状态码
        public int getStatusCode() {
            return status.code();
        }

        // 获取响应内容
        public String getResponseBody() {
            return responseBody;
        }
    }
}