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
import static org.cneko.toneko.common.Bootstrap.MODID;
import static org.cneko.toneko.common.util.ConfigUtil.CONFIG;
import static org.cneko.toneko.common.util.LanguageUtil.*;

public class FabricLanguageImpl implements Language{
    @Override
    public void load() {
        try {
            List<String> languages = List.of("en_us","zh_cn","ko_kr");
            language = CONFIG.getString("language");
            // 创建文件夹如果不存在
            FileUtil.CreatePath(LANG_PATH);
            // 删除旧语言文件
            for (String lang : languages){
                FileUtil.DeleteFile(LANG_PATH+lang+".json");
            }

            // 复制语言文件
            ResourceManager resourceManager = ModMeta.INSTANCE.getServer().getResourceManager();
            for (String lang : languages) {
                Identifier id = Identifier.of(MODID, "lang/"+lang);
                try {
                    // 使用ResourceManager获取资源
                    Resource resource = resourceManager.getResource(id).orElseThrow(() -> new RuntimeException("Resource not found"));
                    // 创建一个BufferedReader来读取资源
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        StringBuilder content = new StringBuilder();
                        while ((line = reader.readLine()) != null) {
                            content.append(line).append("\n");
                        }
                        // 将读取到的内容写入文件
                        FileUtil.WriteFile(LANG_PATH+lang+".json", content.toString());
                    }
                } catch (IOException e) {
                    LOGGER.error(e);
                }
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
