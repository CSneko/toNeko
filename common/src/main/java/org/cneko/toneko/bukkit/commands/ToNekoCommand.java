package org.cneko.toneko.bukkit.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.cneko.toneko.bukkit.commands.util.CmdContext;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.api.Permissions;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static org.cneko.toneko.bukkit.commands.util.CmdUtils.*;
import static org.cneko.toneko.bukkit.util.Language.get;

public class ToNekoCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        CmdContext ctx = new CmdContext(sender,args);
        // 不允许命令执行者不是玩家
        if (!ctx.isPlayer()) return notPlayer(ctx);
        // ------------------------------------ help ----------------------------------
        if (ctx.first("help", Permissions.COMMAND_TONEKO_HELP)){
            return helpCommand(ctx);
        }
        // ----------------------------------- player ----------------------------------
        if (ctx.first("player", Permissions.COMMAND_TONEKO_PLAYER)){
            if (ctx.aSecond("neko")){
                return playerCommand(ctx);
            }
        }
        // ----------------------------------- remove -----------------------------------
        if (ctx.first("remove", Permissions.COMMAND_TONEKO_REMOVE)){
            if (ctx.aSecond("neko")){
                return removeCommand(ctx);
            }
        }
        return true;
    }

    public boolean removeCommand(CmdContext ctx) {
        UUID player = ctx.getPlayer().getUniqueId();
        String nekoName = ctx.getArgument("neko");
        Player nekoPlayer = Bukkit.getPlayer(nekoName);
        if (nekoPlayer == null) return playerNotFound(ctx);
        NekoQuery.Neko neko = NekoQuery.getNeko(nekoPlayer.getUniqueId());
        if (!neko.hasOwner(player)) return playerNotNekoOwner(ctx);
        neko.removeOwner(player);
        neko.save();
        ctx.getSender().sendMessage(get("command.toneko.remove.success",nekoName));
        return true;
    }

    public static boolean playerCommand(CmdContext ctx) {
        UUID player = ctx.getPlayer().getUniqueId();
        String nekoName = ctx.getArgument("neko");
        Player nekoPlayer = Bukkit.getPlayer(nekoName);
        if (nekoPlayer == null) return playerNotFound(ctx);
        NekoQuery.Neko neko = NekoQuery.getNeko(nekoPlayer.getUniqueId());
        if (!neko.isNeko()) return playerNotNeko(ctx);
        if (neko.hasOwner(player)) return playerAlreadyNekoOwner(ctx);
        neko.addOwner(player);
        neko.save();
        ctx.getSender().sendMessage(get("command.toneko.player.success",nekoName));
        return true;
    }


    public static boolean helpCommand(CmdContext ctx){
        ctx.getSender().sendMessage(get("command.toneko.help"));
        return true;
    }
}
