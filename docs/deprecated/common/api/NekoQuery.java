package org.cneko.toneko.common.api;

import lombok.Getter;
import org.cneko.ctlib.common.file.JsonConfiguration;
import org.cneko.toneko.common.api.json.NekoDataModel;
import org.cneko.toneko.common.api.json.NekoParser;
import org.cneko.toneko.common.mod.quirks.Quirk;
import org.cneko.toneko.common.mod.quirks.QuirkRegister;
import org.cneko.toneko.common.util.FileUtil;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.cneko.toneko.common.Bootstrap.*;

@Deprecated
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
    }

    /**
     * 获取猫娘的所有主人
     * @param uuid 玩家UUID
     * @return 主人列表，默认请见resources/defaultPlayerProfile.json
     */
    public static List<NekoDataModel.Owner> getOwners(UUID uuid){
        return Objects.requireNonNull(getProfile(uuid)).getOwners();
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
    }

    public static void removeOwner(UUID uuid,UUID owner){
        Neko neko =  getNeko(uuid);
        neko.removeOwner(owner);
    }

    public static void addBlock(UUID uuid,String block, String replace, String method){
        Neko neko =  getNeko(uuid);
        neko.addBlock(block,replace,method);
    }

    public static void removeBlock(UUID uuid,String block){
        Neko neko =  getNeko(uuid);
        neko.removeBlock(block);
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
    public static NekoDataModel getProfile(UUID uuid) {
        String profilePath = getProfilePath(uuid);
        if (!FileUtil.FileExists(profilePath)) {
            createProfile(uuid);
        }

        NekoDataModel model = NekoParser.fromFile(Path.of(profilePath));
        // 如果解析失败或返回null，创建默认配置
        if (model == null) {
            model = createDefaultProfile(uuid);
            // 保存默认配置到文件，防止下次读取失败
            String json = NekoParser.toJson(model);
            FileUtil.WriteFile(profilePath, json);
        }
        return model;
    }

    private static NekoDataModel createDefaultProfile(UUID uuid) {
        NekoDataModel defaultModel = new NekoDataModel();
        defaultModel.setUuid(uuid);
        defaultModel.setActive(false); // 默认非猫娘状态
        defaultModel.setOwners(new ArrayList<>());
        defaultModel.setBlockWords(new ArrayList<>());
        defaultModel.setQuirks(new ArrayList<>());
        defaultModel.setMoeTags(new ArrayList<>());
        defaultModel.setLevel(0.0);
        return defaultModel;
    }


    static Neko getNeko(UUID uuid){
        return NekoData.getNeko(uuid);
    }



    @Getter
    public static class Neko {
        public UUID uuid;
        private NekoDataModel profile;

        public Neko(UUID uuid) {
            this.uuid = uuid;
            this.profile = NekoQuery.getProfile(uuid);
            createProfile(uuid);
        }

        public Neko(File file) {
            String json = FileUtil.readStringFromFile(file.toPath().toString());
            this.profile = NekoParser.parse(json);
            this.uuid = profile.getUuid();
        }

        public String getProfilePath() {
            return NekoQuery.getProfilePath(uuid);
        }

        /**
         * 查询是否是猫娘
         *
         * @return 是否是猫娘
         */
        public boolean isNeko() {
            return profile.isActive();
        }

        /**
         * 将玩家设为猫娘
         *
         * @param isNeko 是否是猫娘
         */
        public void setNeko(boolean isNeko) {
            profile.setActive(isNeko);
        }

        public boolean hasOwner(UUID owner) {
            AtomicBoolean hasOwner = new AtomicBoolean(false);
            processOwners(owner, o -> hasOwner.set(true));
            return hasOwner.get();
        }

        /**
         * 获取猫娘的所有主人
         *
         * @return 主人列表
         */
        public List<NekoDataModel.Owner> getOwners() {
            return profile.getOwners();
        }

        @Nullable
        public NekoDataModel.Owner getOwner(UUID owner) {
            for (NekoDataModel.Owner owner1 : profile.getOwners()) {
                if (owner1.getUuid().equals(owner)) {
                    return owner1;
                }
            }
            return null;
        }

        /**
         * 添加主人
         *
         * @param owner 主人UUID
         */
        public void addOwner(UUID owner) {
            if (!hasOwner(owner)) {
                NekoDataModel.Owner newOwner = new NekoDataModel.Owner();
                newOwner.setUuid(owner);
                newOwner.setXp(0);
                newOwner.setAliases(new ArrayList<>());
                profile.getOwners().add(newOwner);
            }
        }

        public void removeOwner(UUID owner) {
            profile.getOwners().removeIf(o -> o.getUuid().equals(owner));
        }

        public void addAlias(UUID owner, String alias) {
            processOwners(owner, o -> {
                if (!o.getAliases().contains(alias)) {
                    o.getAliases().add(alias);
                }
            });
        }

        public void removeAlias(UUID owner, String alias) {
            processOwners(owner, o -> o.getAliases().remove(alias));
        }

        public void addXp(UUID owner, int xp) {
            double level = (double) xp / 1000.00d;
            addLevel(level);
            getNeko(owner).addLevel(level);
            processOwners(owner, o -> o.xp += xp);
        }

        public void removeXp(UUID owner, int xp) {
            processOwners(owner, o -> o.xp = Math.max(0, o.xp - xp));
        }

        public void setXp(UUID owner, int xp) {
            processOwners(owner, o -> o.xp = xp);
        }

        public int getXp(UUID owner) {
            AtomicInteger xp = new AtomicInteger(0);
            processOwners(owner, o -> xp.set(o.xp));
            return xp.get();
        }

        public void addLevel(double level) {
            this.setLevel(profile.getLevel() + level);
        }

        public void setLevel(double level) {
            profile.setLevel(level);
        }

        public double getLevel() {
            return profile.getLevel();
        }

        public void addBlock(String block, String replace, String method) {
            NekoDataModel.BlockWord newBlock = new NekoDataModel.BlockWord();
            newBlock.setBlock(block);
            newBlock.setReplace(replace);
            newBlock.setMethod(method);
            profile.getBlockWords().add(newBlock);
        }

        public void removeBlock(String block) {
            profile.getBlockWords().removeIf(b -> b.getBlock().equalsIgnoreCase(block));
        }

        public List<Quirk> getQuirks() {
            List<String> q = profile.getQuirks();
            return q.stream().map(QuirkRegister::getById).toList();
        }

        public boolean hasQuirk(Quirk quirk) {
            return profile.getQuirks().contains(quirk.getId());
        }

        public void addQuirk(Quirk quirk) {
            if (!profile.getQuirks().contains(quirk.getId())) {
                profile.getQuirks().add(quirk.getId());
            }
        }

        public void removeQuirk(Quirk quirk) {
            profile.getQuirks().remove(quirk.getId());
        }

        public void setQuirks(List<Quirk> quirks) {
            List<String> q = quirks.stream().map(Quirk::getId).toList();
            profile.setQuirks(q);
        }
        public void setQuirksById(List<String> quirks) {
            profile.setQuirks(quirks);
        }
        public void fixQuirks(){
            profile.getQuirks().removeIf(q -> QuirkRegister.getById(q)==null);
        }

        public List<String> getMoeTags() {
            return profile.getMoeTags();
        }

        public boolean hasAnyMoeTags() {
            return !profile.getMoeTags().isEmpty();
        }

        public void setMoeTags(List<String> moeTags) {
            profile.setMoeTags(moeTags);
        }

        public void addMoeTags(String tag) {
            if (!profile.getMoeTags().contains(tag)) {
                profile.getMoeTags().add(tag);
            }
        }

        public void removeMoeTags(String tag) {
            profile.getMoeTags().remove(tag);
        }

        public String getNickName() {
            return profile.getNickName();
        }

        public void setNickName(String nickName) {
            profile.setNickName(nickName);
        }

        public void save() {
            String json = NekoParser.toJson(profile);
            FileUtil.WriteFile(getProfilePath(), json);
        }

        @FunctionalInterface
        public interface OwnerAction {
            void apply(NekoDataModel.Owner owner);
        }

        /**
         * 遍历所有主人直到找到匹配的
         *
         * @param uuid 主人UUID
         */
        public void processOwners(UUID uuid, OwnerAction action) {
            for (NekoDataModel.Owner owner : profile.getOwners()) {
                if (owner.getUuid().equals(uuid)) {
                    action.apply(owner);
                    break;
                }
            }
        }

        public void delete() {
            NekoData.deleteNeko(uuid);
        }
    }

    /**
     * 存储了猫娘临时数据的类，大部分情况下都不许动它，知道吗
     */
    public static class NekoData {
        public static final ExecutorService executor = Executors.newFixedThreadPool(
                10000,
                r -> {
                    Thread thread = new Thread(r);
                    thread.setDaemon(true); // 守护线程
                    return thread;
                }
        );

        // 改用线程安全的Map
        private static final Map<UUID, Neko> nekoMap = new ConcurrentHashMap<>();

        public static final UUID EMPTY_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

        /**
         * 获取猫娘
         * @param uuid 猫娘uuid
         * @return 猫娘数据
         */
        public static Neko getNeko(UUID uuid) {
            // 优先从内存加载
            return nekoMap.computeIfAbsent(uuid, id -> {
                Neko neko = loadFromFile(uuid); // 从磁盘加载
                if (neko == null) {
                    neko = new Neko(uuid); // 如果不存在，创建新对象
                }
                return neko;
            });
        }

        /**
         * 从文件加载猫娘数据
         * @param uuid 猫娘uuid
         * @return 猫娘数据
         */
        private static Neko loadFromFile(UUID uuid) {
            File file = new File(DATA_PATH, uuid.toString() + ".json");
            if (file.exists()) {
                try {
                    return new Neko(file);
                } catch (Exception e) {
                    LOGGER.error("Failed to load neko data for UUID: {}", uuid, e);
                }
            }
            return null;
        }

        public static int getNekoCount() {
            return nekoMap.size();
        }

        public static int getAllNekoCount() {
            return FileUtil.getFiles(DATA_PATH).size();
        }
        public static void asyncGetAllNekoCount(AsyncIntCallback callback) {
            executor.submit(() -> {
                int count = getAllNekoCount();
                callback.apply(count);
            });
        }

        @FunctionalInterface
        public interface AsyncIntCallback {
            void apply(int count);
        }

        public static void removeNeko(UUID uuid) {
            nekoMap.remove(uuid);
        }

        public static void saveAndRemoveNeko(UUID uuid) {
            Neko neko = nekoMap.remove(uuid);
            if (neko != null) {
                executor.submit(neko::save);
            }
        }

        public static void deleteNeko(UUID uuid) {
            Neko neko = nekoMap.remove(uuid);
            if (neko != null) {
                executor.submit(() -> FileUtil.DeleteFile(neko.getProfilePath()));
            }
        }
        public static int deleteIf(Predicate<Neko> predicate) {
            // 删除计数器
            AtomicInteger deleteCount = new AtomicInteger(0);

            // 扫描文件夹
            FileUtil.getFiles(PLAYER_DATA_PATH).forEach(file -> {
                try {
                    Neko neko = new Neko(file);
                    if (predicate.test(neko)) {
                        FileUtil.DeleteFile(file.getPath());
                        deleteCount.incrementAndGet(); // 增加删除计数
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to load neko data from file: {}", file, e);
                }
            });

            // 返回删除的数量
            return deleteCount.get();
        }


        public static void saveAll() {
            nekoMap.values().forEach(Neko::save);
        }

        public static void saveAllAsync(Runnable callback) {
            executor.submit(() -> {
                saveAll();
                callback.run();
            });
        }

        public static void removeAll() {
            nekoMap.clear();
        }

        public static void loadAll() {
            nekoMap.clear();
            FileUtil.getFiles(PLAYER_DATA_PATH).forEach(file -> {
                try {
                    Neko neko = new Neko(file);
                    nekoMap.put(neko.getUuid(), neko);
                } catch (Exception e) {
                    LOGGER.error("Failed to load neko data from file: {}", file, e);
                }
            });
        }
    }

}
