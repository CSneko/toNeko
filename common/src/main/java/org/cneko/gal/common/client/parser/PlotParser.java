package org.cneko.gal.common.client.parser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class PlotParser {
    public static class DialogueNode {
        @SerializedName("character")
        private String character;

        @SerializedName("stand_picture")
        private String standPicture;

        @SerializedName("music")
        private String music;

        @SerializedName("text")
        private String text;

        @SerializedName("time")
        private int time;

        @SerializedName("voice")
        private String voice;

        @SerializedName("choices")
        private List<Choice> choices;

        @SerializedName("next")
        private String next;

        // Getters
        public String getCharacter() {
            return character;
        }

        public String getStandPicture() {
            return standPicture;
        }

        public String getMusic() {
            return music;
        }

        public String getText() {
            return text;
        }

        public int getTime() {
            return time;
        }

        public String getVoice() {
            return voice;
        }

        // 将voice出现的第一个 "/" 前的解析为 CV
        public String getCV() {
            if (voice == null || voice.isEmpty()) {
                return null;
            }
            String[] split = voice.split("/");
            return split[0];
        }

        public List<Choice> getChoices() {
            return choices;
        }

        public String getNext() {
            return next;
        }
    }

    // 选择项类
    public static class Choice {
        @SerializedName("text")
        private String text;

        @SerializedName("next")
        private String next;

        // Getters
        public String getText() {
            return text;
        }

        public String getNext() {
            return next;
        }

        @Override
        public String toString() {
            return "Choice{" +
                    "text='" + text + '\'' +
                    ", next='" + next + '\'' +
                    '}';
        }
    }

    // 解析整个对话树
    public static Map<String, DialogueNode> parseDialogueTree(Reader jsonReader) {
        Gson gson = new GsonBuilder().create();
        Type type = new TypeToken<Map<String, DialogueNode>>(){}.getType();
        return gson.fromJson(jsonReader, type);
    }

}
