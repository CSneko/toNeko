package org.cneko.toneko.common.util;

import com.google.gson.*;
import lombok.Getter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

import static org.cneko.toneko.common.Bootstrap.LOGGER;
public class JsonConfiguration {
    @Getter
    private final String original;
    private final JsonObject jsonObject;
    private Path filePath;

    private final Object lock = new Object();

    public JsonConfiguration(String jsonString) {
        this.original = jsonString;
        Gson gson = new Gson();
        // 尝试解析，如果失败则创建空对象，防止崩坏文件导致启动失败
        JsonObject temp;
        try {
            temp = gson.fromJson(jsonString, JsonObject.class);
        } catch (JsonSyntaxException e) {
            temp = new JsonObject();
        }
        this.jsonObject = temp != null ? temp : new JsonObject();
    }

    public JsonConfiguration(JsonObject jsonObject) {
        this.original = jsonObject.toString();
        this.jsonObject = jsonObject;
    }

    public JsonConfiguration(Path filePath) throws IOException {
        String content = "{}";
        try {
            if (Files.exists(filePath)) {
                // 关键修复：强制使用 UTF-8 读取
                byte[] bytes = Files.readAllBytes(filePath);
                content = new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            LOGGER.error(e);
        }

        if (content.trim().isEmpty()) content = "{}";
        this.original = content;

        Gson gson = new Gson();
        JsonObject temp;
        try {
            temp = gson.fromJson(content, JsonObject.class);
        } catch (Exception e) {
            temp = new JsonObject();
        }
        this.jsonObject = temp != null ? temp : new JsonObject();
        this.filePath = filePath;
    }

    // --- 所有访问 jsonObject 的方法加锁 ---

    public JsonElement get(String path) {
        synchronized (lock) {
            return jsonObject.get(path);
        }
    }

    public JsonPrimitive getJsonPrimitive(String path) {
        synchronized (lock) {
            JsonElement element = get(path);
            if (element != null && element.isJsonPrimitive()) {
                return element.getAsJsonPrimitive();
            }
            return new JsonPrimitive("");
        }
    }

    public JsonArray getJsonArray(String path) {
        synchronized (lock) {
            JsonElement element = get(path);
            if (element != null && element.isJsonArray()) {
                return element.getAsJsonArray();
            }
            return new JsonArray();
        }
    }

    public void set(String path, Object value) {
        synchronized (lock) {
            if (value instanceof JsonConfiguration) {
                jsonObject.add(path, ((JsonConfiguration) value).jsonObject);
                return;
            }
            if (value instanceof JsonElement) {
                jsonObject.add(path, (JsonElement) value);
                return;
            }
            if (value instanceof String) {
                jsonObject.addProperty(path, (String) value);
                return;
            }
            if (value instanceof Number) {
                jsonObject.addProperty(path, (Number) value);
                return;
            }
            if (value instanceof Boolean) {
                jsonObject.addProperty(path, (Boolean) value);
                return;
            }
            if (value instanceof Character) {
                jsonObject.addProperty(path, (Character) value);
                return;
            }
            if (value instanceof List) {
                processList(path, (List<?>) value);
                return;
            }
            if (value != null) {
                this.jsonObject.addProperty(path, value.toString());
            }
        }
    }

    private void processList(String path, List<?> list) {
        JsonArray jsonArray = new JsonArray();
        if (list.isEmpty()) {
            jsonObject.add(path, jsonArray);
            return;
        }
        for (Object o : list) {
            if (o instanceof JsonConfiguration) {
                jsonArray.add(((JsonConfiguration) o).jsonObject);
            } else if (o instanceof JsonElement) {
                jsonArray.add((JsonElement) o);
            } else if (o instanceof String) {
                jsonArray.add((String) o);
            } else if (o instanceof Number) {
                jsonArray.add((Number) o);
            } else if (o instanceof Boolean) {
                jsonArray.add((Boolean) o);
            } else if (o instanceof Character) {
                jsonArray.add((Character) o);
            } else if (o != null) {
                jsonArray.add(o.toString());
            }
        }
        jsonObject.add(path, jsonArray);
    }

    public void save(Path filePath) {
        synchronized (lock) {
            if (filePath != null) {
                try {
                    // 1. 确保父目录存在
                    if (filePath.getParent() != null) {
                        Files.createDirectories(filePath.getParent());
                    }

                    // 2. 建议开启 PrettyPrinting (格式化输出)，否则配置文件挤在一行很难看
                    Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                    String content = gson.toJson(this.jsonObject);

                    // 3. 关键修复：强制使用 UTF-8 写入
                    try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        writer.write(content);
                    }
                } catch (IOException e) {
                    LOGGER.error(e);
                }
            }
        }
    }

    public void save() {
        save(this.filePath);
    }


    public String getString(String path) {
        try {
            return getJsonPrimitive(path).getAsString();
        } catch (Exception e) {
            return "";
        }
    }

    public List<String> getStringList(String path) {
        synchronized (lock) {
            JsonArray array = getJsonArray(path);
            List<String> list = new ArrayList<>();
            for (JsonElement e : array) {
                if (e.isJsonPrimitive()) {
                    list.add(e.getAsJsonPrimitive().getAsString());
                }
            }
            return list;
        }
    }

    public float getFloat(String path) { try { return getJsonPrimitive(path).getAsFloat(); }catch (Exception e){ return 0; } }
    public double getDouble(String path) { try { return getJsonPrimitive(path).getAsDouble(); }catch (Exception e){ return 0; } }
    public int getInt(String path) { try { return getJsonPrimitive(path).getAsInt(); }catch (Exception e){ return 0; } }
    public boolean getBoolean(String path) { try { return getJsonPrimitive(path).getAsBoolean(); }catch (Exception e){ return false; } }
    public boolean getBoolean(String path, boolean defValue) { try { return getJsonPrimitive(path).getAsBoolean(); }catch (Exception e){ return defValue; } }

    public JsonConfiguration getJsonConfiguration(String path) {
        synchronized (lock) {
            try {
                JsonElement element = get(path);
                if (element != null && element.isJsonObject()) {
                    return new JsonConfiguration(element.getAsJsonObject());
                }
                return new JsonConfiguration("{}");
            } catch (Exception e) {
                return new JsonConfiguration("{}");
            }
        }
    }

    public boolean contains(String key) {
        synchronized (lock) {
            return jsonObject.has(key);
        }
    }

    public List<Integer> getIntList(String path) {
        synchronized (lock) {
            JsonArray array = getJsonArray(path);
            List<Integer> list = new ArrayList<>();
            for (JsonElement e : array) { if (e.isJsonPrimitive()) list.add(e.getAsJsonPrimitive().getAsInt()); }
            return list;
        }
    }
    public List<Double> getDoubleList(String path) {
        synchronized (lock) {
            JsonArray array = getJsonArray(path);
            List<Double> list = new ArrayList<>();
            for (JsonElement e : array) { if (e.isJsonPrimitive()) list.add(e.getAsJsonPrimitive().getAsDouble()); }
            return list;
        }
    }
    public List<Float> getFloatList(String path) {
        synchronized (lock) {
            JsonArray array = getJsonArray(path);
            List<Float> list = new ArrayList<>();
            for (JsonElement e : array) { if (e.isJsonPrimitive()) list.add(e.getAsJsonPrimitive().getAsFloat()); }
            return list;
        }
    }

    public List<JsonConfiguration> getJsonList(String path) {
        synchronized (lock) {
            JsonArray array = getJsonArray(path);
            return array.asList().stream().map(e -> new JsonConfiguration(e.getAsJsonObject())).collect(Collectors.toList());
        }
    }

    public List<Object> getList(String path) {
        synchronized (lock) {
            JsonArray array = getJsonArray(path);
            List<Object> list = new ArrayList<>();
            for (JsonElement e : array) {
                if (e.isJsonObject()) {
                    list.add(e.getAsJsonObject());
                } else if (e.isJsonPrimitive()) {
                    // 如果是基础类型，根据类型返回，而不是试图转为 Object
                    JsonPrimitive p = e.getAsJsonPrimitive();
                    if (p.isString()) list.add(p.getAsString());
                    else if (p.isBoolean()) list.add(p.getAsBoolean());
                    else if (p.isNumber()) list.add(p.getAsNumber());
                }
            }
            return list;
        }
    }

    public boolean equals(Object obj) {
        synchronized (lock) {
            if (obj instanceof JsonConfiguration other) {
                return this.jsonObject.equals(other.jsonObject);
            }
            return obj.toString().equals(this.jsonObject.toString());
        }
    }

    public List<JsonConfiguration> toJsonList() {
        synchronized (lock) {
            if (jsonObject.isJsonArray()) {
                return jsonObject.getAsJsonArray().asList().stream().map(e -> new JsonConfiguration(e.getAsJsonObject())).collect(Collectors.toList());
            }
            return Collections.emptyList();
        }
    }

    public JsonObject toGson() {
        return jsonObject; // 注意：直接返回内部对象仍然有外部并发修改的风险，最好返回 deepCopy
    }

    @Override
    public String toString() {
        synchronized (lock) {
            return jsonObject.toString();
        }
    }

    // Static methods...
    public static JsonConfiguration of(String jsonString) { return new JsonConfiguration(jsonString); }
    public static JsonConfiguration fromFile(File file) throws IOException { return new JsonConfiguration(file.toPath()); }
    public static JsonConfiguration fromFile(Path filePath) throws IOException { return new JsonConfiguration(filePath); }
}