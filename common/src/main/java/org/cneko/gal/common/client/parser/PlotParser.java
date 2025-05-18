package org.cneko.gal.common.client.parser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class PlotParser {
    @Getter
    public static class DialogueNode {
        // Getters
        @SerializedName("character")
        private String character; // 可选

        @SerializedName("big_picture")
        private BigPicture bigPicture; // 可选

        @SerializedName("stand_picture")
        private StandPicture standPicture; // 可选

        @SerializedName("music")
        private String music; // 可选

        @SerializedName("text")
        private String text;

        @SerializedName("voice")
        private String voice; // 可选

        @SerializedName("choices")
        private List<Choice> choices; // 与next至少一个存在

        @SerializedName("next")
        private String next;

        // 将voice出现的第一个 "/" 前的解析为 CV
        public String getCV() {
            if (voice == null || voice.isEmpty()) {
                return null;
            }
            String[] split = voice.split("/");
            return split[0];
        }

        public boolean nextShouldEnd(){
            return next == null || next.isEmpty() || next.equals("end");
        }
        @Nullable
        public String getNextPlotIfShouldBeSwitch(){
            if(next.startsWith("plot:")){
                return next.substring(5);
            }
            return null;
        }

    }

    // 选择项类
    @Getter
    public static class Choice {
        // Getters
        @SerializedName("text")
        private String text;

        @SerializedName("next")
        private String next;

        @Override
        public String toString() {
            return "Choice{" +
                    "text='" + text + '\'' +
                    ", next='" + next + '\'' +
                    '}';
        }
        public boolean nextShouldEnd(){
            return next == null || next.isEmpty() || next.equals("end");
        }
        @Nullable
        public String getNextPlotIfShouldBeSwitch(){
            if(next.startsWith("plot:")){
                return next.substring(5);
            }
            return null;
        }
    }

    @Getter
    public static class StandPicture{
        @SerializedName("name")
        private String name;
        @SerializedName("height")
        private int height;
        @SerializedName("width")
        private int width;
        @SerializedName("x_offset")
        private float xOffset;
        @SerializedName("y_offset")
        private float yOffset;
    }

    @Getter
    public static class BigPicture{
        @SerializedName("name")
        private String name;
        @SerializedName("show_text")
        private boolean showText;
    }

    // 解析整个对话树
    public static Map<String, DialogueNode> parseDialogueTree(Reader jsonReader) {
        Gson gson = new GsonBuilder().create();
        Type type = new TypeToken<Map<String, DialogueNode>>(){}.getType();
        return gson.fromJson(jsonReader, type);
    }

}
