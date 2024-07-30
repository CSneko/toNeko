package org.cneko.toneko.fabric.impl;

import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.cneko.ctlib.common.file.JsonConfiguration;
import org.cneko.toneko.common.util.FileUtil;
import org.cneko.toneko.fabric.ModMeta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.cneko.toneko.common.Bootstrap.LOGGER;
import static org.cneko.toneko.common.util.ConfigUtil.CONFIG;
import static org.cneko.toneko.common.util.LanguageUtil.*;

public class FabricLanguageImpl implements Language{
    @Override
    public void load() {
        try {
            List<String> languages = List.of("en_us","zh_cn","zh_tw","ko_kr");
            language = CONFIG.getString("language");
            // 创建文件夹如果不存在
            FileUtil.CreatePath(LANG_PATH);
            // 删除旧语言文件
            for (String lang : languages){
                FileUtil.DeleteFile(LANG_PATH+lang+".json");
            }

            // 复制语言文件
            for (String lang : languages) {
                String file = LANG_PATH+lang+".json";
                FileUtil.CreateFile(file);
               FileUtil.copyResource("assets/toneko/lang/"+lang+".json", file);
            }
            // 读取语言文件
            LANG = JsonConfiguration.fromFile(Path.of(LANG_PATH+language+".json"));
            EN_US_LANG = JsonConfiguration.fromFile(Path.of(LANG_PATH+"en_us.json"));
        } catch (Exception e) {
            LANG = JsonConfiguration.of("{}");
            LOGGER.error("Failed to load language file",e);
        }
        phrase = translatable(LANG.getString("misc.toneko.nya"));
        prefix = translatable(LANG.getString("misc.toneko.prefix"));
    }
}
