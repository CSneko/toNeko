package com.crystalneko.tonekonk.commands;

import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import com.crystalneko.tonekonk.api.NekoSet;

import static org.cneko.ctlib.common.util.LocalDataBase.Connections.sqlite;

public class ToNekoCommand extends Command implements CommandExecutor {

    public ToNekoCommand() {
        super("toneko", "toneko命令", "/toneko player 玩家名", new String[]{"tn"});
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        String type = args[0];
        if(type==null){
            sender.sendMessage("参数不能为空!");
            return false;
        }
        if(type.equalsIgnoreCase("player")){
            if(args.length <2){
                sender.sendMessage("参数不足!");
                return false;
            }
            String player = args[1];
            if(!NekoSet.isNeko(player)){
                NekoSet.setNeko(player, sender.getName());
                sender.sendMessage("§a成功将§6"+player+"§a设置为一只猫娘，你成为了它的主人");
                return true;
            }else {
                sender.sendMessage("§c该玩家已经是猫娘了");
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        return execute(commandSender, label, args);
    }
}
