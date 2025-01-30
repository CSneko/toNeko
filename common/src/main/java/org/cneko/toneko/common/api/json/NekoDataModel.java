package org.cneko.toneko.common.api.json;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NekoDataModel {
    // 顶层字段
    @SerializedName("uuid")
    private UUID uuid;

    @SerializedName("is")
    private boolean active;

    @SerializedName("level")
    private double level;
    @SerializedName("blockWords")
    private List<BlockWord> blockWords;
    @SerializedName("owners")
    private List<Owner> owners;
    @SerializedName("quirks")
    private List<String> quirks;
    @SerializedName("moe_tags")
    private List<String> moeTags = new ArrayList<>();
    @SerializedName("nickname")
    private String nickname = "";

    // 嵌套 BlockWord 类
    public static class BlockWord {
        @SerializedName("block")
        private String block;
        @SerializedName("replace")
        private String replace;
        @SerializedName("method")
        private String method;

        // Getters
        public String getBlock() { return block; }
        public void setBlock(String block) { this.block = block; }
        public String getReplace() { return replace; }
        public void setReplace(String replace) { this.replace = replace; }
        public Method getMethod() { return Method.fromString(method); }
        public void setMethod(Method method) { this.method = method.getMethod(); }
        public void setMethod(String method) { this.method = method; }
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
            public String getMethod() {
                return method;
            }
        }

    }

    // 嵌套 Owner 类
    public static class Owner {
        @SerializedName("uuid")
        private UUID uuid;
        @SerializedName("xp")
        public int xp;
        @SerializedName("aliases")
        private List<String> aliases;

        // Getters
        public UUID getUuid() { return uuid; }
        public void setUuid(UUID uuid) { this.uuid = uuid; }
        public int getXp() { return xp; }
        public void setXp(int xp) { this.xp = xp; }
        public List<String> getAliases() { return aliases; }
        public void setAliases(List<String> aliases) { this.aliases = aliases; }
    }

    // Getters
    public UUID getUuid() { return uuid; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public double getLevel() { return level; }
    public void setLevel(double level) { this.level = level; }
    public List<BlockWord> getBlockWords() { return blockWords; }
    public List<Owner> getOwners() { return owners; }
    public List<String> getQuirks() { return quirks; }
    public void setQuirks(List<String> quirks) { this.quirks = quirks; }
    public List<String> getMoeTags() { return moeTags; }
    public void setMoeTags(List<String> moeTags) { this.moeTags = moeTags; }
    public String getNickName() { return nickname; }
    public void setNickName(String nickName) { this.nickname = nickName; }

}
