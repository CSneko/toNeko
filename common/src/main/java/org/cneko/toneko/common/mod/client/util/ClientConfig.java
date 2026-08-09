package org.cneko.toneko.common.mod.client.util;

import org.cneko.toneko.common.util.ConfigBuilder;
import org.cneko.toneko.common.util.JsonConfiguration;

import java.nio.file.Path;

/**
 * 客户端本地配置（config/toneko_client.json），与服务端 toneko.json 分离。
 * <p>
 * 只在客户端代码路径被引用（接收器/渲染器均包在 {@code context.client().execute} 或主线程内），
 * 专用服务器不会加载本类。JsonConfiguration 内部 synchronized + volatile 单例双保险。
 */
public final class ClientConfig {
    private static final Path FILE = Path.of("config/toneko_client.json");
    private static final ConfigBuilder BUILDER = ConfigBuilder.create(FILE)
            .addString("ai.chatDisplay", "chat", "",
                    "AI回复显示方式：chat=聊天栏，bubble=猫娘头顶气泡",
                    "AI reply display mode: chat (chat bar) or bubble (above neko's head)")
            .addString("ai.bubble.duration", "6000", "",
                    "头顶气泡显示时长（毫秒，最小1000）",
                    "Head bubble display duration (ms, min 1000)")
            .addString("ai.bubble.color", "FF69B4", "",
                    "头顶气泡颜色（RGB十六进制，如 FF69B4 粉色）",
                    "Head bubble color (RGB hex, e.g. FF69B4 pink)")
            .build();
    private static volatile JsonConfiguration config;

    private ClientConfig() {}

    private static JsonConfiguration get() {
        JsonConfiguration c = config;
        if (c == null) {
            synchronized (ClientConfig.class) {
                c = config;
                if (c == null) {
                    c = BUILDER.createConfig();
                    config = c;
                }
            }
        }
        return c;
    }

    /** 是否使用头顶气泡显示 AI 回复（false = 聊天栏） */
    public static boolean isBubbleMode() {
        return "bubble".equals(getChatDisplay());
    }

    /** 当前 AI 回复显示方式（"chat" 或 "bubble"） */
    public static String getChatDisplay() {
        return get().getString("ai.chatDisplay");
    }

    /** 设置 AI 回复显示方式（"chat" 或 "bubble"）并保存 */
    public static void setChatDisplay(String mode) {
        if (!"chat".equals(mode) && !"bubble".equals(mode)) return;
        get().set("ai.chatDisplay", mode);
        get().save(FILE);
    }

    /** 头顶气泡显示时长（毫秒），配置非法或过小时回退默认值 */
    public static int getBubbleDuration() {
        try {
            return Math.max(1000, Integer.parseInt(get().getString("ai.bubble.duration")));
        } catch (NumberFormatException e) {
            return 6000;
        }
    }

    /** 设置气泡显示时长（毫秒）并保存，最小 1000 */
    public static void setBubbleDuration(int ms) {
        get().set("ai.bubble.duration", String.valueOf(Math.max(1000, ms)));
        get().save(FILE);
    }

    /** 头顶气泡颜色（RGB int），配置非法时回退粉色 */
    public static int getBubbleColor() {
        String hex = get().getString("ai.bubble.color");
        try {
            if (hex.startsWith("#")) hex = hex.substring(1);
            return Integer.parseInt(hex, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return 0xFF69B4;
        }
    }

    /** 设置气泡颜色（"RRGGBB" 或 "#RRGGBB"），格式非法则忽略 */
    public static void setBubbleColor(String hex) {
        String clean = hex.trim();
        if (clean.startsWith("#")) clean = clean.substring(1);
        if (!clean.matches("[0-9a-fA-F]{6}")) return;
        get().set("ai.bubble.color", clean.toLowerCase());
        get().save(FILE);
    }

    /** 保存当前配置到磁盘 */
    public static void save() {
        get().save(FILE);
    }
}
