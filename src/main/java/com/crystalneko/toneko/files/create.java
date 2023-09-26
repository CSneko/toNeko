package com.crystalneko.toneko.files;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class create {
    public Boolean createNewFile(String filePath) {

        File file = new File(filePath);

        if (!file.exists()) {
            try {
                return file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            return true;
        }

        return false;
    }

    /**
     * 在yml中创建键
     * @param key 键名
     * @param value 键值
     * @param config yml文件实例
     * @param configFile yml文件
     */

    public Boolean createNewKey(String key, Object value, YamlConfiguration config, File configFile) {
        // 检查配置文件是否已经包含了指定键
        if (!config.contains(key)) {
            // 在配置文件中创建新键
            config.set(key, value);
            // 保存配置文件
            try {
                config.save(configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return  true;
        }else{
            return false;
        }
    }
}
