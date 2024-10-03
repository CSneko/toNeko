package org.cneko.toneko.bukkit.util;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

@SuppressWarnings("UnstableApiUsage")
public class PermissionChecker {
    public static boolean check(CommandSourceStack source, String permission){
        if (source.getSender() instanceof Player){
            return source.getSender().hasPermission(permission);
        }
        return false;
    }
}
