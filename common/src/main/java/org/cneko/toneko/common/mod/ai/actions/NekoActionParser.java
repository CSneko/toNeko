package org.cneko.toneko.common.mod.ai.actions;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.cneko.toneko.common.Bootstrap.LOGGER;

/**
 * 解析 AI 回复中的 JSON 代码块动作。
 * AI 在回复末尾（或任意位置）输出 ```json ... ``` 代码块，
 * 内容为单个动作对象或动作数组：
 *   {"action": "move_to_player"}
 *   {"action": "give_item", "item": "minecraft:apple", "count": 1}
 * 解析失败时静默忽略动作，只返回清理后的文本 —— 永不阻塞聊天。
 */
public class NekoActionParser {
    private static final Gson GSON = new Gson();
    /** 匹配 ```json ... ``` 代码块（非贪婪，支持跨行） */
    private static final Pattern JSON_BLOCK = Pattern.compile("(?s)```json\\s*(.*?)```");

    private NekoActionParser() {}

    public static ParseResult parse(String responseText) {
        if (responseText == null) return new ParseResult(List.of(), "");
        Matcher matcher = JSON_BLOCK.matcher(responseText);
        List<NekoAction> actions = new ArrayList<>();
        String cleaned = matcher.replaceAll("").trim();

        Matcher finder = JSON_BLOCK.matcher(responseText);
        while (finder.find()) {
            String json = finder.group(1).trim();
            List<NekoAction> parsed = parseJson(json);
            if (!parsed.isEmpty()) {
                actions.addAll(parsed);
            }
        }
        return new ParseResult(actions, cleaned);
    }

    /** 解析单个 JSON 内容（对象或数组）为动作列表 */
    private static List<NekoAction> parseJson(String json) {
        List<NekoAction> result = new ArrayList<>();
        try {
            JsonElement element = GSON.fromJson(json, JsonElement.class);
            if (element == null || element.isJsonNull()) return result;
            if (element.isJsonArray()) {
                for (JsonElement e : element.getAsJsonArray()) {
                    NekoAction action = toAction(e);
                    if (action != null) result.add(action);
                }
            } else {
                NekoAction action = toAction(element);
                if (action != null) result.add(action);
            }
        } catch (JsonSyntaxException e) {
            LOGGER.warn("[AI-ACTION] invalid action JSON: {}", e.getMessage());
        }
        return result;
    }

    private static NekoAction toAction(JsonElement element) {
        if (!element.isJsonObject()) return null;
        JsonObject obj = element.getAsJsonObject();
        JsonElement typeEl = obj.get("action");
        if (typeEl == null || !typeEl.isJsonPrimitive() || typeEl.getAsString().isEmpty()) return null;
        String type = typeEl.getAsString().trim();
        String item = obj.has("item") && obj.get("item").isJsonPrimitive()
                ? obj.get("item").getAsString().trim() : "";
        int count = obj.has("count") && obj.get("count").isJsonPrimitive()
                ? Math.max(1, obj.get("count").getAsInt()) : 1;
        String target = obj.has("target") && obj.get("target").isJsonPrimitive()
                ? obj.get("target").getAsString().trim() : "";
        return new NekoAction(type, item, count, target);
    }

    /** 解析结果：动作列表 + 去掉代码块后的显示文本 */
    public record ParseResult(List<NekoAction> actions, String cleanedText) {}
}
