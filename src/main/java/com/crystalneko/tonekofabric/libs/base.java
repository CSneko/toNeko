package com.crystalneko.tonekofabric.libs;

import com.crystalneko.ctlibfabric.sql.sqlite;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.source.doctree.SeeTree;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.Language;

import java.io.File;
import java.io.FileWriter;
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
    /*public static String getJson(String filePath,String jsonPath){
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            File jsonFile = new File(filePath);
            // 将JSON数据读取为JsonNode对象
            JsonNode rootNode = objectMapper.readTree(jsonFile);

            if (rootNode.has(jsonPath)) {
                return rootNode.get(jsonPath).asText();
            } else {
                // 处理节点不存在的情况
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }
    public static void setJson(String filePath, String jsonPath, String jsonValue) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            File jsonFile = new File(filePath);
            boolean fileExists = jsonFile.exists();

            // 如果文件不存在或者为空文件，则创建新的 JSON 结构
            if (!fileExists || jsonFile.length() == 0) {
                ObjectNode rootNode = objectMapper.createObjectNode();
                setJsonNodeValue(rootNode, jsonPath, jsonValue);

                String updatedJson = objectMapper.writeValueAsString(rootNode);

                FileWriter writer = new FileWriter(jsonFile);
                writer.write(updatedJson);
                writer.close();

                return;
            }

            // 将JSON数据读取为JsonNode对象
            JsonNode rootNode = objectMapper.readTree(jsonFile);

            setJsonNodeValue(rootNode, jsonPath, jsonValue);

            // 将修改后的JsonNode对象转换为JSON字符串
            String updatedJson = objectMapper.writeValueAsString(rootNode);

            // 写入更新后的JSON字符串至文件
            FileWriter writer = new FileWriter(jsonFile);
            writer.write(updatedJson);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void setJsonNodeValue(JsonNode rootNode, String jsonPath, String jsonValue) {
        String[] pathSegments = jsonPath.split("/");
        ObjectNode currentNode = (ObjectNode) rootNode;

        for (int i = 1; i < pathSegments.length - 1; i++) {
            String segment = pathSegments[i];
            if (!currentNode.has(segment) || !currentNode.get(segment).isObject()) {
                currentNode.set(segment, currentNode.objectNode());
            }
            currentNode = (ObjectNode) currentNode.get(segment);
        }

        String lastSegment = pathSegments[pathSegments.length - 1];
        currentNode.put(lastSegment, jsonValue);
    }*/
    public static Text getStringLanguage(String key, String[] replace){
        MutableText TextResult = Text.translatable(key,replace);
        return TextResult;
    }





}
