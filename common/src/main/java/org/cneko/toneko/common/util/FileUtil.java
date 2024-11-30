package org.cneko.toneko.common.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.cneko.toneko.common.Bootstrap.LOGGER;

@SuppressWarnings("all")
public class FileUtil {
    // 创建文件及文件夹，文件已存在则不创建
    public static void CreateFile(String file){
        try{
            java.io.File f = new java.io.File(file);
            if(!f.exists()){
                f.getParentFile().mkdirs();
                f.createNewFile();
            }
        }catch(Exception ignored){
        }
    }
    // 创建路径
    public static void CreatePath(String path){
        try{
            java.io.File f = new java.io.File(path);
            if(!f.exists()){
                f.mkdirs();
            }
        }catch(Exception ignored){
        }
    }

    // 文件是否存在
    public static boolean FileExists(String file){
        try{
            java.io.File f = new java.io.File(file);
            return f.exists();
        }catch(Exception ignored){
            return false;
        }
    }

    // 覆盖文件原本的内容强制写入，不存在则创建
    public static void WriteFile(String file, String content){
        try{
            java.io.File f = new java.io.File(file);
            if(!f.exists()){
                f.getParentFile().mkdirs();
                f.createNewFile();
            }
            FileWriter fw = new FileWriter(f, StandardCharsets.UTF_8);  // Fix issue#44
            fw.write(content);
            fw.close();
        }catch(Exception ignored){
        }
    }

    // 删除文件
    public static void DeleteFile(String file){
        try{
            java.io.File f = new java.io.File(file);
            if(f.exists()){
                f.delete();
            }
        }catch(Exception ignored){
        }
    }

    public static void copyResource(String resourcePath,String targetPath) {
        try {
            // 使用ClassLoader读取资源文件
            InputStream inputStream = FileUtil.class.getClassLoader().getResourceAsStream(resourcePath);

            if (inputStream != null) {
                // 读取资源文件内容，指定编码为UTF-8
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                reader.close();
                // 将读取到的内容写入文件
                FileUtil.WriteFile(targetPath, content.toString());
            } else {
                LOGGER.error("Cannot find resource: {}", resourcePath);
            }
        } catch (IOException e) {
            LOGGER.error("Cannot copy resource: ", e);
        }
    }

    /**
     * 获取指定目录下的所有文件（包括子目录中的文件）。
     *
     * @param path 目录路径
     * @return 文件列表
     */
    public static List<File> getFiles(String path) {
        File directory = new File(path);
        List<File> files = new ArrayList<>();
        if (directory.exists() && directory.isDirectory()) {
            getFilesRecursively(directory, files);
        }
        return files;
    }

    public static int readNumberFromFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine();
            if (line != null) {
                return Integer.parseInt(line);
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static String readStringFromFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 递归获取目录下的所有文件。
     *
     * @param directory 当前目录
     * @param files 文件列表
     */
    private static void getFilesRecursively(File directory, List<File> files) {
        File[] entries = directory.listFiles();
        if (entries != null) {
            for (File entry : entries) {
                if (entry.isDirectory()) {
                    getFilesRecursively(entry, files);
                } else {
                    files.add(entry);
                }
            }
        }
    }
}
