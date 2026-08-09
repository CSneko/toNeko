package org.cneko.toneko.common.mod.ai;

import net.minecraft.util.RandomSource;
import org.cneko.toneko.common.util.ConfigUtil;

import java.util.ArrayList;
import java.util.List;

import static org.cneko.toneko.common.mod.ai.Prompts.translateOrReadable;

/**
 * AI 猫娘日记的条目组装工具。
 * <p>
 * 条目格式（纯文本，成书页面与 AI 上下文直接消费）：{@code 元数据行 + "\n" + 正文}，
 * 元数据行由 {@code misc.toneko.diary.meta} 模板拼天气/心情/群系/维度，是"写下的那一刻"的语言快照。
 * <p>
 * 注意：模板含字面 % 时不能走 {@code translateOrReadable(key, args)}（内部 String.format 会抛
 * IllegalFormatException），一律用 {@code translateOrReadable(key).replace("%s", ...)} 替换。
 */
public final class NekoDiary {
    /** 日记条目上限默认值（可经配置 ai.actions.diary.max_entries 调整） */
    public static final int MAX_DIARY_ENTRIES = 50;

    /** 当前配置的日记条目上限（至少 1），NekoEntity 追加/读取时使用 */
    public static int maxDiaryEntries() {
        return ConfigUtil.getAIActionsDiaryMaxEntries();
    }
    /** AI 正文长度上限（保留换行） */
    public static final int MAX_BODY_LENGTH = 200;
    /** 懒初始化随机种子条目数 1-2 */
    private static final int MAX_SEED_COUNT = 2;
    /** 随机条目池大小（语言文件 diary.toneko.entry.0-79） */
    private static final int ENTRY_POOL_SIZE = 80;

    private NekoDiary() {}

    /** 组装一篇条目：元数据行 + 换行 + 正文 */
    public static String composeEntry(String weather, String mood, String biome, String dimension, String body) {
        String meta = translateOrReadable("misc.toneko.diary.meta", weather, mood, biome, dimension);
        return meta + "\n" + body;
    }

    /**
     * 随机种子条目（无日记时初始化用）：1-2 条 diary.toneko.entry.* 随机条目，
     * 带同一份当前真实环境元数据，形状与 AI 写的条目一致。
     * 模板替换用 replace 而非带参翻译，防模板含字面 % 崩溃。
     */
    public static List<String> seedEntries(RandomSource random, String nekoName,
                                           String weather, String mood, String biome, String dimension) {
        int count = 1 + random.nextInt(MAX_SEED_COUNT);
        List<String> seeds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String template = translateOrReadable("diary.toneko.entry." + random.nextInt(ENTRY_POOL_SIZE));
            seeds.add(composeEntry(weather, mood, biome, dimension, template.replace("%s", nekoName)));
        }
        return seeds;
    }

    /**
     * AI 上下文：最近 maxEntries 篇，每篇折叠换行并截断到 maxCharsPerEntry 字符，
     * 拼成 "你最近写的日记：\n- ..." 文本段。日记为空时返回空串。
     */
    public static String buildContext(List<String> entries, int maxEntries, int maxCharsPerEntry) {
        if (entries == null || entries.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(translateOrReadable("misc.toneko.ai.diary.context"));
        int from = Math.max(0, entries.size() - maxEntries);
        for (int i = from; i < entries.size(); i++) {
            String e = entries.get(i).replace('\n', ' ');
            if (e.length() > maxCharsPerEntry) e = e.substring(0, maxCharsPerEntry);
            sb.append("\n- ").append(e);
        }
        return sb.toString();
    }
}
