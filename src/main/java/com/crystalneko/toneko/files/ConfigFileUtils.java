package com.crystalneko.toneko.files;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.*;

public class ConfigFileUtils {

    public static Boolean createNewFile(String filePath) {
        File file = new File(filePath);

        if (!file.exists()) {
            // 检查文件所在的文件夹是否存在，不存在则创建
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

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

    public static Boolean createNewKey(String key, Object value, YamlConfiguration config, File configFile) {
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

    //用于判断和设置yaml中某个键是否存在并设置值
    public static void setValue(String keyToSet , Object value,File file) {
        YamlConfiguration yamlConfiguration = new YamlConfiguration();

        try {
            yamlConfiguration.load(file);
            yamlConfiguration.set(keyToSet, value);
            yamlConfiguration.save(file);
        } catch (IOException | InvalidConfigurationException e) {
        }
    }

    //设置某个值为null
    public static void setNullValue(File file, String keyToSetNull) {
        YamlConfiguration yamlConfiguration = new YamlConfiguration();

        try {
            yamlConfiguration.load(file);
            if (yamlConfiguration.contains(keyToSetNull)) {
                yamlConfiguration.set(keyToSetNull, null);
                yamlConfiguration.save(file);
            } else {
            }
        } catch (IOException | InvalidConfigurationException e) {
            System.out.println(e.getMessage());
        }
    }
}
