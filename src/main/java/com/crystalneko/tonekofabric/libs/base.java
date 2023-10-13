package com.crystalneko.tonekofabric.libs;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class base {
    //--------------------------------------------------------获取世界名称---------------------------------------------
    public static final String getWorldName() {
        MinecraftServer server = (MinecraftServer) FabricLoader.getInstance().getGameInstance();
        if (server != null) {
            String worldName = server.getSaveProperties().getLevelName();
            return worldName;
        } else {
            return "publicWorld";
        }
    }
    /**创建文件和目录
    *示例：createFileInDirectory("path/to","/file.txt")
     * @param directoryPath 文件路径
     * @param fileName 文件名
     * @return 是否成功创建
     */
    public static Boolean createFileInDirectory(String directoryPath,String fileName) throws IOException {
            String filePath = directoryPath + fileName;

            // 创建目录
            Path directory = Paths.get(directoryPath);
            Files.createDirectories(directory);

            // 创建文件
            Path file = Paths.get(filePath);
            if (!Files.exists(file)) {
                Files.createFile(file);
                return true;
            } else {
                return false;
            }
    }
    /**
     * 获取猫娘主人
     * @param neko 猫娘名称
     * @param worldName 世界名称
     * @return 猫娘的主人（没有则返回null）
     */
    public static String isNekoHasOwner(String neko,String worldName){
        // 加载数据文件
        File dataFile = new File("ctlib/toneko/nekos.yml");
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        //判断是否有主人
        if(data.getString(worldName + "." + neko + ".owner") != null){
            return data.getString(worldName + "." + neko + ".owner");
        } else {
            return null;
        }
    }
    public static void setPlayerNeko(String neko,String worldName,String owner) {
        // 加载数据文件
        File dataFile = new File("ctlib/toneko/nekos.yml");
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        //设置主人的值
        data.set(worldName + "." + neko + ".owner",owner);
    }
}
