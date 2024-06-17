package org.cneko.toneko.common.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.core.layout.JsonLayout;
import org.cneko.ctlib.common.file.JsonConfiguration;

import java.util.ArrayList;
import java.util.List;

public class JsonUtil {
    public static List<JsonObject> jsonListToJsonList(List<JsonConfiguration> list){
        return toJsonList(toStringList(list));
    }
    public static List<JsonObject> toJsonList(List<String> list){
        List<JsonObject> json = new ArrayList<>();
        for (String s : list){
            json.add(JsonParser.parseString(s).getAsJsonObject());
        }
        return json;
    }
    public static List<String> toStringList(List<JsonConfiguration> json) {
        List<String> list = new ArrayList<>();
        for (JsonConfiguration j : json){
            list.add(j.toString());
        }
        return list;
    }
    public static String listToJson(List<String> list){
        // 创建Gson对象
        Gson gson = new Gson();

        // 将String列表转换为JSON字符串
        return gson.toJson(list);
    }
    public static JsonArray jsonListToGsonArray(List<JsonConfiguration> list){
        return listToGson(toStringList(list));
    }
    public static JsonArray listToGson(List<String> list){
        // 创建Gson对象
        Gson gson = new Gson();

        // 创建JsonArray来存储解析后的JsonObject
        JsonArray jsonArray = new JsonArray();

        // 使用JsonParser来直接解析每个字符串为JsonObject，处理转义问题
        for (String str : list) {
            JsonObject jsonObject = JsonParser.parseString(str).getAsJsonObject();
            jsonArray.add(jsonObject);
        }

        return jsonArray;
    }
    public static String jsonListToJson(List<JsonConfiguration> list){
        return listToJson(toStringList(list));
    }
}
