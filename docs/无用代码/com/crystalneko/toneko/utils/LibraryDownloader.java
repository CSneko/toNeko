package com.crystalneko.toneko.utils;

import com.crystalneko.toneko.ToNeko;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.cneko.toneko.common.util.scheduled.SchedulerPoolProvider;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class LibraryDownloader {
    private ToNeko plugin;

    public LibraryDownloader(ToNeko plugin){
        this.plugin = plugin;
    }
    //下载插件
    public static void checkAndDownloadPlugin(String pluginName, String pluginUrl) {
        PluginManager pluginManager = Bukkit.getPluginManager();
        Plugin plugin = pluginManager.getPlugin(pluginName);

        if (plugin == null) {
            Bukkit.getLogger().info(pluginName+"插件未启动，开始下载并启动插件...");

            try {
                downloadPlugin(pluginUrl, pluginName);
                pluginManager.loadPlugin(new File("plugins/" + pluginName + ".jar"));
                pluginManager.enablePlugin(pluginManager.getPlugin(pluginName));
                Bukkit.getLogger().info("插件下载并启动成功！");
            } catch (Exception e) {
                Bukkit.getLogger().severe("插件下载或启动失败：" + e.getMessage());
            }
        } else {
            Bukkit.getLogger().info(pluginName+"插件已启动！");
        }
    }

    private static void downloadPlugin(String pluginUrl, String pluginName) throws IOException {
        URL url = new URL(pluginUrl);
        BufferedInputStream in = new BufferedInputStream(url.openStream());
        FileOutputStream fileOutputStream = new FileOutputStream("plugins/" + pluginName + ".jar");

        byte[] dataBuffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
            fileOutputStream.write(dataBuffer, 0, bytesRead);
        }

        in.close();
        fileOutputStream.close();
    }

    //删除插件
    public void deletePlugin() {
        SchedulerPoolProvider.getINSTANCE().scheduleSync(() -> {
            // 获取插件文件的路径
            File pluginFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
            try {
                // 延迟一些时间，等待插件释放所有资源
                Thread.sleep(1000);
                // 删除插件文件
                if (pluginFile.delete()) {
                    // 如果成功删除插件文件，则卸载插件
                    Bukkit.getPluginManager().disablePlugin(plugin);
                } else {
                    plugin.getLogger().severe("删除插件文件失败！");
                }
            } catch (Exception e) {
                plugin.getLogger().severe("卸载插件失败：" + e.getMessage());
            }
        },1,0,0,null);
    }

    //下载文件的方法
    public static void downloadFile(String fileUrl, String saveFilePath) throws IOException {
        URL url = new URL(fileUrl);
        URLConnection connection = url.openConnection();
        BufferedInputStream in = new BufferedInputStream(connection.getInputStream());

        File outputFile = new File(saveFilePath);
        File parentDir = outputFile.getParentFile();
        if (parentDir != null) {
            parentDir.mkdirs();
        }

        FileOutputStream out = new FileOutputStream(outputFile);

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }

        out.close();
        in.close();
    }



}
