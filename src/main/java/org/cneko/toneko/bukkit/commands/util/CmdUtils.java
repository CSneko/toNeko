package org.cneko.toneko.bukkit.commands.util;

import org.cneko.toneko.bukkit.util.Language;

import static org.cneko.toneko.bukkit.util.Language.get;

public class CmdUtils {
    public static boolean notPlayer(CmdContext ctx){
        ctx.getSender().sendMessage(Language.get("command.toneko.not_player"));
        return true;
    }
    public static boolean playerNotFound(CmdContext ctx){
        ctx.getSender().sendMessage(get("command.toneko.not_found"));
        return true;
    }
    public static boolean playerNotNeko(CmdContext ctx){
        ctx.getSender().sendMessage(get("command.toneko.player.notNeko",ctx.getArgument("neko")));
        return true;
    }
    public static boolean playerAlreadyNekoOwner(CmdContext ctx){
        ctx.getSender().sendMessage(get("command.toneko.player.alreadyOwner",ctx.getArgument("neko")));
        return true;
    }
}
