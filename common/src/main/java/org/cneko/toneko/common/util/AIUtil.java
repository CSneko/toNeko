package org.cneko.toneko.common.util;

import org.cneko.ctlib.common.file.JsonConfiguration;
import org.cneko.ctlib.common.network.HttpPost;
import org.cneko.toneko.common.util.scheduled.SchedulerPoolProvider;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.cneko.toneko.common.Bootstrap.LOGGER;

public class AIUtil {
    public static final String API_URL = "https://chat.ai.cneko.org";
    public static final String PAST_MESSAGE_PATH = "ctlib/toneko/AI/";
    private static final ExecutorService executor = Executors.newFixedThreadPool(100);

    public static void init(){
        FileUtil.CreatePath(PAST_MESSAGE_PATH);
    }
    public static void sendMessage(UUID uuid, String prompt, String message, MessageCallback callback){
        executor.submit(()->{
            try{
                FileUtil.CreatePath(PAST_MESSAGE_PATH + uuid + "/");
                String pastMessagePath = PAST_MESSAGE_PATH + uuid + "/" +"messages.json";
                FileUtil.CreateFile(pastMessagePath);
                // 读取json
                String json = FileUtil.readStringFromFile(pastMessagePath);
                if (json.equalsIgnoreCase("")){
                    FileUtil.WriteFile(pastMessagePath, "{\"contents\":[]}");
                }
                JsonConfiguration j = JsonConfiguration.fromFile(Path.of(pastMessagePath));
                List<JsonConfiguration> contents = j.getJsonList("contents");
                // 检查是否超过了20
                if (contents.size() >= 20){
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
                    callback.execute(resMsg);
                }
            }catch (Exception e){
                LOGGER.error("Failed to send message", e);
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


    @FunctionalInterface
    public interface MessageCallback {
        void execute(String message);
    }
}
