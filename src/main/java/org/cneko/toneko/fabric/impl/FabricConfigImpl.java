package org.cneko.toneko.fabric.impl;

import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.cneko.toneko.fabric.ModMeta;
import org.cneko.toneko.common.util.FileUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.cneko.toneko.common.Bootstrap.LOGGER;
import static org.cneko.toneko.common.Bootstrap.MODID;
import static org.cneko.toneko.common.util.ConfigUtil.CONFIG_FILE;
import static org.cneko.toneko.common.util.ConfigUtil.Config;

public class FabricConfigImpl implements Config{
    @Override
    public void load() {
        ResourceManager resourceManager = ModMeta.INSTANCE.getServer().getResourceManager();
        Identifier id = Identifier.of(MODID, "config.yml");
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
                FileUtil.WriteFile(CONFIG_FILE, content.toString());
            }
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }
}
