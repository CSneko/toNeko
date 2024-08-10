package org.cneko.toneko.bukkit.util;

import org.cneko.toneko.common.util.LanguageUtil;

public class Language {
    public static String get(String key) {
        return LanguageUtil.translatable(key);
    }
    public static String get(String key, Object... args) {
        return LanguageUtil.translatable(key, args);
    }
}
