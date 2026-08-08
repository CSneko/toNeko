package org.cneko.toneko.common.util;

import com.google.gson.Gson;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 基于 JDK HttpClient 的轻量 HTTP 客户端（NekoAI v0.2.0 起不再依赖 netty）。
 * 对外 API 与旧版兼容：sendGet / sendPost 返回 CompletableFuture，非 200 时抛 HttpException。
 */
public class HttpClient {
    private final Gson gson;
    private final java.net.http.HttpClient client;

    public HttpClient() {
        this(new Gson());
    }

    public HttpClient(Gson gson) {
        this.gson = gson;
        this.client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** JDK HttpClient 无显式关闭，保留方法以兼容旧调用方 */
    public void close() {
    }

    public <T> CompletableFuture<T> sendPost(String url, Object body, Class<T> responseType) {
        try {
            String json = gson.toJson(body);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> parseResponse(response, responseType));
        } catch (IllegalArgumentException e) {
            return failedFuture(e);
        }
    }

    public <T> CompletableFuture<T> sendGet(String url, Map<String, String> queryParams, Class<T> responseType) {
        try {
            // 拼接查询参数
            String fullUrl = url;
            if (queryParams != null && !queryParams.isEmpty()) {
                StringBuilder queryBuilder = new StringBuilder();
                for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                    if (!queryBuilder.isEmpty()) {
                        queryBuilder.append('&');
                    }
                    String encodedKey = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
                    String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
                    queryBuilder.append(encodedKey).append('=').append(encodedValue);
                }
                fullUrl += (fullUrl.contains("?") ? "&" : "?") + queryBuilder;
            }

            HttpRequest request = HttpRequest.newBuilder(URI.create(fullUrl))
                    .GET()
                    .build();
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> parseResponse(response, responseType));
        } catch (IllegalArgumentException e) {
            return failedFuture(e);
        }
    }

    private <T> T parseResponse(HttpResponse<String> response, Class<T> responseType) {
        String content = response.body();

        // 处理非200状态码
        if (response.statusCode() != 200) {
            throw new HttpException(response.statusCode(), content);
        }

        // 直接返回字符串内容
        if (responseType == String.class) {
            return (T) content;
        }
        // 处理原始字节数组
        if (responseType == byte[].class) {
            return (T) content.getBytes(StandardCharsets.UTF_8);
        }
        // 其他类型使用Gson转换
        return gson.fromJson(content, responseType);
    }

    private <T> CompletableFuture<T> failedFuture(Throwable cause) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(cause);
        return future;
    }

    public static class HttpException extends RuntimeException {
        private final int status;
        private final String responseBody;

        public HttpException(int status, String responseBody) {
            super("HTTP Error " + status);
            this.status = status;
            this.responseBody = responseBody;
        }

        // 获取状态码
        public int getStatusCode() {
            return status;
        }

        // 获取响应内容
        public String getResponseBody() {
            return responseBody;
        }
    }
}
