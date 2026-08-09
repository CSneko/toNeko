package org.cneko.toneko.common.mod.client.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * 客户端文本工具。
 * <p>
 * vanilla 的 {@link Component#literal} 不解析 § legacy 格式码（显示为字面符号），
 * 服务端格式化文本（如 {@code Messaging.format} 的输出）需要这里手动解析成 Style。
 */
public final class ClientTextUtil {
    private ClientTextUtil() {}

    /**
     * 把带 § legacy 格式码的字符串解析为带样式的 Component。
     * 支持颜色码（§0-9a-f）、格式码（§k-o）、重置（§r）、RGB 码（§x§R§R§G§G§B§B）。
     * 颜色码按 legacy 语义重置当前格式；尾部孤立的 § 按字面追加。
     */
    public static MutableComponent parseLegacyFormatting(String text) {
        MutableComponent root = Component.empty();
        StringBuilder seg = new StringBuilder();
        Style style = Style.EMPTY;
        int i = 0;
        int len = text.length();
        while (i < len) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < len) {
                char code = text.charAt(i + 1);
                ChatFormatting fmt = ChatFormatting.getByCode(code);
                if (fmt != null) {
                    flush(root, seg, style);
                    if (fmt == ChatFormatting.RESET) {
                        style = Style.EMPTY;
                    } else if (fmt.isFormat()) {
                        style = switch (fmt) {
                            case BOLD -> style.withBold(true);
                            case ITALIC -> style.withItalic(true);
                            case UNDERLINE -> style.withUnderlined(true);
                            case STRIKETHROUGH -> style.withStrikethrough(true);
                            case OBFUSCATED -> style.withObfuscated(true);
                            default -> style;
                        };
                    } else {
                        // legacy 语义：颜色码同时重置此前所有格式
                        style = Style.EMPTY.withColor(fmt);
                    }
                    i += 2;
                    continue;
                }
                // §x§R§R§G§G§B§B RGB 码
                if (code == 'x' && i + 13 < len) {
                    String rgb = text.substring(i + 2, i + 14).replaceAll("\\u00A7", "");
                    if (rgb.matches("[0-9a-fA-F]{6}")) {
                        flush(root, seg, style);
                        int color = Integer.parseInt(rgb, 16);
                        style = Style.EMPTY.withColor(color);
                        i += 14;
                        continue;
                    }
                }
            }
            seg.append(c);
            i++;
        }
        flush(root, seg, style);
        return root;
    }

    private static void flush(MutableComponent root, StringBuilder seg, Style style) {
        if (seg.length() > 0) {
            root.append(Component.literal(seg.toString()).withStyle(style));
            seg.setLength(0);
        }
    }
}
