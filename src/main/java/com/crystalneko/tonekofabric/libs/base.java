package com.crystalneko.tonekofabric.libs;

import com.crystalneko.tonekofabric.ToNekoFabric;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.cneko.ctlib.common.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static com.crystalneko.tonekofabric.api.Messages.translatable;
import static org.cneko.ctlib.common.util.LocalDataBase.Connections.sqlite;

public class base {
    public static YamlConfiguration config;
    public static String dataFolder = "ctlib/toneko";
    public static String language;
    public static YamlConfiguration languageConfig;
    //public static Boolean clientLanguage;
    public base(){
        create();  //创建必要目录
        Path configFile = Path.of( dataFolder +"/config.yml");
        if(!Files.exists(configFile)){copyResource("/assets/toneko/config.yml",dataFolder, "assets/toneko/config.yml");}  //如果配置文件不存在，则复制到文件夹中
        try {
            config = new YamlConfiguration(configFile);
        } catch (IOException e) {
            System.out.println("无法加载配置文件:"+e.getMessage());
        }
//        //读取语言文件
//        loadLanguageFile();
//        //是否使用客户端语言
//        clientLanguage = config.getBoolean("client-language");
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
        createDirectoryIfNotExists(Path.of(dataFolder));
        createDirectoryIfNotExists(Path.of(dataFolder + "/language"));
        createDirectoryIfNotExists(Path.of(dataFolder + "/assets"));
        createDirectoryIfNotExists(Path.of(dataFolder + "/assets/toneko"));
        //删除语言文件
        deleteFileIfExists(Path.of(dataFolder + "/language/zh_cn.yml"));
        deleteFileIfExists(Path.of(dataFolder + "/language/en_us.yml"));

    }

    private void deleteFileIfExists(Path filePath) {
        if (Files.exists(filePath)) {
            try {
                Files.delete(filePath);
            } catch (IOException e) {
                System.out.println("无法删除语言文件: " + e.getMessage());
            }
        }
    }


    private void createDirectoryIfNotExists(Path directoryPath) {
        if (!Files.exists(directoryPath)) {
            try {
                Files.createDirectory(directoryPath);
            } catch (IOException e) {
                System.out.println("Can not create path: " + e.getMessage());
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
            //移动到ctlib/toneko/config.yml
            Files.move(Path.of(dataFolder +"/assets/toneko/config.yml"),Path.of(dataFolder +"/config.yml"));
        } catch (IOException e) {
            System.out.println("无法复制资源文件" + e);
        }
    }
    private void loadLanguageFile() {
        // 获取配置文件中的语言选项
        language = config.getString("language");
        // 根据语言选项加载对应的语言文件
        Path languageFile = Path.of(dataFolder +"/language/" + language + ".yml");
        if (!Files.exists(languageFile)) {
            copyResource("/language/" + language + ".yml", dataFolder+"/language",language + ".yml");
        }

        try {
            languageConfig = new YamlConfiguration(languageFile);
        } catch (IOException e) {
            System.out.println("无法加载语言文件:" + e.getMessage());
        }
    }





}
