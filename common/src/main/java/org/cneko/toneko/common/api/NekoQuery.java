package org.cneko.toneko.common.api;

import com.google.gson.JsonObject;
import org.cneko.ctlib.common.file.JsonConfiguration;
import org.cneko.toneko.common.quirks.Quirk;
import org.cneko.toneko.common.quirks.QuirkRegister;
import org.cneko.toneko.common.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.cneko.toneko.common.Bootstrap.*;

public class NekoQuery {
    /**
     * 查询是否是猫娘
     * @param uuid 玩家UUID
     * @return 是否是猫娘
     */
    public static boolean isNeko(UUID uuid){
        return getNeko(uuid).isNeko();
    }

    /**
     * 将玩家设为猫娘
     * @param uuid 玩家UUID
     * @param isNeko 是否是猫娘
     */
    public static void setNeko(UUID uuid, boolean isNeko){
        Neko neko = getNeko(uuid);
        neko.setNeko(isNeko);
        neko.save();
    }

    /**
     * 获取猫娘的所有主人
     * @param uuid 玩家UUID
     * @return 主人列表，默认请见resources/defaultPlayerProfile.json
     */
    public static JsonConfiguration getOwners(UUID uuid){
        return getProfile(uuid).getJsonConfiguration("owners");
    }

    /**
     * 判断是否是猫娘的主人
     * @param uuid 玩家UUID
     * @param owner 主人UUID
     * @return 是否是猫娘的主人
     */
    public static boolean hasOwner(UUID uuid, UUID owner){
        return getNeko(uuid).hasOwner(owner);
    }

    /**
     * 添加主人
     * @param uuid 玩家UUID
     * @param owner 主人UUID
     */
    public static void addOwner(UUID uuid, UUID owner){
        Neko neko =  getNeko(uuid);
        neko.addOwner(owner);
        neko.save();
    }

    public static void removeOwner(UUID uuid,UUID owner){
        Neko neko =  getNeko(uuid);
        neko.removeOwner(owner);
        neko.save();
    }

    public static void addBlock(UUID uuid,String block, String replace, String method){
        Neko neko =  getNeko(uuid);
        neko.addBlock(block,replace,method);
        neko.save();
    }

    public static void removeBlock(UUID uuid,String block){
        Neko neko =  getNeko(uuid);
        neko.removeBlock(block);
        neko.save();
    }

    public static double getLevel(UUID uuid){
        return getNeko(uuid).getLevel();
    }



    /**
     * 获取猫娘数据文件路径
     * @param uuid 玩家UUID
     * @return 猫娘数据文件路径
     */
    public static String getProfilePath(UUID uuid){
        return PLAYER_DATA_PATH + uuid.toString() + ".json";
    }

    /**
     * 创建猫娘数据文件,已经存在则忽略
     * @param uuid 玩家UUID
     */
    public static void createProfile(UUID uuid){
        String profilePath = getProfilePath(uuid);
        // 如果文件存在则忽略
        if(FileUtil.FileExists(profilePath)){
            return;
        }
        // 读取默认数据
        JsonConfiguration j = DEFAULT_PLAYER_PROFILE;
        j.set("uuid", uuid.toString());
        // 写入文件
        j.save(Path.of(profilePath));
    }

    /**
     * 获取猫娘数据文件
     * @param uuid 玩家UUID
     * @return 猫娘数据文件，不存在则返回默认的
     */
    public static JsonConfiguration getProfile(UUID uuid){
        String profilePath = getProfilePath(uuid);
        if(!FileUtil.FileExists(profilePath)){
            createProfile(uuid);
        }
        try {
            return JsonConfiguration.fromFile(Path.of(profilePath));
        } catch (IOException e) {
            LOGGER.error("Failed to read file:", e);
            return null;
        }
    }

    public static Neko getNeko(UUID uuid){
        return NekoData.getNeko(uuid);
    }


    public static class Neko {
        public UUID uuid;
        private JsonConfiguration profile;
        public Neko(UUID uuid){
            this.uuid = uuid;
            profile = NekoQuery.getProfile(uuid);
            createProfile(uuid);
        }
        public Neko(File file){
            try {
                profile = JsonConfiguration.fromFile(file.toPath());
                uuid = UUID.fromString(profile.getString("uuid"));
            } catch (IOException e) {
                LOGGER.error("Unable to load file:",e);
            }
        }

        public String getProfilePath(){
            return NekoQuery.getProfilePath(uuid);
        }

        public JsonConfiguration getProfile(){
            return profile;
        }
        /**
         * 查询是否是猫娘
         * @return 是否是猫娘
         */
        public boolean isNeko(){
            return profile.getBoolean("is", false);
        }
        /**
         * 将玩家设为猫娘
         * @param isNeko 是否是猫娘
         */
        public void setNeko(boolean isNeko){
            JsonConfiguration profile = getProfile();
            profile.set("is", isNeko);
            FileUtil.WriteFile(getProfilePath(), profile.toString());
            save();
        }

        public boolean hasOwner(UUID owner){
            AtomicBoolean hasOwner = new AtomicBoolean(false);
            processOwners(owner, o -> {
                hasOwner.set(true);
            });
            return hasOwner.get();
        }
        /**
         * 获取猫娘的所有主人
         * @return 主人列表，默认请见resources/defaultPlayerProfile.json
         */
        public List<JsonConfiguration> getOwners(){
            return getProfile().getJsonList("owners");
        }

        /**
         * 添加主人
         * @param owner 主人UUID
         */
        public void addOwner(UUID owner){
            if(!hasOwner(owner)){
                // 读取主人列表
                List<JsonConfiguration> o = getOwners();
                // 获取默认数据
                JsonConfiguration j = DEFAULT_OWNER_PROFILE;
                j.set("uuid", owner.toString());
                // 添加数据
                o.add(j);
                getProfile().set("owners", o);
            }
        }

        public void removeOwner(UUID owner){
            processOwners(owner, o -> {
                List<JsonConfiguration> owners = getOwners();
                owners.remove(o);
                getProfile().set("owners", owners);
            });
        }

        public void addAlias(UUID owner, String alias){
            //System.out.println(owner);
            processOwners(owner, o -> {
                    List<String> aliases = o.getStringList("aliases");
                    if(!aliases.contains(alias)){
                        aliases.add(alias);
                        //System.out.println(alias);
                        o.set("aliases", aliases);
                    }
                    //System.out.println("aaa "+alias);
                });
        }
        public void removeAlias(UUID owner, String alias){
            processOwners(owner, o -> {
                List<String> aliases = o.getStringList("aliases");
                if (aliases.contains(alias)) {
                    aliases.remove(alias);
                    o.set("aliases", aliases);
                }
            });
        }
        public void addXp(UUID owner, int xp){
            addLevel((double) xp /1000.00d);
            processOwners(owner, o -> {
                int oxp = o.getInt("xp");
                oxp += xp;
                o.set("xp", oxp);
            });
        }
        public void removeXp(UUID owner, int xp){
            processOwners(owner, o -> {
                int oxp = o.getInt("xp");
                oxp -= xp;
                if(oxp < 0) oxp = 0;
                o.set("xp", oxp);
            });
        }
        public void setXp(UUID owner, int xp){
            processOwners(owner, o -> {
                o.set("xp", xp);
            });
        }
        public int getXp(UUID owner){
            AtomicInteger xp = new AtomicInteger(0);
            processOwners(owner, o -> xp.set(o.getInt("xp")));
            return xp.get();
        }

        public void addLevel(double level){
            double l = getLevel() + level;
            setLevel(l);
        }
        public void setLevel(double level){
            getProfile().set("level", level);
        }
        public double getLevel(){
            return getProfile().getDouble("level");
        }

        public void addBlock(String block, String replace, String method){
            List<JsonConfiguration> blockWords = getProfile().getJsonList("blockWords");
            JsonConfiguration BW = DEFAULT_BLOCK_WORDS;
            BW.set("replace", replace);
            BW.set("method", method);
            BW.set("block", block);
            blockWords.add(BW);
            getProfile().set("blockWords", blockWords);
        }

        public void removeBlock(String block){
            List<JsonConfiguration> blockWords = getProfile().getJsonList("blockWords");
            blockWords.removeIf(o -> o.getString("block").equalsIgnoreCase(block));
            getProfile().set("blockWords", blockWords);
        }

        public List<Quirk> getQuirks(){
            List<Quirk> quirks = new ArrayList<>();
            JsonObject gson = getProfile().toGson();
            gson.getAsJsonArray("quirks").forEach(o -> {
                quirks.add(QuirkRegister.getById(o.getAsJsonPrimitive().getAsString()));
            });
            return quirks;
        }
        public boolean hasQuirk(Quirk quirk){
            return getQuirks().contains(quirk);
        }
        public void addQuirk(Quirk quirk){
            if(!getQuirks().contains(quirk)){
                List<String> quirks = getProfile().getStringList("quirks");
                quirks.add(quirk.getId());
                getProfile().set("quirks", quirks);
            }
        }
        public void removeQuirk(Quirk quirk){
            getProfile().getStringList("quirks").removeIf(s -> s.equalsIgnoreCase(quirk.getId()));
        }
        public void setQuirks(List<Quirk> quirks){
            getProfile().set("quirks", quirks.stream().map(Quirk::getId).collect(Collectors.toList()));
        }
        public void setQuirksById(List<String> quirks){
            getProfile().set("quirks", quirks);
        }
        public void fixQuirks(){
            List<String> ids = getProfile().getStringList("quirks");
            ids.removeIf(s -> !QuirkRegister.hasQuirk(s));
            getProfile().set("quirks", ids);
        }
        public List<String> getMoeTags(){
            return getProfile().getStringList("moe_tags");
        }
        public boolean hasAnyMoeTags(){
            return !getMoeTags().isEmpty();
        }
        public void setMoeTags(List<String> moeTags){
            getProfile().set("moe_tags", moeTags);
        }
        public void addMoeTags(String tag){
            if (!getMoeTags().contains(tag)){
                List<String> moeTags = getMoeTags();
                moeTags.add(tag);
                setMoeTags(moeTags);
            }
        }
        public void removeMoeTags(String tag){
            getProfile().getStringList("moe_tags").removeIf(s -> s.equalsIgnoreCase(tag));
        }

        public boolean hasNickName(){
            return getProfile().getString("nickname") != null;
        }
        public String getNickName(){
            return getProfile().getString("nickname");
        }
        public void setNickName(String nickName){
            getProfile().set("nickname", nickName);
        }
        public NekoSkin getSkin(){
            return NekoSkin.of(getProfile().getString("skin"));
        }
        public void setSkin(NekoSkin skin){
            getProfile().set("skin", skin.getSkin());
        }
        public boolean hasSkin(){
            return !getProfile().getString("skin").equalsIgnoreCase("");
        }

        public void save(){
            try {
                getProfile().save();
            }catch (Exception e){
                LOGGER.error("Failed to save profile", e);
            }
        }

        @FunctionalInterface
        public interface OwnerAction {
            void apply(JsonConfiguration ownerConfig);
        }

        /**
         * 遍历所有主人直到找到匹配的
         * @param uuid 主人UUID
         */
        public void processOwners(UUID uuid, OwnerAction action) {
            /* 都是你害的,为什么这么难搞啊
                我要崩溃了 (☍﹏⁰。)
             */
            List<JsonConfiguration> owners = getOwners(); // 获取当前的主人列表
            boolean updated = false; // 标记是否进行了修改
            for (JsonConfiguration o : owners) {
                if (o.getString("uuid").equalsIgnoreCase(uuid.toString())) {
                    action.apply(o); // 执行Lambda定义的操作
                    updated = true; // 标记已修改
                    break;
                }
            }
            if (updated) { // 如果有修改，则更新profile中的owners
                getProfile().set("owners", owners);
            }
        }
        public UUID getUuid(){
            return uuid;
        }
    }

    /**
     * 存储了猫娘临时数据的类，大部分情况下都不许动它，知道吗
     */
    public static class NekoData {
        public static List<Neko> nekoList = new ArrayList<>();

        /**
         * 获取猫娘
         * @param uuid 猫娘uuid
         * @return 猫娘数据
         */
        public static Neko getNeko(UUID uuid){
            // 如果猫娘列表中有这个猫娘，就返回它
            for (Neko neko : nekoList){
                if(neko.getUuid().equals(uuid)){
                    return neko;
                }
            }
            // 如果没有，就创建一个
            Neko neko = new Neko(uuid);
            nekoList.add(neko);
            return neko;
        }

        /**
         * 移除猫娘
         * @param uuid 猫娘uuid
         */
        public static void removeNeko(UUID uuid){
            nekoList.remove(getNeko(uuid));
        }

        /**
         * 保存所有猫娘数据
         */
        public static void saveAll(){
            nekoList.forEach(Neko::save);
        }

        /**
         * 清除所有猫娘数据并重新加载
         */
        public static void loadAll(){
            nekoList.clear();
            FileUtil.getFiles(PLAYER_DATA_PATH).forEach(file -> {
                try {
                    Neko neko = new Neko(file);
                    nekoList.add(neko);
                }catch (Exception e){
                    LOGGER.error("Failed to load neko data", e);
                }
            });
        }

    }
}
