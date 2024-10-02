package org.cneko.toneko.bukkit.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.cneko.toneko.bukkit.commands.util.CmdContext;
import org.cneko.toneko.bukkit.util.Language;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.api.Permissions;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.bukkit.util.Language.get;

public class ToNekoAdminCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        CmdContext ctx = new CmdContext(sender,args);
        if (ctx.first("help", Permissions.COMMAND_TONEKOADMIN_HELP)) {
            return helpCommand(ctx);
        } else if (ctx.first("reload", Permissions.COMMAND_TONEKOADMIN_RELOAD)) {
            return reload(ctx);
        } else if (ctx.first("set", Permissions.COMMAND_TONEKOADMIN_SET)) {
            if (ctx.aSecond("neko")) {
                return setCommand(ctx);
            }
        }
        return true;
    }

    public boolean setCommand(CmdContext ctx) {
        CommandSender sender = ctx.getSender();
        String nekoName = ctx.getArgument("neko");
        // 获取玩家(如果可以的话)
        Player nekoPlayer = Bukkit.getPlayer(nekoName);
        if (nekoPlayer == null){
            sender.sendMessage(get("command.toneko.not_found"));
            return true;
        }
        NekoQuery.Neko neko = NekoQuery.getNeko(nekoPlayer.getUniqueId());
        boolean isNeko = neko.isNeko();
        if(isNeko){
            // 如果是猫猫，则设置为非猫猫
            neko.setNeko(false);
            sender.sendMessage(get("command.tonekoadmin.set.false", nekoName));
        }else {
            neko.setNeko(true);
            sender.sendMessage(get("command.tonekoadmin.set.true", nekoName));
        }
        return true;
    }

    public boolean reload(CmdContext ctx) {
        ConfigUtil.load();
        LanguageUtil.load();
        ctx.getSender().sendMessage(get("command.tonekoadmin.reload"));
        return true;
    }

    public boolean helpCommand(CmdContext ctx) {
        ctx.getSender().sendMessage(get("command.tonekoadmin.help"));
        return true;
    }
}
