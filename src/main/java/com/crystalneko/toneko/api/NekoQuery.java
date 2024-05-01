package com.crystalneko.toneko.api;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;



public class NekoQuery {
    YamlConfiguration data;
    String name;
    public NekoQuery(String name){
        File dataFile = new File("plugins/toNeko/nekos.yml");
        data = YamlConfiguration.loadConfiguration(dataFile);
        this.name = name;
    }

    /**
     * 判断是否为Neko
     */
    public boolean isNeko(){
        return hasOwner();
    }

    /**
     * 获取猫娘是否有主人
     */
    public boolean hasOwner(){
        return data.getString(name+ ".owner") != null;
    }
    /**
     * 获取猫娘的主人名称

     * @return 主人名称
     */
    public String getOwner(){
        return data.getString(name+ ".owner");
    }

    /**
     * 获取猫娘对主人的别名
     */
    public List<String> getAlias(){
        List<String> aliases = new ArrayList<>();

        if (data.getList(name + ".aliases") != null) {
            aliases = data.getStringList(name + ".aliases");
        } else {
            aliases.add("Crystal_Neko");
        }
        return aliases;
    }

    public YamlConfiguration getData(){
        return data;
    }
}
