package org.cneko.toneko.common.api;

import org.cneko.ctlib.common.file.JsonConfiguration;
import org.cneko.toneko.common.Bootstrap;
import org.cneko.toneko.common.util.FileUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import static org.cneko.toneko.common.Bootstrap.*;

public class NekoQuery {
    /**
     * 查询是否是猫娘
     * @param uuid 玩家UUID
     * @return 是否是猫娘
     */
    public static boolean isNeko(UUID uuid){
        return new Neko(uuid).isNeko();
    }

    /**
     * 将玩家设为猫娘
     * @param uuid 玩家UUID
     * @param isNeko 是否是猫娘
     */
    public static void setNeko(UUID uuid, boolean isNeko){
        Neko neko = new Neko(uuid);
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
        return new Neko(uuid).hasOwner(owner);
    }

    /**
     * 添加主人
     * @param uuid 玩家UUID
     * @param owner 主人UUID
     */
    public static void addOwner(UUID uuid, UUID owner){
        Neko neko =  new Neko(uuid);
        neko.addOwner(owner);
        neko.save();
    }

    public static void removeOwner(UUID uuid,UUID owner){
        Neko neko =  new Neko(uuid);
        neko.addOwner(owner);
        neko.save();
    }

    public static void addBlock(UUID uuid,String block, String replace, String method){
        Neko neko =  new Neko(uuid);
        neko.addBlock(block,replace,method);
        neko.save();
    }

    public void removeBlock(UUID uuid,String block){
        Neko neko =  new Neko(uuid);
        neko.removeBlock(block);
        neko.save();
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
        FileUtil.WriteFile(profilePath, j.toString());
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
        return new Neko(uuid);
    }


    public static class Neko {
        public UUID uuid;
        private JsonConfiguration profile;
        public Neko(UUID uuid){
            this.uuid = uuid;
            profile = NekoQuery.getProfile(uuid);
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
            createProfile(uuid);
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
        public JsonConfiguration getOwners(){
            return getProfile().getJsonConfiguration("owners");
        }

        /**
         * 添加主人
         * @param owner 主人UUID
         */
        public void addOwner(UUID owner){
            createProfile(uuid);
            if(!hasOwner(owner)){
                // 读取主人列表
                JsonConfiguration owners = getOwners();
                List<JsonConfiguration> o = owners.toJsonList();
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
                List<JsonConfiguration> owners = getOwners().toJsonList();
                owners.remove(o);
                getProfile().set("owners", owners);
            });
        }

        public void addAlias(UUID owner, String alias){
            processOwners(owner, o -> {
                    List<String> aliases = o.getStringList("aliases");
                    if(!aliases.contains(alias)){
                        aliases.add(alias);
                        o.set("aliases", aliases);
                    }
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
        public int getXp(UUID owner){
            AtomicInteger xp = new AtomicInteger(0);
            processOwners(owner, o -> xp.set(o.getInt("xp")));
            return xp.get();
        }

        public void addBlock(String block, String replace, String method){
            createProfile(uuid);
            List<JsonConfiguration> blockWords = getProfile().getJsonList("blockWords");
            JsonConfiguration BW = DEFAULT_BLOCK_WORDS;
            BW.set("replace", replace);
            BW.set("method", method);
            BW.set("block", block);
            blockWords.add(BW);
            getProfile().set("blockWords", blockWords);
        }

        public void removeBlock(String block){
            createProfile(uuid);
            List<JsonConfiguration> blockWords = getProfile().getJsonList("blockWords");
            blockWords.removeIf(o -> o.getString("block").equalsIgnoreCase(block));
            getProfile().set("blockWords", blockWords);
        }

        public void save(){
            try {
                getProfile().save();
            }catch (IOException e){
                LOGGER.error("Failed to save profile", e);
            }
        }

        @FunctionalInterface
        private interface OwnerAction {
            void apply(JsonConfiguration ownerConfig);
        }
        private void processOwners(UUID uuid, OwnerAction action) {
            createProfile(uuid);
            List<JsonConfiguration> owners = getOwners().toJsonList();
            for (JsonConfiguration o : owners) {
                if (o.getString("uuid").equalsIgnoreCase(uuid.toString())) {
                    action.apply(o); // 执行Lambda定义的操作
                }
            }
            getProfile().set("owners", owners);
        }


    }
}
