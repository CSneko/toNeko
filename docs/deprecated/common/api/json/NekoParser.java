package org.cneko.toneko.common.api.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.cneko.toneko.common.util.FileUtil;

import java.nio.file.Path;
import java.util.UUID;

public class NekoParser {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(UUID.class, new UuidAdapter()) // 自定义 UUID 解析
            .create();

    // 自定义 UUID 适配器
    private static class UuidAdapter extends com.google.gson.TypeAdapter<UUID> {
        @Override
        public void write(JsonWriter out, UUID value) throws java.io.IOException {
            out.value(value != null ? value.toString() : null);
        }

        @Override
        public UUID read(JsonReader in) throws java.io.IOException {
            return UUID.fromString(in.nextString());
        }
    }

    public static NekoDataModel parse(String json) {
        return gson.fromJson(json, NekoDataModel.class);
    }
    public static NekoDataModel fromFile(Path path) {
        return gson.fromJson(FileUtil.readStringFromFile(path.toString()), NekoDataModel.class);
    }

    public static String toJson(NekoDataModel model) {
        return gson.toJson(model);
    }
}
