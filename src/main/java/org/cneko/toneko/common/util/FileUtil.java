package org.cneko.toneko.common.util;

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
            java.io.FileWriter fw = new java.io.FileWriter(f);
            fw.write(content);
            fw.close();
        }catch(Exception ignored){
        }
    }
}
