package org.cneko.toneko.bukkit.api;

import com.google.common.collect.Maps;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class ClientStatus {
    public static final Map<Player,Boolean> INSTALLED = Maps.newHashMap();
    public static boolean isInstalled(Player player){
        return INSTALLED.getOrDefault(player,false);
    }
    public static boolean isInstalled(CommandSourceStack stack){
        if (stack.getExecutor() instanceof Player){
            return isInstalled((Player) stack.getExecutor());
        }
        return false;
    }
    public static void setInstalled(Player player,boolean installed){
        INSTALLED.put(player,installed);
    }

}
