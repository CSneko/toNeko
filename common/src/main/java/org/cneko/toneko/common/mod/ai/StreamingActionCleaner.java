package org.cneko.toneko.common.mod.ai;

/**
 * 流式动作 JSON 剥离器：逐 chunk 吞掉 ```json ... ``` 代码块与行内 {"action": ...}，
 * 只向打字机输出干净文本增量（与 {@link org.cneko.toneko.common.mod.ai.actions.NekoActionParser}
 * 的通用化语义一致：代码块变体（```json/```jsonl/无标签/大小写）、未闭合块丢弃、行内裸 JSON）。
 * <p>
 * 输出保证单调不减：每段增量直接 append，不回缩、不重发。
 * 原始完整文本（含 JSON 块）仍由调用方保存历史并交给 NekoActionExecutor 解析动作。
 */
public class StreamingActionCleaner {
    private static final String BLOCK_CLOSER = "```";
    /** 动作对象起点（行内裸 JSON） */
    private static final String INLINE_ACTION = "{\"action\"";

    /** 未决定归属的缓冲（净文本尾部可能是不完整的 opener 前缀，如 "```js"） */
    private final StringBuilder pending = new StringBuilder();
    /** 当前是否位于 ```json 块内（块内容只被丢弃） */
    private boolean inBlock = false;
    /** 当前是否位于行内 {"action": 之后（丢弃到行尾） */
    private boolean inInline = false;

    /**
     * 喂入一段增量文本，返回可安全显示的净文本增量（可为空串）。
     * 输出单调不减：调用方直接 append 即可，无需重绘/回退。
     */
    public String feed(String delta) {
        if (delta == null || delta.isEmpty()) return "";
        pending.append(delta);
        return processPending();
    }

    /**
     * 流结束收尾：丢弃未闭合的 ```json 块与行内 JSON（同 NekoActionParser 的容错语义），
     * 返回残余净文本。
     */
    public String finish() {
        if (inBlock) {
            pending.setLength(0);
            inBlock = false;
        } else if (inInline) {
            pending.setLength(0);
            inInline = false;
        }
        String rest = pending.toString();
        pending.setLength(0);
        return rest;
    }

    private String processPending() {
        StringBuilder out = new StringBuilder();
        while (true) {
            if (inBlock) {
                // 块内：丢弃内容直到遇到 ```（第一个出现即闭块）
                int closeIdx = pending.indexOf(BLOCK_CLOSER);
                if (closeIdx < 0) {
                    int keep = incompleteSuffixLen(pending, BLOCK_CLOSER);
                    pending.setLength(keep);
                    break;
                }
                pending.delete(0, closeIdx + BLOCK_CLOSER.length());
                inBlock = false;
            } else if (inInline) {
                // 行内 JSON：丢弃到行尾（AI 输出的裸 JSON 一般独占一行；换行保留，保持对话分行）
                int nlIdx = pending.indexOf("\n");
                if (nlIdx < 0) {
                    pending.setLength(0);
                    break;
                }
                pending.delete(0, nlIdx);
                inInline = false;
            } else {
                // 净文本状态：找代码块 opener；没有则找行内 {"action"
                int openerIdx = findBlockOpener(pending);
                if (openerIdx >= 0) {
                    // opener 之前的文本为净文本，opener 吞掉，进入块内
                    out.append(pending, 0, openerIdx);
                    pending.delete(0, openerIdx + 3 + tagLength(pending, openerIdx));
                    inBlock = true;
                    continue;
                }
                int inlineIdx = pending.indexOf(INLINE_ACTION);
                if (inlineIdx >= 0) {
                    out.append(pending, 0, inlineIdx);
                    pending.delete(0, inlineIdx + INLINE_ACTION.length());
                    inInline = true;
                    continue;
                }
                // 两者都没有：整段净文本，但保留尾部可能是 opener/行内 JSON 前缀的字符
                int keep = Math.max(incompleteSuffixLen(pending, INLINE_ACTION),
                        Math.max(incompleteSuffixLen(pending, "```jsonl"),
                                Math.max(incompleteSuffixLen(pending, "```json"),
                                        openerReserveLen(pending))));
                if (pending.length() > keep) {
                    out.append(pending, 0, pending.length() - keep);
                    pending.delete(0, pending.length() - keep);
                }
                break;
            }
        }
        return out.toString();
    }

    /**
     * 找代码块 opener：``` 后跟 json/jsonl 标签（忽略大小写），或 ``` 后直接跟 {/[，
     * 或 ``` 后跟空白且空白后首个非空白字符是 {/[（无标签动作块；``` 后跟普通文本如 ```java 不是动作块），
     * 或 ``` 在缓冲末尾（可能是跨 chunk 的不完整标签，返回 -1 交由保留逻辑等待）。
     */
    private static int findBlockOpener(StringBuilder sb) {
        String s = sb.toString();
        int idx = s.indexOf("```");
        while (idx >= 0) {
            int after = idx + 3;
            if (after >= s.length()) return -1; // ``` 在末尾：可能是跨 chunk 前缀，等待
            String rest = s.substring(after).toLowerCase();
            if (rest.startsWith("jsonl") || rest.startsWith("json")) return idx;
            char c = rest.charAt(0);
            if (c == '{' || c == '[') return idx;
            if (Character.isWhitespace(c)) {
                // ``` 后空白：lookahead 到首个非空白，必须是 { 或 [ 才算无标签动作块
                int nb = 1;
                while (nb < rest.length() && Character.isWhitespace(rest.charAt(nb))) nb++;
                if (nb < rest.length() && (rest.charAt(nb) == '{' || rest.charAt(nb) == '[')) return idx;
            }
            idx = s.indexOf("```", idx + 3);
        }
        return -1;
    }

    /** opener 的语言标签长度（json=4 / jsonl=5 / 无标签=0），从 opener 位置 idx 起判断 */
    private static int tagLength(StringBuilder sb, int idx) {
        if (idx + 3 >= sb.length()) return 0;
        String rest = sb.substring(idx + 3).toLowerCase();
        if (rest.startsWith("jsonl")) return 5;
        if (rest.startsWith("json")) return 4;
        return 0;
    }

    /** 缓冲尾部是否为 ```json 类 opener 的不完整前缀（如 "```j"），是则返回需保留的长度 */
    private static int openerReserveLen(StringBuilder sb) {
        String s = sb.toString();
        int n = s.length();
        int b = s.lastIndexOf("```");
        if (b < 0 || n - b > 8) return 0;
        String tail = s.substring(b).toLowerCase();
        if (tail.equals("```") || tail.equals("```j") || tail.equals("```js")
                || tail.equals("```json") || tail.equals("```jsonl")) {
            return n - b;
        }
        String[] full = {"```jsonl", "```json"};
        for (String f : full) {
            if (f.startsWith(tail) && tail.length() < f.length()) return n - b;
        }
        return 0;
    }

    /** 缓冲尾部与目标字符串前缀的最长匹配长度（0~len-1，用于跨 chunk 的不完整标记） */
    private static int incompleteSuffixLen(StringBuilder sb, String target) {
        int max = Math.min(sb.length(), target.length() - 1);
        for (int len = max; len > 0; len--) {
            if (sb.substring(sb.length() - len).equals(target.substring(0, len))) {
                return len;
            }
        }
        return 0;
    }
}
