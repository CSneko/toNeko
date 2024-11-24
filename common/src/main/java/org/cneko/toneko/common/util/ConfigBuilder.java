package org.cneko.toneko.common.util;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.cneko.toneko.common.Bootstrap.LOGGER;

public class ConfigBuilder {
    private final Path path;
    private YC config;
    private final Map<String,Entry> defaults = new LinkedHashMap<>();
    public ConfigBuilder(Path path){
        this.path = path;
        // 尝试读取文件
        try {
            config = new YC(path);
        } catch (Exception e) {
            // 出现错误，创建一个空的配置文件
            config = new YC("");
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

    public ConfigBuilder addString(String key, String value, String comment) {
        return this.add(key, Entry.of(value, comment), comment);
    }

    public ConfigBuilder addString(String key, String value, String... comments) {
        return this.add(key, Entry.of(value, String.join("\n", comments)), comments);
    }

    public ConfigBuilder addBoolean(String key, Boolean value, String comment) {
        return this.add(key, Entry.of(value, comment), comment);
    }

    public ConfigBuilder addBoolean(String key, Boolean value, String... comments) {
        return this.add(key, Entry.of(value, String.join("\n", comments)), comments);
    }
    public void setBoolean(String key, boolean value) {
        config.set(key, value);
        try {
            config.saveToFile(path.toFile());
        } catch (IOException ignored) {
        }
    }
    public void setString(String key, String value) {
        config.set(key, value);
        try {
            config.saveToFile(path.toFile());
        } catch (IOException ignored) {
        }
    }
    public Entry get(String key){
        return Entry.of(config.get(key),"");
    }

    public List<String> getKeys() {
        return new ArrayList<>(defaults.keySet());
    }



    public ConfigBuilder build() {
        for (String key : defaults.keySet()) {
            Entry entry = defaults.get(key);
            if (!config.containsNestedKey(key)) { // Use a new method to check nested keys
                config.addComment(key, entry.comment);
                config.set(key, entry.get());
            }
        }
        try {
            config.saveToFile(path.toFile());
        } catch (IOException e) {
            LOGGER.error("Unable to save config file", e);
        }
        return this;
    }



    public YC createConfig(){
        try {
            return YC.fromFile(path);
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
        public Entry(Object value, Types type,String comment){
            this.value = value;
            this.type = type;
            this.comment = comment;
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

        public static Entry of(Object value,String comment){
            if (value instanceof String){
                return new Entry(value,Types.STRING,comment);
            }else if (value instanceof Number){
                return new Entry(value,Types.NUMBER,comment);
            }else if (value instanceof Boolean){
                return new Entry(value,Types.BOOLEAN,comment);
            }else if (value instanceof List<?>){
                return new Entry(value,Types.LIST,comment);
            }else if (value instanceof ConfigBuilder){
                return new Entry(value,Types.CONFIG,comment);
            }
            throw new IllegalArgumentException("Invalid type: " + value.getClass().getName());
        }

        public enum Types{
            STRING,
            NUMBER,
            BOOLEAN,
            LIST,
            CONFIG
        }
    }


    public static class YC {
        private Map<String, Object> data;
        private DumperOptions options;
        private Yaml yaml;


        public YC() {
            data = new LinkedHashMap<>(); // 保持顺序
            options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK); // 使用块样式，更易读
            options.setWidth(200); // 防止过早换行
            yaml = new Yaml(options);

        }

        public YC(String yamlString) {
            this();
            loadFromString(yamlString);
        }

        public YC(File yamlFile) throws IOException {
            this();
            loadFromFile(yamlFile);
        }

        public YC(Path filePath) throws IOException{
            this(filePath.toFile());
        }

        public void loadFromString(String yamlString) {
            data = yaml.load(yamlString);
            if (data == null){
                data = new LinkedHashMap<>();
            }
        }

        public void loadFromFile(File yamlFile) throws IOException {
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(yamlFile))) {
                data = yaml.load(reader);
            }
        }


        public void set(String key, Object value) {
            // 支持嵌套key，例如 "a.b.c"
            String[] keys = key.split("\\.");
            Map<String, Object> current = data;
            for (int i = 0; i < keys.length - 1; i++) {
                if (!current.containsKey(keys[i]) || !(current.get(keys[i]) instanceof Map)) {
                    current.put(keys[i], new LinkedHashMap<>());
                }
                current = (Map<String, Object>) current.get(keys[i]);
            }
            current.put(keys[keys.length - 1], value);
        }

        public String getString(String key) {
            return getString(key, null);
        }

        public String getString(String key, String defaultValue) {
            Object value = get(key);
            return value != null ? value.toString() : defaultValue;
        }


        public Boolean getBoolean(String key) {
            return getBoolean(key, null);
        }

        public Boolean getBoolean(String key, Boolean defaultValue) {
            Object value = get(key);
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else if (value instanceof String) {
                return Boolean.parseBoolean((String) value); // 尝试将字符串解析为布尔值
            }
            return defaultValue;
        }



        private Object get(String key) {
            String[] keys = key.split("\\.");
            Map<String, Object> current = data;
            for (int i = 0; i < keys.length - 1; i++) {
                if (!current.containsKey(keys[i]) || !(current.get(keys[i]) instanceof Map)) {
                    return null;
                }
                current = (Map<String, Object>) current.get(keys[i]);
            }
            return current.get(keys[keys.length - 1]);
        }

        public boolean contains(String key) {
            return data.containsKey(key);
        }

        public boolean containsNestedKey(String key) {
            String[] keys = key.split("\\.");
            Map<String, Object> current = data;
            for (String k : keys) {
                if (!current.containsKey(k)) {
                    return false;
                }
                Object value = current.get(k);
                if (value instanceof Map) {
                    current = (Map<String, Object>) value;
                } else if (!(value instanceof Map) && keys[keys.length -1] != k) {
                    return false;
                }


            }
            return true;
        }

        public void addComment(String key, String... commentLines) {
            // SnakeYaml 不直接支持在特定 key 前添加注释.  变通方法: 使用 Tag.COMMENT
            StringBuilder comment = new StringBuilder();
            for (String line : commentLines) {
                comment.append("# ").append(line).append("\n");
            }
            set(key + ".comment", new Tag("!comment"), comment.toString()); // 添加虚拟 key 用于存储注释
        }


        private void set(String key, Tag tag, Object value) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put(tag.getValue(), value);
            set(key,map);

        }

        @Override
        public String toString() {
            return yaml.dump(data);
        }



        public void saveToFile(File yamlFile) throws IOException {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(yamlFile))) {
                dump(data, writer, 0);  // 使用递归方法处理嵌套结构
            }
        }


        private void dump(Object value, BufferedWriter writer, int indent) throws IOException {
            if (value instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                    String key = entry.getKey().toString();
                    Object val = entry.getValue();

                    // 先处理注释
                    if (key.endsWith(".comment")) {
                        writeComment(writer, (Map<String, String>)val, indent);

                    } else {
                        writeIndent(writer, indent);
                        writer.write(key + ": ");


                        if (val instanceof Map && ((Map<?, ?>) val).containsKey("!comment")) {
                            writeComment(writer, (Map<String, String>) val, indent);

                            // 直接获取非注释的值
                            Set<?> keySet = ((Map<?, ?>) val).keySet();

                            for(Object k:keySet){
                                if(!k.equals("!comment")){
                                    val = ((Map<?, ?>) val).get(k);
                                    break; // 找到非注释值后跳出循环
                                }
                            }

                        }
                        // 再写 key-value 对
                        if(val != null) {
                            if (val instanceof Map || val instanceof List){
                                writer.newLine();
                                dump(val, writer, indent + 2);
                            } else{
                                writer.write(yaml.dump(val).trim());
                                writer.newLine();
                            }

                        }

                    }

                }

            } else if (value instanceof List) {
                for(Object item : (List) value) {
                    writeIndent(writer,indent);
                    writer.write("- ");
                    if(item instanceof  Map || item instanceof List) {
                        writer.newLine();
                        dump(item,writer,indent +2);
                    } else {
                        writer.write(yaml.dump(item).trim());
                        writer.newLine();
                    }


                }


            }
        }

        private void writeComment(BufferedWriter writer, Map<String, String> val, int indent) throws IOException {

            String comment = val.get("!comment");
            for (String line : comment.split("\n")) {
                writeIndent(writer, indent);
                if (!line.trim().startsWith("#")) writer.write("# ");
                writer.write(line.trim());
                writer.newLine();
            }
        }


        private void writeIndent(BufferedWriter writer, int indent) throws IOException {
            for (int i = 0; i < indent; i++) {
                writer.write(" ");
            }
        }




        // 工厂方法
        public static YC fromFile(Path filePath) throws IOException {
            return new YC(filePath);
        }

        public static YC of(String yamlContent) {
            return new YC(yamlContent);
        }
    }


}
