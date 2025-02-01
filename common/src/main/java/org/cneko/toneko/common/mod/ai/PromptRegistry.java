package org.cneko.toneko.common.mod.ai;

import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.entities.NekoEntity;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PromptRegistry {
    private static final Map<String, PromptFactory> PROMPT_REGISTRY = new HashMap<>();


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
        // 遍历注册的所有PromptFactory
        for (Map.Entry<String, PromptFactory> entry : PROMPT_REGISTRY.entrySet()) {
            String key = entry.getKey();
            // 构造占位符字符串，例如 key 为 "var"，占位符为 "%var%"
            String placeholder = "%" + key + "%";
            // 如果提示词中包含该占位符
            if (prompt.contains(placeholder)) {
                // 调用对应的工厂生成替换文本
                String replacement = entry.getValue().getPrompt(neko, other);
                // 替换所有出现的占位符
                prompt = prompt.replace(placeholder, replacement);
            }
        }
        return prompt;
    }


    public interface PromptFactory {
        String getPrompt(NekoEntity neko,INeko other);
    }
}
