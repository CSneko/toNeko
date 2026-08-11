package org.cneko.toneko.common.mod.ai;

import org.cneko.toneko.common.mod.entities.GhostNekoEntity;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.util.ConfigUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
    /** 环境感知占位符允许更长（含 intro/事实/instruction 三段，事实部分自身已裁剪到 ~200） */
    private static final int MAX_SURROUNDINGS_LENGTH = 256;
    /** 日记上下文占位符允许更长（最近 2 篇 + 引导语，buildContext 已按篇截断） */
    private static final int MAX_DIARY_LENGTH = 300;
    /** 幽灵生前记忆描述允许更长（名字/萌属性/主人等玩家可控文本需净化） */
    private static final int MAX_GHOST_PAST_LENGTH = 200;


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
            int maxLen = switch (key) {
                case "neko_inventory" -> MAX_INVENTORY_LENGTH;
                case "neko_surroundings" -> MAX_SURROUNDINGS_LENGTH;
                case "neko_diary" -> MAX_DIARY_LENGTH;
                default -> MAX_INSERT_LENGTH;
            };
            String replacement = sanitize(factory.getPrompt(neko, other), maxLen);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        String result = sb.toString();
        // 环境感知：prompt 显式含 %neko_surroundings% 时已在占位符处替换（用户掌控位置）；
        // 否则配置开启时自动追加到末尾（覆盖默认 prompt 与 CrystalNeko 硬编码 prompt——
        // 后者不含任何占位符，注册层追加是唯一注入途径）。追加发生在占位符扫描之后，
        // 无二次替换/嵌套注入风险。追加文本必须显式净化（昵称等是玩家可控文本）。
        if (ConfigUtil.isAISurroundingsEnabled() && !prompt.contains("%neko_surroundings%")) {
            String surroundings = SurroundingsScanner.describe(neko, other);
            if (!surroundings.isEmpty()) {
                result += "\n" + sanitize(surroundings, MAX_SURROUNDINGS_LENGTH);
            }
        }
        // 日记上下文：启用 AI 动作时自动注入最近几篇日记（供 write_diary 参考保持风格）；
        // prompt 显式含 %neko_diary% 时走占位符路径。追加发生在占位符扫描之后，无二次替换风险。
        if (ConfigUtil.isAIActionsEnabled() && !prompt.contains("%neko_diary%")) {
            String diary = NekoDiary.buildContext(neko.getDiaryEntries(), 2, 100);
            if (!diary.isEmpty()) {
                result += "\n" + sanitize(diary, MAX_DIARY_LENGTH);
            }
        }
        // 附加人设（萝莉/萌属性性格强化）：追加在环境感知之前，保证模型优先注意到
        String persona = buildPersonaExtra(neko);
        if (!persona.isEmpty()) {
            result += "\n" + persona;
        }
        return result;
    }

    /**
     * 按实体状态/萌属性附加人设描述（本地化文案，不烧 token）：
     * - 幽灵猫娘：生前类型 + 保留的性格（名字/主人等仍保留着的信息不写"生前"）在前 + 幽灵身份在后
     * - 萝莉（isNekoBaby）：娇小可爱的萝莉猫娘形象
     * - 雌小鬼（mesugaki，含组合）：喜欢叫别人"杂鱼"
     */
    private static String buildPersonaExtra(NekoEntity neko) {
        List<String> parts = new ArrayList<>();
        if (neko instanceof GhostNekoEntity ghost) {
            // 生前身份在前：幽灵记得自己生前的模样
            buildGhostPastLife(parts, ghost);
            parts.add(Prompts.translateOrReadable("misc.toneko.ai.persona.ghost"));
        }
        if (neko.isNekoBaby()) {
            parts.add(Prompts.translateOrReadable("misc.toneko.ai.persona.loli"));
        }
        if (neko.getMoeTags().contains("mesugaki")) {
            parts.add(Prompts.translateOrReadable("misc.toneko.ai.persona.mesugaki"));
        }
        return String.join("\n", parts);
    }

    /**
     * 幽灵的生前后记忆：生前类型（转化时记录在 NBT，幽灵 type 只会读出"幽灵猫娘"）
     * + 保留的性格（萌属性）。名字/主人/好感度等数据幽灵仍然保留着，
     * 无需"生前"限定，且名字已由 %neko_name% 占位符注入。全部过净化。
     */
    private static void buildGhostPastLife(List<String> parts, GhostNekoEntity ghost) {
        String pastTypeKey = ghost.getPastTypeName();
        if (pastTypeKey != null && !pastTypeKey.isEmpty()) {
            String pastType = Prompts.translateOrReadable(pastTypeKey);
            parts.add(sanitize(Prompts.translateOrReadable("misc.toneko.ai.persona.ghost_past_type", pastType),
                    MAX_GHOST_PAST_LENGTH));
        }
        String moe = Prompts.NEKO_MOE_TAGS.getPrompt(ghost, null);
        if (!moe.isEmpty()) {
            parts.add(sanitize(Prompts.translateOrReadable("misc.toneko.ai.persona.ghost_moe", moe),
                    MAX_GHOST_PAST_LENGTH));
        }
    }

    /**
     * 净化插入到 prompt 中的文本（昵称/名字/描述等可能是玩家可控的）：
     * 去除 Minecraft 格式化代码、压缩换行与连续空白、截断超长文本，
     * 避免污染 system prompt 的结构与语义。
     */
    static String sanitize(String text, int maxLen) {
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
