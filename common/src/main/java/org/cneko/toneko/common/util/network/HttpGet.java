package org.cneko.toneko.common.util.network;

import org.cneko.toneko.common.util.JsonConfiguration;
import org.cneko.toneko.common.util.network.HttpBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class HttpGet {
    public static class SimpleHttpGet {
        /**
         * 发送GET请求并获取响应结果
         * @param url 请求的URL地址
         * @param headers 请求头参数，可以为null
         * @return 响应结果字符串，如果请求失败则返回null
         * @throws IOException 如果发生I/O错误则抛出异常
         */
        public static String get(@NotNull String url, @Nullable Map<String, String> headers) throws IOException {
            URL u = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setRequestMethod("GET");

            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = in.readLine()) != null) {
                    response.append(line);
                }

                in.close();
                return response.toString();
            }

            return null;
        }

        /**
         * 新建线程发送GET请求并获取响应结果
         * @param url 请求的URL地址
         * @param headers 请求头参数，可以为null
         */
        public static void AsyncGet(@NotNull String url, @Nullable Map<String, String> headers) {
            new Thread(() -> {
                try {
                    get(url, headers);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }).start();
        }

        /**
         * 发送GET请求并获取响应结果
         * @param url 请求的URL地址
         * @param headers 请求头参数，可以为null
         * @param defaultValue 默认值，在请求失败或返回结果为null时返回该值
         * @return 响应结果字符串，如果请求失败或返回结果为null则返回defaultValue
         */
        public static String getString(@NotNull String url, @Nullable Map<String, String> headers, String defaultValue) {
            try {
                String response = get(url, headers);
                return response != null ? response : defaultValue;
            } catch (IOException e) {
                return defaultValue;
            }
        }


        /**
         * 发送GET请求并获取响应结果，返回的结果为JsonConfiguration对象
         * @param url 请求的URL地址
         * @param headers 请求头参数，可以为null
         * @return JsonConfiguration对象，如果请求失败则抛出IOException异常
         * @throws IOException 如果发生I/O错误则抛出异常
         */
        public static JsonConfiguration getJson(@NotNull String url, @Nullable Map<String, String> headers) throws IOException {
            String jsonString = get(url, headers);
            return new JsonConfiguration(jsonString);
        }


        /**
         * 下载文件并保存到指定路径
         * @param path 文件保存的路径
         * @param url 文件下载的URL地址
         * @param headers 请求头参数，可以为null
         * @return 下载成功则返回true，否则返回false
         * @throws IOException 如果发生I/O错误则抛出异常
         */
        public static boolean getFile(@NotNull String path, @NotNull String url, @Nullable Map<String, String> headers) throws IOException {
            URL u = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setRequestMethod("GET");

            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream in = conn.getInputStream();
                OutputStream out = new FileOutputStream(new File(path));

                byte[] buffer = new byte[1024];
                int len;

                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }

                in.close();
                out.close();
                return true;
            }

            return false;
        }


        /**
         * 异步下载文件并保存到指定路径，不阻塞主线程
         * @param path 文件保存的路径
         * @param url 文件下载的URL地址
         * @param headers 请求头参数，可以为null
         */
        public static void asyncGetFile(@NotNull String path,@NotNull String url,@Nullable Map<String, String> headers) {
            new Thread(() -> {
                try {
                    boolean success = getFile(path, url, headers);
                } catch (IOException e) {
                    // 文件下载失败的逻辑处理
                    System.out.println("文件下载失败:"+e.getMessage());
                }
            }).start();
        }
    }

    public static class HttpGetObject extends HttpBase.HttpBaseObject {
        public HttpGetObject(@NotNull String url, @Nullable Map<String, String> headers) {
            super(url, headers);
        }
        public HttpGetObject(@NotNull String url) {
            super(url);
        }

        /**
         * 设置http请求方法
         */
        @Override
        public HttpBase.HttpMethod getMethod(){
            return HttpBase.HttpMethod.GET;
        }

        /**
         * 发送GET请求并获取响应结果
         *
         * @throws IOException 如果发生I/O错误则抛出异常
         */
        public void get() throws IOException {
            this.connect();
        }





    }
}