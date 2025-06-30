package org.cneko.toneko.common.util.network;

import com.google.gson.JsonObject;
import org.cneko.toneko.common.util.JsonConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class HttpBase {
    enum HttpMethod {
        GET, POST, PUT, DELETE, HEAD, OPTIONS, TRACE, CONNECT
    }
    public abstract static class HttpBaseObject {
        private Map<String, String> cookies;
        protected String url;
        protected Map<String, String> headers;
        protected String response;
        protected int responseCode = 0;
        protected String responseHeaders;
        protected HttpMethod method;
        private Thread asyncThread;

        private String jumpUrl;
        private boolean autoJump = false;

        public HttpBaseObject(@NotNull String url, @Nullable Map<String, String> headers) {
            this.url = url;
            this.headers = headers;
            this.method = getMethod();
            this.cookies = new ConcurrentHashMap<>();
        }
        public HttpBaseObject(@NotNull String url){
            this(url, new ConcurrentHashMap<>());
        }

        /**
         * 获取http请求方法
         */
        public abstract HttpMethod getMethod() ;

        /**
         * 发送HTTP请求并获取响应结果
         *
         * @throws IOException 如果发生I/O错误则抛出异常
         */
        public void connect() throws IOException {
            URL u = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setRequestMethod(method.toString());

            // 应用Cookie
            for (Map.Entry<String, String> entry : cookies.entrySet()) {
                conn.setRequestProperty("Cookie", entry.getKey() + "=" + entry.getValue());
            }

            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            // 获取响应内容
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null) {
                response.append(line);
            }

            in.close();
            this.response = response.toString();

            // 保存响应头
            Map<String, java.util.List<String>> map = conn.getHeaderFields();
            StringBuilder headers = new StringBuilder();
            for (Map.Entry<String, java.util.List<String>> entry : map.entrySet()) {
                headers.append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
            }
            this.responseHeaders = headers.toString();
        }


        /**
         * 设置url
         *
         * @param url 要设置的url
         */
        public void setUrl(String url) {
            this.url = url;
        }

        /**
         * 设置请求头
         *
         * @param headers 要设置的请求头
         */
        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }

        /**
         * 获取响应文本
         *
         * @return 响应文本，如果请求失败则返回null
         */
        public String getResponse() {
            return response;
        }

        /**
         * 获取响应头
         *
         * @return 响应头字符串，如果请求失败则返回null
         */
        public String getResponseHeaders(){
            return responseHeaders;
        }

        /**
         * 获取响应头
         * @return 响应头JsonConfiguration
         */
        public JsonConfiguration getResponseHeadersJson(){
            // 创建一个新的 JSON 对象
            JsonObject jsonObject = new JsonObject();
            // 按行分割响应头字符串
            String[] lines = responseHeaders.split("\n");
            // 遍历每一行，将其按冒号分割为键值对，并添加到 JSON 对象中
            for (String line : lines) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    jsonObject.addProperty(key, value);
                }
            }
            return new JsonConfiguration(jsonObject.toString());
        }

        /**
         * 获取状态码
         *
         * @return 状态码
         */
        public int getStatusCode(){
            return responseCode;
        }

        /**
         * 获取url
         *
         * @return url
         */
        public String getUrl(){
            return this.url;
        }

        /**
         * 获取请求头
         * @return 请求头
         */
        public Map<String,String> getHeaders(){
            return this.headers;
        }

        /**
         * 保存到文件
         *
         * @param path 文件保存的路径
         */
        public void saveToFile(Path path) throws IOException {
            if (response != null) {
                try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                    writer.write(response);
                }
            } else {
                throw new IllegalStateException("Response is null, cannot save to file.");
            }
        }
        /**
         * 在异步线程发送请求
         */
        public void asyncConnect() {
            asyncThread = new Thread(() -> {
                try {
                    connect();
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            });
            asyncThread.start();
        }

        /**
         * 等待异步连接完成
         */
        public void waitAsyncConnect() throws InterruptedException {
            if (asyncThread != null) {
                asyncThread.join(); // 等待异步线程完成
            } else {
                throw new IllegalStateException("No async connect operation has been started.");
            }
        }

        /**
         * 保存到文件
         *
         * @param file 文件保存的路径
         */
        public void saveToFile(File file) throws IOException{
            this.saveToFile(file.toPath());
        }

        /**
         * 获取Json
         *
         * @return JsonConfiguration对象
         */
        public JsonConfiguration getJson(){
            return new JsonConfiguration(this.response);
        }

        /**
         * 设置跳转URL
         */
        public void setJumpUrl() {
            this.jumpUrl = getJumpUrl();
        }

        /**
         * 获取跳转的URL
         * 如果是3xx状态码则返回重定向的URL，否则返回原始的URL
         * @return 跳转的URL
         */
        public String getJumpUrl() {
            if (responseCode >= 300 && responseCode < 400 && responseHeaders != null) {
                // 解析响应头中的重定向URL
                String locationHeader = getResponseHeadersJson().getString("Location");
                if (locationHeader != null) {
                    return locationHeader;
                }
            }
            return url;
        }

        /**
         * 执行跳转操作
         * 如果是3xx状态码则将url设置为跳转的URL并执行跳转
         */
        public void jump() throws IOException{
            if (responseCode >= 300 && responseCode < 400) {
                url = jumpUrl;
            }
            connect();
        }

        /**
         * 设置是否自动跳转
         * @param autoJump 是否自动跳转
         */
        public void setJump(boolean autoJump) {
            this.autoJump = autoJump;
        }

        /**
         * 设置Cookie
         */
        public void setCookie(Map<String,String> cookies) {
            this.cookies = cookies;
        }

        /**
         * 获取Cookie值
         * @return 对应的Cookie值，如果不存在则返回null
         */
        public Map<String,String> getCookie() {
            return cookies;
        }



    }
}