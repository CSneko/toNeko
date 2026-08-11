package org.cneko.toneko.common.mod.ai.actions;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.cneko.toneko.common.Bootstrap.LOGGER;

/**
 * 解析 AI 回复中的动作 JSON（通用化：不限定位置与格式）。
 * <p>
 * 支持形式：
 * - 闭合代码块：```json ... ``` / ```jsonl / 无语言标签 ``` / 大小写变体，块内可夹带说明文字（提取其中 JSON）
 * - 未闭合代码块：```json 开头无闭合，容错删到文本末尾
 * - 行内裸 JSON：{"action": ...} / [{"action": ...}] 出现在任意位置（括号配对提取，正文中其他 JSON 不受影响）
 * 内容为单个动作对象或动作数组。解析失败时静默忽略动作，只返回清理后的文本 —— 永不阻塞聊天。
 */
public class NekoActionParser {
    private static final Gson GSON = new Gson();
    /** 闭合代码块：``` 后可选 json/jsonl 语言标签（忽略大小写），块内容非贪婪到第一个 ``` */
    private static final Pattern CLOSED_BLOCK = Pattern.compile("(?s)```(?:[jJ][sS][oO][nN][lL]?)?\\s*(.*?)```");
    /** 未闭合代码块：``` 后可选 json/jsonl 标签，无闭合则匹配到文本末尾（容错删除） */
    private static final Pattern UNCLOSED_BLOCK = Pattern.compile("(?s)```(?:[jJ][sS][oO][nN][lL]?)?[\\s\\S]*$");
    /** 动作对象起点：{"action": */
    private static final Pattern ACTION_OBJ = Pattern.compile("\\{\\s*\"action\"\\s*:");
    /** 动作数组起点：[{"action": */
    private static final Pattern ACTION_ARR = Pattern.compile("\\[\\s*\\{");

    private NekoActionParser() {}

    public static ParseResult parse(String responseText) {
        if (responseText == null) return new ParseResult(List.of(), "");
        List<NekoAction> actions = new ArrayList<>();
        List<int[]> removals = new ArrayList<>();

        // 1. 闭合代码块：整个块（含围栏）移除；块内可能夹带说明文字（AI 偶尔不听话），提取其中的 JSON
        Matcher blockMatcher = CLOSED_BLOCK.matcher(responseText);
        while (blockMatcher.find()) {
            removals.add(new int[]{blockMatcher.start(), blockMatcher.end()});
            for (Hit hit : findHits(blockMatcher.group(1))) {
                actions.addAll(hit.actions());
            }
        }

        // 2. 未闭合代码块：容错删到文本末尾（避免 ```json 残骸显示/回喂模型）；
        //    块内容中合法的动作 JSON 仍提取执行
        Matcher unclosed = UNCLOSED_BLOCK.matcher(responseText);
        if (unclosed.find()) {
            int start = unclosed.start();
            boolean covered = removals.stream().anyMatch(r -> start >= r[0] && start < r[1]);
            if (!covered) {
                for (Hit hit : findHits(responseText.substring(Math.min(start + 3, responseText.length())))) {
                    actions.addAll(hit.actions());
                }
                removals.add(new int[]{start, responseText.length()});
            }
        }

        // 3. 行内裸 JSON（任意位置）：跳过已被代码块覆盖的区间（避免块内动作二次解析）
        for (Hit hit : findHits(responseText)) {
            boolean covered = removals.stream().anyMatch(c -> hit.start() >= c[0] && hit.end() <= c[1]);
            if (!covered) {
                actions.addAll(hit.actions());
                removals.add(new int[]{hit.start(), hit.end()});
            }
        }

        // 4. 合并重叠区间后删除
        return new ParseResult(actions, removeRanges(responseText, removals));
    }

    /** 一次成功解析的命中：区间 + 解析出的动作 */
    private record Hit(int start, int end, List<NekoAction> actions) {}

    /**
     * 在文本中扫描所有动作 JSON（对象或数组起点）并解析。
     * 非动作对象（正文中偶发的普通 JSON）解析结果为空，不产生命中、不影响正文。
     */
    private static List<Hit> findHits(String text) {
        List<Hit> hits = new ArrayList<>();
        int pos = 0;
        while (pos < text.length()) {
            Matcher objM = ACTION_OBJ.matcher(text);
            Matcher arrM = ACTION_ARR.matcher(text);
            int objPos = objM.find(pos) ? objM.start() : -1;
            int arrPos = arrM.find(pos) ? arrM.start() : -1;
            int start = objPos < 0 ? arrPos : (arrPos < 0 ? objPos : Math.min(objPos, arrPos));
            if (start < 0) break;
            int end = matchBrace(text, start);
            if (end > 0) {
                List<NekoAction> parsed = parseJson(text.substring(start, end));
                if (!parsed.isEmpty()) {
                    hits.add(new Hit(start, end, parsed));
                }
                pos = end;
            } else {
                pos = start + 1; // 括号未配对（残破 JSON）：跳过该起点继续
            }
        }
        return hits;
    }

    /** 从 start 处（必须是 { 或 [）括号配对（支持嵌套与字符串转义），返回闭括号后的位置；失败返回 -1 */
    private static int matchBrace(String text, int start) {
        char open = text.charAt(start);
        char close = open == '{' ? '}' : ']';
        int depth = 0;
        boolean inString = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (c == '\\') { i++; continue; }
                if (c == '"') inString = false;
                continue;
            }
            if (c == '"') { inString = true; continue; }
            if (c == open) { depth++; continue; }
            if (c == close) {
                depth--;
                if (depth == 0) return i + 1;
            }
        }
        return -1;
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
        // change_affection 的 count 可正可负（正=增好感，负=减好感），取原始值；
        // 其余动作保持 ≥1 钳制，防止 count=0/负数破坏 give_item 等逻辑
        int rawCount = obj.has("count") && obj.get("count").isJsonPrimitive()
                ? obj.get("count").getAsInt() : 1;
        int count = "change_affection".equals(type) ? rawCount : Math.max(1, rawCount);
        String target = obj.has("target") && obj.get("target").isJsonPrimitive()
                ? obj.get("target").getAsString().trim() : "";
        String text = obj.has("text") && obj.get("text").isJsonPrimitive()
                ? obj.get("text").getAsString().trim() : "";
        return new NekoAction(type, item, count, target, text);
    }

    /** 合并重叠区间后按序删除，返回清理后的文本 */
    private static String removeRanges(String text, List<int[]> ranges) {
        if (ranges.isEmpty()) return text.trim();
        ranges.sort(Comparator.comparingInt(r -> r[0]));
        List<int[]> merged = new ArrayList<>();
        for (int[] r : ranges) {
            if (r[1] <= r[0]) continue;
            if (!merged.isEmpty() && r[0] <= merged.get(merged.size() - 1)[1]) {
                merged.get(merged.size() - 1)[1] = Math.max(merged.get(merged.size() - 1)[1], r[1]);
            } else {
                merged.add(new int[]{r[0], r[1]});
            }
        }
        StringBuilder sb = new StringBuilder();
        int pos = 0;
        for (int[] r : merged) {
            sb.append(text, pos, r[0]);
            pos = r[1];
        }
        sb.append(text, pos, text.length());
        return sb.toString().trim();
    }

    /** 解析结果：动作列表 + 去掉 JSON 后的显示文本 */
    public record ParseResult(List<NekoAction> actions, String cleanedText) {}
}
