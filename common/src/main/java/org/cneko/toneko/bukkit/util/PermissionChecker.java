package org.cneko.toneko.bukkit.util;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.cneko.toneko.common.api.NekoQuery;

@SuppressWarnings("UnstableApiUsage")
public class PermissionChecker {
    public static boolean check(CommandSourceStack source, String permission){
        if (source.getSender() instanceof Player){
            return source.getSender().hasPermission(permission);
        }
        return false;
    }

    public static boolean checkAndNeko(CommandSourceStack source, String permission){
        if (source.getSender() instanceof Player player){
            return check(source, permission) || NekoQuery.isNeko(player.getUniqueId());
        }
        return false;
    }
}
