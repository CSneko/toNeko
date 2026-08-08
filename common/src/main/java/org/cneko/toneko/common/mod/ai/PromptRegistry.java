package org.cneko.toneko.common.mod.ai;

import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.entities.NekoEntity;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PromptRegistry {
    private static final Map<String, PromptFactory> PROMPT_REGISTRY = new HashMap<>();

    /** 占位符模式：%key% */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([a-zA-Z_][a-zA-Z0-9_]*)%");
    /** Minecraft 格式化代码（§a / §r 等） */
    private static final Pattern FORMAT_CODE_PATTERN = Pattern.compile("§[0-9a-fk-orK-OR]");
    /** 插入到 prompt 中的玩家可控文本的最大长度，防止昵称等超长内容膨胀 token */
    private static final int MAX_INSERT_LENGTH = 64;
    /** 背包内容占位符允许更长（物品列表通常超过 64 字符） */
    private static final int MAX_INVENTORY_LENGTH = 600;


    public static PromptFactory register(String key, PromptFactory promptFactory) {
        PROMPT_REGISTRY.put(key, promptFactory);
        return promptFactory;
    }

    public static Collection<PromptFactory> getAll() {
        return PROMPT_REGISTRY.values();
    }

    public static String generatePrompt(NekoEntity neko, INeko other, String prompt) {
        if (prompt == null) {
            return "";
        }
        // 单次扫描替换所有占位符：替换结果不会被再次扫描，
        // 杜绝"玩家名恰好是 %xxx% 被二次替换"的嵌套注入问题
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(prompt);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            PromptFactory factory = PROMPT_REGISTRY.get(key);
            if (factory == null) {
                // 未注册的占位符保持原样
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
                continue;
            }
            int maxLen = "neko_inventory".equals(key) ? MAX_INVENTORY_LENGTH : MAX_INSERT_LENGTH;
            String replacement = sanitize(factory.getPrompt(neko, other), maxLen);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 净化插入到 prompt 中的文本（昵称/名字/描述等可能是玩家可控的）：
     * 去除 Minecraft 格式化代码、压缩换行与连续空白、截断超长文本，
     * 避免污染 system prompt 的结构与语义。
     */
    private static String sanitize(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        String cleaned = FORMAT_CODE_PATTERN.matcher(text).replaceAll("");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned.length() > maxLen
                ? cleaned.substring(0, maxLen)
                : cleaned;
    }


    public interface PromptFactory {
        String getPrompt(NekoEntity neko, INeko other);
    }
}
