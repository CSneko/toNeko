package org.cneko.toneko.fabric.impl;

import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.cneko.toneko.fabric.ModMeta;
import org.cneko.toneko.common.util.FileUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.cneko.toneko.common.Bootstrap.LOGGER;
import static org.cneko.toneko.common.Bootstrap.MODID;
import static org.cneko.toneko.common.util.ConfigUtil.CONFIG_FILE;
import static org.cneko.toneko.common.util.ConfigUtil.Config;

public class FabricConfigImpl implements Config{
    @Override
    public void load() {
        try (InputStream inputStream = FabricConfigImpl.class.getClassLoader().getResourceAsStream("assets/toneko/config.yml");
             Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
             FileUtil.WriteFile(CONFIG_FILE, scanner.useDelimiter("\\A").next());
        } catch (IOException e) {
            LOGGER.error(e);
        }
        /*
        ResourceManager resourceManager = ModMeta.INSTANCE.getServer().getResourceManager();
        Identifier id = Identifier.of(MODID, "assets/toneko/config.yml");
        try {
            // 使用ResourceManager获取资源
            Resource resource = resourceManager.getResourceOrThrow(id);
            // 创建一个BufferedReader来读取资源
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                StringBuilder content = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                // 将读取到的内容写入文件
                FileUtil.WriteFile(CONFIG_FILE, content.toString());
            }
        } catch (IOException e) {
            LOGGER.error(e);
        }

         */
    }
}
