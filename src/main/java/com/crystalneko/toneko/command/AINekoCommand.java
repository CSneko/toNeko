package com.crystalneko.toneko.command;

import com.crystalneko.toneko.utils.ConfigFileUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.crystalneko.toneko.ToNeko.getMessage;
import static com.crystalneko.toneko.command.ToNekoCommand.killPlayer;

public class AINekoCommand implements CommandExecutor {
    private final Map<Player, Boolean> confirmMap = new ConcurrentHashMap<>(); //MT-Map for folia support

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("command.only-player"));
            return true;
        }

        Player player = (Player) sender;
        if(args.length == 0){
            player.sendMessage(getMessage("command.toneko.help"));
        }else if(args.length >= 2 && args[0].equalsIgnoreCase("add")){
            if (player.hasPermission("aineko.command.add")) {
                //创建数据文件实例
                File dataFile = new File("plugins/toNeko/nekos.yml");
                // 加载数据文件
                YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
                //创建数据值
                Boolean created = ConfigFileUtils.createNewKey(args[1] + ".owner", player.getName(), data, dataFile);
                ConfigFileUtils.createNewKey(args[1] + ".xp", 0, data, dataFile);
                List<String> aliases = new ArrayList<>();
                aliases.add("被你发现了");
                ConfigFileUtils.createNewKey(args[1] + ".aliases", aliases, data, dataFile);
                ConfigFileUtils.createNewKey(args[1] + ".type","AI",data,dataFile);
                if (created) {
                    killPlayer(player,args[1]);
                    player.sendMessage("§a成功将玩家§6" + args[1] + "§a设置成一个猫娘，你成为了它的主人");
                } else {
                    player.sendMessage("§b它已经是一个猫娘了，它的主人是§6" + data.getString(args[1] + ".owner"));
                }

            }else {
                player.sendMessage(getMessage("command.no-permission"));
            }
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("remove")) {
            if (player.hasPermission("aineko.command.remove")) {
                if (confirmMap.getOrDefault(player, false)) {
                    //创建数据文件实例
                    File dataFile = new File("plugins/toNeko/nekos.yml");
                    // 加载数据文件
                    YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
                    //判断玩家是否为主人
                    if (data.getString(args[1] + ".owner") != null) {
                        if (data.getString(args[1] + ".owner").equals(player.getName())) {
                            //获取被删除的猫娘对象
                            Player neko = Bukkit.getPlayer(args[1]);
                            if (neko != null) {
                                    ConfigFileUtils.setNullValue(dataFile, args[1] + ".owner");
                                    ConfigFileUtils.setNullValue(dataFile, args[1] + ".aliases");
                                    ConfigFileUtils.setNullValue(dataFile, args[1] + ".type");
                                    ConfigFileUtils.setValue(args[1] + ".xp", 0, dataFile);
                                player.sendMessage("§a你已经成功删除了猫娘§6" + args[1]);
                            } else {
                                player.sendMessage("§c猫娘不存在");
                            }
                        } else {
                            player.sendMessage("§c你不是" + args[1] + "的主人!");
                        }
                    }
                    confirmMap.remove(player);  // 执行完毕后移除确认状态
                    return true;
                } else {
                    // 发出确认提醒
                    player.sendMessage("§c请再次输入该命令以确认执行: /toneko remove " + args[1]);
                    confirmMap.put(player, true);
                    return true;
                }
            }else {
                player.sendMessage(getMessage("command.no-permission"));
            }
        }
        return true;
    }
}
