package org.cneko.toneko.common.mod.ai;

/**
 * 流式动作 JSON 剥离器：逐 chunk 吞掉 ```json ... ``` 代码块，
 * 只向打字机输出干净文本增量（与 {@link org.cneko.toneko.common.mod.ai.actions.NekoActionParser}
 * 的正则语义一致：```json 开块、第一个 ``` 闭块、未闭合的块整体丢弃）。
 * <p>
 * 输出保证单调不减：每段增量直接 append，不回缩、不重发。
 * 原始完整文本（含 JSON 块）仍由调用方保存历史并交给 NekoActionExecutor 解析动作。
 */
public class StreamingActionCleaner {
    private static final String BLOCK_OPENER = "```json";
    private static final String BLOCK_CLOSER = "```";

    /** 未决定归属的缓冲（净文本尾部可能是不完整的 opener 前缀，如 "```js"） */
    private final StringBuilder pending = new StringBuilder();
    /** 当前是否位于 ```json 块内（块内容只被丢弃） */
    private boolean inBlock = false;

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
     * 流结束收尾：丢弃未闭合的 ```json 块（与 NekoActionParser 的"无闭合不匹配"一致），
     * 返回残余净文本。
     */
    public String finish() {
        if (inBlock) {
            // 未闭合块：整块丢弃（进入块时 opener 已被移除，pending 只剩块内容）
            pending.setLength(0);
            inBlock = false;
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
                    // 保留尾部可能是不完整闭块前缀的字符（最多 2 个）
                    int keep = incompleteSuffixLen(pending, BLOCK_CLOSER);
                    pending.setLength(keep);
                    break;
                }
                pending.delete(0, closeIdx + BLOCK_CLOSER.length());
                inBlock = false;
            } else {
                int openerIdx = pending.indexOf(BLOCK_OPENER);
                if (openerIdx < 0) {
                    // 无 opener：整段净文本，但保留尾部可能是 opener 前缀的字符
                    int keep = incompleteSuffixLen(pending, BLOCK_OPENER);
                    if (pending.length() > keep) {
                        out.append(pending, 0, pending.length() - keep);
                        pending.delete(0, pending.length() - keep);
                    }
                    break;
                }
                // opener 之前的文本为净文本，opener 本身被吞掉，进入块内
                out.append(pending, 0, openerIdx);
                pending.delete(0, openerIdx + BLOCK_OPENER.length());
                inBlock = true;
            }
        }
        return out.toString();
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
