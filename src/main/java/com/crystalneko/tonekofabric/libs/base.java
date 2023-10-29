package com.crystalneko.tonekofabric.libs;

import com.crystalneko.ctlibfabric.sql.sqlite;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class base {
    //--------------------------------------------------------获取世界名称---------------------------------------------
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
        //判断是否为猫娘
        if(sqlite.checkValueExists(worldName+"Nekos","neko",neko)){
            return sqlite.getColumnValue(worldName+"Nekos","owner","neko",neko);
        } else {
            return null;
        }
    }
    public static void setPlayerNeko(String neko,String worldName,String owner) {
        //设置值
        sqlite.saveData(worldName+"Nekos","neko",neko);
        //设置主人的值
        sqlite.saveDataWhere(worldName+"Nekos","owner","neko",neko,owner);
    }
    public static Text getStringLanguage(String key, String[] replace){
        return Text.translatable(key, (Object[]) replace);
    }





}
