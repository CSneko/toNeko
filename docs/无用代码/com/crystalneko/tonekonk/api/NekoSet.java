package com.crystalneko.tonekonk.api;

import cn.nukkit.utils.Config;

import java.io.File;

public class NekoSet {
    public static void setNeko(String name, String owner) {
        // 指定配置文件路径
        String filePath = "plugins/toNeko/neko.yml";

        // 创建一个 Config 对象来处理配置文件
        Config config = new Config(new File(filePath), Config.YAML);

        // 设置配置文件中的数据
        config.set(name + ".owner", owner);

        // 保存修改后的配置文件
        config.save();
    }

    public static boolean isNeko(String name) {
        // 指定配置文件路径
        String filePath = "plugins/toNeko/neko.yml";

        // 创建一个 Config 对象来处理配置文件
        Config config = new Config(new File(filePath), Config.YAML);

        // 读取配置文件中的数据并判断是否存在
        return config.exists(name + ".owner");
    }
}
