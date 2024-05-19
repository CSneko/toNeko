package org.cneko.toneko.common.api;

import org.cneko.ctlib.common.file.JsonConfiguration;
import org.cneko.toneko.common.util.FileUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import static org.cneko.toneko.common.Bootstrap.*;

public class NekoQuery {
    /**
     * 查询是否是猫娘
     * @param uuid 玩家UUID
     * @return 是否是猫娘
     */
    public static boolean isNeko(UUID uuid){
        return getProfile(uuid).getBoolean("is", false);
    }

    /**
     * 将玩家设为猫娘
     * @param uuid 玩家UUID
     * @param isNeko 是否是猫娘
     */
    public static void setNeko(UUID uuid, boolean isNeko){
        createProfile(uuid);
        JsonConfiguration profile = getProfile(uuid);
        profile.set("is", isNeko);
        FileUtil.WriteFile(getProfilePath(uuid), profile.toString());
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
        for (JsonConfiguration o : getOwners(uuid).toJsonList()){
            if(o.getString("uuid").equalsIgnoreCase(owner.toString())){
                return true;
            }
        }
        return false;
    }

    /**
     * 添加主人
     * @param uuid 玩家UUID
     * @param owner 主人UUID
     */
    public static void addOwner(UUID uuid, UUID owner){
        createProfile(uuid);
        if(!hasOwner(uuid, owner)){
            // 读取主任列表
            JsonConfiguration owners = getOwners(uuid);
            List<JsonConfiguration> o = owners.toJsonList();
            // 获取默认数据
            JsonConfiguration j = DEFAULT_OWNER_PROFILE;
            j.set("uuid", owner.toString());
            // 添加数据
            o.add(j);
            JsonConfiguration p = getProfile(uuid);
            p.set("owners", o);
            // 写入文件
            try {
                p.save();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to save profile", e);
            }
        }
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
        // 如果数据文件不存在，则返回默认
        if(FileUtil.FileExists(getProfilePath(uuid))){
            try {
                return JsonConfiguration.fromFile(Path.of(getProfilePath(uuid)));
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE,e.getMessage());
            }
        }
        return DEFAULT_PLAYER_PROFILE;
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

        public JsonConfiguration getProfile(){
            return profile;
        }

        public boolean isNeko(){
            return profile.getBoolean("is", false);
        }

        public boolean hasOwner(UUID owner){
            for (JsonConfiguration o : getOwners().toJsonList()){
                if(o.getString("uuid").equalsIgnoreCase(owner.toString())){
                    return true;
                }
            }
            return false;
        }

        public JsonConfiguration getOwners(){
            return getProfile().getJsonConfiguration("owners");
        }

        public void addOwner(UUID owner){
            createProfile(uuid);
            if(!hasOwner(owner)){
                // 读取主任列表
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

        public void save(){
            try {
                getProfile().save();
            }catch (IOException e){
                LOGGER.log(Level.SEVERE, "Failed to save profile", e);
            }
        }



    }
}
