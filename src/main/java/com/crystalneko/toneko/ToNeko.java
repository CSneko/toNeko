package com.crystalneko.toneko;

import com.crystalneko.toneko.command.NekoCommand;
import com.crystalneko.toneko.command.ToNekoCommand;
import com.crystalneko.toneko.event.PlayerAttack;
import com.crystalneko.toneko.event.PlayerDeath;
import com.crystalneko.toneko.event.PlayerJoin;
import com.crystalneko.toneko.files.create;
import com.crystalneko.toneko.chat.nekoed;
import com.crystalneko.toneko.files.downloadPlugin;
import com.crystalneko.toneko.items.getStick;

import org.bukkit.Bukkit;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public final class ToNeko extends JavaPlugin {
    private create createFile;
    private nekoed catedChat;
    private getStick getstick;
    private PlayerDeath playerDeath;
    private downloadPlugin DownloadPlugin;
    private PlayerJoin playerJoin;
    private PlayerAttack playerAttack;


    @Override
    public void onEnable() {
        //温馨提示：代码中所有的判断是否为猫娘都是判断是否有主人，这意味着猫娘必须有主人，否则就不是猫娘
        int pluginId = 19899;
        Metrics metrics = new Metrics(this, pluginId);
        //判断是否启用了ctLib
        downloadPlugin.checkAndDownloadPlugin("ctLib","https://w.csk.asia/res/plugins/ctLib.jar");
        //获取config.yml
        checkAndSaveResource("config.yml");
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
        //初始化死亡监听器
        this.playerDeath = new PlayerDeath(this);
        //注册命令执行器
        getCommand("toneko").setExecutor(new ToNekoCommand(this,getstick));
        getCommand("neko").setExecutor(new NekoCommand());
        //注册玩家加入监听器
        this.playerJoin = new PlayerJoin(this);
        //注册玩家受到攻击监听器
        this.playerAttack = new PlayerAttack(this);
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
                    System.out.println("有新版本可以下载，请前往 https://github.com/CSneko/toneko 下载");
                    connection.disconnect();
                    return true;
                } else {
                    // 版本相同，无需更新
                    System.out.println("当前已是最新版本");
                    connection.disconnect();
                    return false;
                }
            } else {
                // 请求失败
                System.out.println("无法检查更新");
                connection.disconnect();
                return false;
            }
        } catch (IOException e) {
            // 发生异常
            System.out.println("无法检查更新:" + e);
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
}
