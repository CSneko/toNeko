package com.crystalneko.toneko;

import com.crystalneko.toneko.bstats.Metrics;
import com.crystalneko.toneko.command.NekoCommand;
import com.crystalneko.toneko.command.TabCompleter;
import com.crystalneko.toneko.command.ToNekoCommand;
import com.crystalneko.toneko.event.PlayerAttack;
import com.crystalneko.toneko.event.PlayerDeath;
import com.crystalneko.toneko.event.PlayerJoin;
import com.crystalneko.toneko.event.PlayerQuit;
import com.crystalneko.toneko.files.create;
import com.crystalneko.toneko.chat.nekoed;
import com.crystalneko.toneko.files.downloadPlugin;
import com.crystalneko.toneko.items.getStick;

import com.crystalneko.toneko.items.stickLevel2;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;


public final class ToNeko extends JavaPlugin {
    private create createFile;
    private nekoed catedChat;
    private getStick getstick;
    private PlayerDeath playerDeath;
    private downloadPlugin DownloadPlugin;
    private PlayerJoin playerJoin;
    private PlayerAttack playerAttack;
    public static FileConfiguration languageConfig;
    private String language;
    private PlayerQuit playerQuit;
    public stickLevel2 stickLevel;
    public static Logger logger;


    @Override
    public void onEnable() {
        //温馨提示：代码中所有的判断是否为猫娘都是判断是否有主人，这意味着猫娘必须有主人，否则就不被判断为猫娘

        //获取logger
        logger = Logger.getLogger("toNeko");

        int pluginId = 19899;
        Metrics metrics = new Metrics(this, pluginId);
        //判断是否启用了ctLib
        downloadPlugin.checkAndDownloadPlugin("ctLib","https://w.csk.asia/res/plugins/ctLib.jar");
        //获取config.yml
        checkAndSaveResource("config.yml");
        //更新配置文件
        updateConfig();
        //复制文件
        copyResource();
        //加载语言文件
        loadLanguageFile();
        //初始化下载类
        this.DownloadPlugin = new downloadPlugin(this);
        //检查更新和自动更新
        automaticUpdates();
        //初始化createFile
        createFile = new create();
        //创建数据保存文件
        createFile.createNewFile("plugins/toNeko/nekos.yml");
        //初始化聊天监听器
        this.catedChat = new nekoed(this);
        //初始化厥猫棍获取器
        this.getstick = new getStick(this);
        //注册命令执行器
        getCommand("toneko").setExecutor(new ToNekoCommand(this,getstick));
        getCommand("neko").setExecutor(new NekoCommand());
        getCommand("toneko").setTabCompleter(new TabCompleter());
        getCommand("neko").setTabCompleter(new TabCompleter());
        //注册玩家加入监听器
        this.playerJoin = new PlayerJoin(this);
        //注册玩家退出监听器
        this.playerQuit = new PlayerQuit(this);
        //注册玩家受到攻击监听器
        this.playerAttack = new PlayerAttack(this);
        //初始化死亡监听器
        this.playerDeath = new PlayerDeath(this);
        //注册物品
        stickLevel = new stickLevel2();
        stickLevel.stickLevel2(this);

    }

    @Override
    public void onDisable() {

    }

    //保存资源文件
    private void checkAndSaveResource(String filePath) {
        if (!isFileExists(filePath)) {
            saveResource(filePath, false);
        } else {
        }
    }
    private boolean isFileExists(String filePath) {
        File file = new File(getDataFolder(), filePath);
        return file.exists() && file.isFile();
    }
    //检查更新的方法
    public Boolean checkUpdates() {
        String remoteUrl = "https://w.csk.asia/res/version/toNeko.txt"; // 远程网站中存储版本号的文件地址

        try {
            URL url = new URL(remoteUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String remoteVersion = reader.readLine();

                reader.close();

                // 将远程版本号与插件版本进行比较
                String pluginVersion = getDescription().getVersion(); // 获取插件的版本号
                if (!pluginVersion.equals(remoteVersion)) {
                    // 版本不同，发出更新提醒
                    System.out.println(getMessage("onEnable.new-version"));
                    connection.disconnect();
                    return true;
                } else {
                    // 版本相同，无需更新
                    System.out.println(getMessage("onEnable.up-to-date"));
                    connection.disconnect();
                    return false;
                }
            } else {
                // 请求失败
                System.out.println(getMessage("onEnable.unable-check-update"));
                connection.disconnect();
                return false;
            }
        } catch (IOException e) {
            // 发生异常
            System.out.println(getMessage("onEnable.unable-check-update")+":" + e);
            return false;
        }
    }
    //检查更新和自动更新
    public void automaticUpdates(){

        if(getConfig().getBoolean("automatic-updates")){
            Boolean needUpdate = checkUpdates();
            //判断是否需要更新
            if(needUpdate){
                try {
                    //下载更新文件
                    downloadPlugin.downloadFile("https://w.csk.asia/res/plugins/toNeko.jar","plugins/toNeko" + getDescription().getVersion() + "[+].jar");
                    //删除插件
                    DownloadPlugin.deletePlugin();
                    //启动插件
                    PluginManager pluginManager = Bukkit.getPluginManager();
                    try {
                        pluginManager.loadPlugin(new File("plugins/toNeko" + getDescription().getVersion() + "[+].jar"));
                    } catch (InvalidPluginException e) {
                        throw new RuntimeException(e);
                    } catch (InvalidDescriptionException e) {
                        throw new RuntimeException(e);
                    }
                    pluginManager.enablePlugin(pluginManager.getPlugin("toNeko"));
                } catch (IOException e) {
                    System.out.println(e);
                }
            }
        }
    }
    //配置文件更新
    private void updateConfig() {
        //备份配置文件
        File backupFile = new File("plugins/toNeko/config_old.yml");

        //加载配置
        File configFile = new File("plugins/toNeko/config.yml");
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
        InputStream defaultConfigStream = getResource("config.yml"); // 默认配置文件的输入流
        try {
            Files.copy(defaultConfigStream, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        File newConfigFile = new File("plugins/toNeko/config.yml");
        YamlConfiguration newConfig = YamlConfiguration.loadConfiguration(newConfigFile);
        //将新的配置项写入到配置文件中
        newConfig.set("automatic-updates", ifConfig("automatic-updates"));
        newConfig.set("online", ifConfig("online"));
        newConfig.set("language", ifConfig("language"));
        try {
            newConfig.save(configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
    private String ifConfig(String option){
        File oldConfigFile = new File("plugins/toNeko/config_old.yml");
        YamlConfiguration oldConfig = YamlConfiguration.loadConfiguration(oldConfigFile);
        if(oldConfig.getString(option) != null){
            return oldConfig.getString(option);
        }else {
            File configFile = new File("plugins/toNeko/config.yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            return config.getString(option);
        }
    }
    //复制资源
    private void copyResource(){
        // 创建目标文件夹
        File targetFolder = new File(getDataFolder().getParentFile(), "toNeko");
        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }
        saveResource("language/zh_cn.yml", true);
        saveResource("language/en_us.yml", true);
    }
    private void loadLanguageFile() {
        // 获取配置文件中的语言选项
        language = getConfig().getString("language");

        // 根据语言选项加载对应的语言文件
        File languageFile = new File(getDataFolder(), "language/" + language + ".yml");
        if (!languageFile.exists()) {
            saveResource("language/" + language + ".yml", false);
        }

        languageConfig = YamlConfiguration.loadConfiguration(languageFile);
    }

    // 获取翻译内容的方法
    public static String getMessage(String key) {
        return languageConfig.getString(key);
    }

}

