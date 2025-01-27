package org.cneko.toneko.common.mod.util;

import net.minecraft.server.MinecraftServer;
import org.cneko.toneko.common.mod.ModMeta;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

public class PlayerUtil {
    public static Player getPlayerByName(String name) {
        return ModMeta.INSTANCE.getServer().getPlayerList().getPlayerByName(name);
    }
    public static UUID getPlayerUUIDByName(String name) {
        return getPlayerByName(name).getUUID();
    }
    public static Player getPlayerByUUID(UUID uuid) {
        return ModMeta.INSTANCE.getServer().getPlayerList().getPlayer(uuid);
    }
    public static List<ServerPlayer> getPlayerList() {
        return ModMeta.INSTANCE.getServer().getPlayerList().getPlayers();
    }

    /**
     * 获取玩家的 UUID 集合
     *
     * @param server 可为空的 Minecraft 服务器实例
     * @return 玩家 UUID 集合
     */
    public static Set<UUID> getPlayerUUIDs(@Nullable MinecraftServer server) {
        Set<UUID> uuids = new HashSet<>();

        // 获取 playerdata 文件夹路径
        File playerDataDir = new File("world/playerdata");

        if (playerDataDir.exists() && playerDataDir.isDirectory()) {
            // 遍历文件夹中的 .dat 和 .dat_old 文件
            File[] files = playerDataDir.listFiles((dir, name) -> name.endsWith(".dat") || name.endsWith(".dat_old"));
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName().split("\\.")[0]; // 去掉扩展名
                    try {
                        UUID uuid = UUID.fromString(fileName);
                        uuids.add(uuid);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }
        return uuids;
    }

}
