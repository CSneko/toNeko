package org.cneko.toneko.bukkit.commands.util;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class CmdContext {
    private final CommandSender sender;
    private final String[] args;
    private final Map<String,String> arguments = new HashMap<>();
    private final Player player;
    public CmdContext(CommandSender sender, String[] args){
        this.sender = sender;
        this.args = args;
        if(sender instanceof Player){
            player = (Player) sender;
        }else player = null;
    }
    public CommandSender getSender(){
        return sender;
    }
    public Player getPlayer(){
        return player;
    }
    public boolean isPlayer(){
        return player != null;
    }
    public boolean first(String arg, String perm){
        return can(1,arg,perm);
    }
    public boolean can(int index, String arg,String perm){
        if(args.length > index) {
            return args[index].equalsIgnoreCase(arg) && sender.hasPermission(perm);
        }
        return false;
    }
    public boolean aSecond(String argument){
        return argument(2,argument);
    }
    public boolean argument(int index, String argument){
        if(args.length > index){
            arguments.put(argument,args[index]);
            return true;
        }
        return false;
    }
    public String getArgument(String key){
        return arguments.get(key);
    }

}
