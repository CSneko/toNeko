package com.crystalneko.tonekofabric.libs;

import com.crystalneko.ctlibPublic.File.YamlConfiguration;
import com.crystalneko.ctlibPublic.sql.sqlite;
import com.crystalneko.tonekofabric.ToNekoFabric;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class base {
    public static YamlConfiguration config;
    public static String dataFolder = "ctlib/toneko";
    public static String language;
    public static YamlConfiguration languageConfig;
    public static Boolean clientLanguage;
    public base(){
        create();  //创建必要目录
        Path configFile = Path.of("ctlib/toneko/config.yml");
        if(!Files.exists(configFile)){copyResource("/config.yml",dataFolder,"config.yml");}  //如果配置文件不存在，则复制到文件夹中
        try {
            config = new YamlConfiguration(configFile);
        } catch (IOException e) {
            System.out.println("无法加载配置文件:"+e.getMessage());
        }
        //读取语言文件
        loadLanguageFile();
        //是否使用客户端语言
        clientLanguage = config.getBoolean("client-language");
    }
    public static void start(String worldName){
        if(!ToNekoFabric.started){
            if(!sqlite.isTableExists(worldName+"Nekos")) {
                sqlite.createTable(worldName + "Nekos");
            }
            sqlite.addColumn(worldName+"Nekos","neko");
            sqlite.addColumn(worldName+"Nekos","owner");
            sqlite.addColumn(worldName+"Nekos","block");
            sqlite.addColumn(worldName+"Nekos","method");
            sqlite.addColumn(worldName+"Nekos","replace");
            sqlite.addColumn(worldName+"Nekos","xp");
            sqlite.addColumn(worldName+"Nekos","type");
            if(!sqlite.isTableExists(worldName+"NekoEnt")) {
                sqlite.createTable(worldName + "NekoEnt");
            }
            sqlite.addColumn(worldName + "NekoEnt", "uuid");
            sqlite.addColumn(worldName + "NekoEnt", "name");
        }
    }
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
        sqlite.saveDataWhere(worldName+"Nekos","xp","neko",neko,"0");
        //设置主人的值
        sqlite.saveDataWhere(worldName+"Nekos","owner","neko",neko,owner);
    }
    public static String getOwner(String neko,String worldName){
        //获取主人名称
        String owner = sqlite.getColumnValue(worldName+"Nekos","owner","neko",neko);
        if(owner != null){
            return owner;
        }else {
            return translatable("base.null.owner").getString();
        }
    }
    public static String getPlayerName(PlayerEntity player){
        String playerName = player.getName().getString();
        playerName = playerName.replace("literal{", "").replace("}", "");
        return playerName;
    }
    public static Text getStringLanguage(String key, String[] replace){
        return Text.translatable(key, (Object[]) replace);
    }
    public static String getWorldName(World world){
        String name = world.toString();
        name = name.replace("[","");
        name = name.replace("]","");
        name = name.replace("ServerLevel","");
        return name;
    }
    public void create(){
        Path path = Path.of("ctlib/toneko");
        if(!Files.exists(path)){
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                System.out.println("can not create path:"+e.getMessage());
            }
        }
        if(!Files.exists(Path.of("ctlib/toneko/language"))){
            try {
                Files.createDirectory(Path.of("ctlib/toneko/language"));
            } catch (IOException e) {
                System.out.println("无法创建文件夹:" +e.getMessage());
            }
        }
        //删除语言文件
        Path zh_cnPath = Path.of("ctlib/toneko/language/zh_cn.yml");
        if (Files.exists(zh_cnPath)){
            try {
                Files.delete(zh_cnPath);
            } catch (IOException e) {
                System.out.println("无法删除语言文件:"+e.getMessage());
            }
        }
        Path en_usPath = Path.of("ctlib/toneko/language/en_us.yml");
        if (Files.exists(zh_cnPath)){
            try {
                Files.delete(en_usPath);
            } catch (IOException e) {
                System.out.println("无法删除语言文件:"+e.getMessage());
            }
        }

    }
    public void copyResource(String resourcePath,String dataFolder,String fileName){
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                System.out.println("无法找到资源文件");
                return;
            }
            // 获取插件的数据文件夹路径
            Path pluginDataFolder = Path.of(dataFolder);
            // 将资源文件复制到插件数据文件夹下的ctlib目录
            Path outputPath = pluginDataFolder.resolve(fileName);
            Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("资源文件复制成功");

        } catch (IOException e) {
            System.out.println("无法复制资源文件" + e);
        }
    }
    private void loadLanguageFile() {
        // 获取配置文件中的语言选项
        language = config.getString("language");
        // 根据语言选项加载对应的语言文件
        Path languageFile = Path.of("ctlib/toneko/language/" + language + ".yml");
        if (!Files.exists(languageFile)) {
            copyResource("/language/" + language + ".yml", dataFolder+"/language",language + ".yml");
        }

        try {
            languageConfig = new YamlConfiguration(languageFile);
        } catch (IOException e) {
            System.out.println("无法加载语言文件:" + e.getMessage());
        }
    }

    // 获取翻译内容的方法
    public static String getMessage(String key) {
        return languageConfig.getString(key);
    }
    public static Text translatable(String key){
        //如果使用客户端语言，则返回客户端语言,否则返回服务端语言
        if(clientLanguage){
            return Text.translatable(key);
        }else {
            String text = getMessage(key);
            Text text1 = Text.of(text);
            return  text1;
        }
    }
    public static Text translatable(String key,String[] replace){
        //如果使用客户端语言，则返回客户端语言,否则返回服务端语言
        if(clientLanguage){
            return Text.translatable(key, (Object[]) replace);
        }else {
            String text = getMessage(key);
            text = text.replace("%d",replace[0]);
            if(replace.length ==2){
                text = text.replace("%c",replace[1]);
            }
            Text text1 = Text.of(text);
            return  text1;
        }
    }





}
