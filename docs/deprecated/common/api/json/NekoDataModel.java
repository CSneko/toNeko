package org.cneko.toneko.common.api.json;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Deprecated
public class NekoDataModel {
    // Getters
    // 顶层字段
    @Getter
    @Setter
    @SerializedName("uuid")
    private UUID uuid;

    @Getter
    @Setter
    @SerializedName("is")
    private boolean active;

    @Getter
    @Setter
    @SerializedName("level")
    private double level;
    @Getter
    @Setter
    @SerializedName("blockWords")
    private List<BlockWord> blockWords;
    @Setter
    @Getter
    @SerializedName("owners")
    private List<Owner> owners;
    @Setter
    @Getter
    @SerializedName("quirks")
    private List<String> quirks;
    @Setter
    @Getter
    @SerializedName("moe_tags")
    private List<String> moeTags = new ArrayList<>();
    @SerializedName("nickname")
    private String nickname = "";

    // 嵌套 BlockWord 类
    public static class BlockWord {
        // Getters
        @Getter
        @Setter
        @SerializedName("block")
        private String block;
        @Getter
        @Setter
        @SerializedName("replace")
        private String replace;
        @SerializedName("method")
        private String method;

        public Method getMethod() { return Method.fromString(method); }
        public void setMethod(Method method) { this.method = method.getMethod(); }
        public void setMethod(String method) { this.method = method; }
        @Getter
        public enum Method {
            WORD("word"),
            ALL("all");
            private final String method;
            Method(String method) {
                this.method = method;
            }
            public static Method fromString(String method) {
                for (Method m : Method.values()) {
                    if (m.method.equalsIgnoreCase(method)) {
                        return m;
                    }
                }
                throw new IllegalArgumentException("Unknown method: " + method);
            }
        }

    }

    // 嵌套 Owner 类
    @Getter
    @Setter
    public static class Owner {
        // Getters
        @SerializedName("uuid")
        private UUID uuid;
        @SerializedName("xp")
        public int xp;
        @SerializedName("aliases")
        private List<String> aliases;

    }

    public String getNickName() { return nickname; }
    public void setNickName(String nickName) { this.nickname = nickName; }

}
