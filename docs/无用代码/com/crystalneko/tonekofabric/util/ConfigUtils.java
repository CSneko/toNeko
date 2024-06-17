package com.crystalneko.tonekofabric.util;

import org.cneko.ctlib.common.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static com.google.common.io.Resources.getResource;

public class ConfigUtils {
    public static void updateConfig() throws IOException{
        //备份配置文件
        File backupFile = new File("ctlib/toNeko/config_old.yml");

        //加载配置
        File configFile = new File("ctlib/toNeko/config.yml");
        //判断旧配置是否存在
        if(backupFile.exists()){
            backupFile.delete();
        }
        try {
            Files.copy(configFile.toPath(), backupFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //使用默认配置文件替换旧的配置文件
        InputStream defaultConfigStream = getResource("assets/toneko/config.yml").openStream(); // 默认配置文件的输入流
        try {
            Files.copy(defaultConfigStream, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        File newConfigFile = new File("ctlib/toNeko/config.yml");
        YamlConfiguration newConfig = new YamlConfiguration(newConfigFile);
        //将新的配置项写入到配置文件中
        newConfig.set("automatic-updates", ifConfig("automatic-updates"));
        newConfig.set("language", ifConfig("language"));
        newConfig.set("AI.enable", ifConfig("AI.enable"));
        newConfig.set("AI.API", ifConfig("AI.API"));
        newConfig.set("AI.prompt", ifConfig("AI.prompt"));
        newConfig.set("chat.enable", ifConfig("chat.enable"));
        newConfig.set("chat.format", ifConfig("chat.format"));
        try {
            newConfig.save(configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object ifConfig(String option){
        try {
            File oldConfigFile = new File("ctlib/toNeko/config_old.yml");
            YamlConfiguration oldConfig = new YamlConfiguration(oldConfigFile);
            if (oldConfig.get(option) != null) {
                return oldConfig.get(option);
            } else {
                File configFile = new File("ctlib/toNeko/config.yml");
                YamlConfiguration config = new YamlConfiguration(configFile);
                return config.get(option);
            }
        }catch (IOException e){
            System.out.println("读取配置文件失败:"+e.getMessage());
            return null;
        }
    }
}
