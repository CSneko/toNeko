package org.cneko.toneko.bukkit.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PlayerUtil {
    public static Player getPlayerByName(String playerName) {
        return Bukkit.getPlayer(playerName);
    }

    public static UUID getPlayerUUIDByName(String name) {
        return Bukkit.getPlayerUniqueId(name);
    }

}
