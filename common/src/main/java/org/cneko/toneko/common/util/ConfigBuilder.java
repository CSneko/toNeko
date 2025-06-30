package org.cneko.toneko.common.util;

import com.google.gson.Gson;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.*;

import static org.cneko.toneko.common.Bootstrap.LOGGER;

public class ConfigBuilder {
    private final Path path;
    private JsonConfiguration config;
    private final Map<String,Entry> defaults = new LinkedHashMap<>();
    public ConfigBuilder(Path path){
        this.path = path;
        // 尝试读取文件
        try {
            config = new JsonConfiguration(path);
        } catch (Exception e) {
            // 出现错误，创建一个空的配置文件
            config = new JsonConfiguration("{}");
        }
    }

    private ConfigBuilder add(String key, Entry defaultValue, String comment) {
        defaults.put(key, defaultValue);
        return this;
    }

    private ConfigBuilder add(String key, Entry defaultValue, String... comments) {
        String combinedComment = String.join("\n", comments);
        return this.add(key, defaultValue, combinedComment);
    }

    public ConfigBuilder addString(String key, String value,String url, String comment) {
        return this.add(key, Entry.of(value, comment,url), comment);
    }

    public ConfigBuilder addString(String key, String value,String url, String... comments) {
        return this.add(key, Entry.of(value, String.join("\n", comments),url), comments);
    }

    public ConfigBuilder addBoolean(String key, Boolean value,String url, String comment) {
        return this.add(key, Entry.of(value, comment,url), comment);
    }

    public ConfigBuilder addBoolean(String key, Boolean value,String url, String... comments) {
        return this.add(key, Entry.of(value, String.join("\n", comments),url), comments);
    }
    public void setBoolean(String key, boolean value) {
        config.set(key, value);
        config.save(path);

    }
    public void setString(String key, String value) {
        config.set(key, value);
        config.save(path);
    }
    public Entry get(String key){
        return defaults.get(key);
    }
    public Entry getExist(String key) {
        Object value = config.get(key);
        if (value instanceof com.google.gson.JsonPrimitive) {
            com.google.gson.JsonPrimitive primitive = (com.google.gson.JsonPrimitive) value;
            if (primitive.isBoolean()) {
                value = primitive.getAsBoolean();
            } else if (primitive.isNumber()) {
                value = primitive.getAsNumber();
            } else {
                value = primitive.getAsString();
            }
        }
        return Entry.of(value, get(key).comment, config.getString("url"));
    }
    public String getKey(Entry entry){
        for (String key : defaults.keySet()) {
            if (defaults.get(key).equals(entry)) {
                return key;
            }
        }
        return null;
    }

    public List<String> getKeys() {
        return new ArrayList<>(defaults.keySet());
    }



    public ConfigBuilder build() {
        for (String key : defaults.keySet()) {
            Entry entry = defaults.get(key);
            if (!config.contains(key)) {
                //config.addComment(key, entry.comment);
                config.set(key, entry.get());
            }
        }
        try {
            Path configPath = Path.of("config/");
            if (!Files.exists(configPath)){
                Files.createDirectories(configPath);
            }
            config.save(path);
        } catch (IOException e) {
            LOGGER.error("Unable to save config file", e);
        }
        return this;
    }



    public JsonConfiguration createConfig(){
        try {
            return new JsonConfiguration(path);
        } catch (IOException e) {
            return config;
        }
    }



    public static ConfigBuilder create(Path path){
        return new ConfigBuilder(path);
    }

    public static class Entry{
        private final Object value;
        private final Types type;
        private final String comment;
        private String url;
        public Entry(Object value, Types type,String comment){
            this.value = value;
            this.type = type;
            this.comment = comment;
        }
        public Entry(Object value, Types type,String comment,@Nullable String url){
            this(value,type,comment);
            this.url = url;
        }
        public Object get(){
            return value;
        }
        public Types type(){
            return type;
        }
        public String comment(){
            return comment;
        }
        public Entry setUrl(String url){
            this.url = url;
            return this;
        }
        public String url(){
            return url;
        }
        public String string(){
            return (String) value;
        }
        public Number number(){
            return (Number) value;
        }
        public boolean bool(){
            return (boolean) value;
        }
        public ConfigBuilder config(){
            return (ConfigBuilder) value;
        }

        public static Entry of(Object value, String comment, String url) {
            if (value instanceof com.google.gson.JsonPrimitive) {
                com.google.gson.JsonPrimitive primitive = (com.google.gson.JsonPrimitive) value;
                if (primitive.isString()) {
                    return new Entry(primitive.getAsString(), Types.STRING, comment, url);
                } else if (primitive.isBoolean()) {
                    return new Entry(primitive.getAsBoolean(), Types.BOOLEAN, comment, url);
                } else if (primitive.isNumber()) {
                    return new Entry(primitive.getAsNumber(), Types.NUMBER, comment, url);
                }
            } else if (value instanceof String) {
                return new Entry(value, Types.STRING, comment, url);
            } else if (value instanceof Number) {
                return new Entry(value, Types.NUMBER, comment, url);
            } else if (value instanceof Boolean) {
                return new Entry(value, Types.BOOLEAN, comment, url);
            } else if (value instanceof List<?>) {
                return new Entry(value, Types.LIST, comment, url);
            } else if (value instanceof ConfigBuilder) {
                return new Entry(value, Types.CONFIG, comment, url);
            }
            throw new IllegalArgumentException("Invalid type: " + (value != null ? value.getClass().getName() : "null"));
        }

        public enum Types{
            STRING,
            NUMBER,
            BOOLEAN,
            LIST,
            CONFIG
        }
    }


}
