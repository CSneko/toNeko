package com.crystalneko.toneko.command;

import com.crystalneko.toneko.items.StickItemWrapper;
import com.crystalneko.toneko.utils.ConfigFileUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.cneko.ctlib.common.util.ChatPrefix;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.crystalneko.toneko.ToNeko.getMessage;
import static org.cneko.ctlib.common.util.LocalDataBase.Connections.sqlite;

public class ToNekoCommand implements CommandExecutor {
    private final Map<Player, Boolean> confirmMap = new ConcurrentHashMap<>(); //MT-Map for folia support

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("command.only-player"));
            return true;
        }

        Player player = (Player) sender;

        // 处理子命令
        if(args.length == 0){
            player.sendMessage(getMessage("command.toneko.help"));
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("player")) {
            if (player.hasPermission("toneko.command.player")) {
                //创建数据文件实例
                File dataFile = new File("plugins/toNeko/nekos.yml");
                // 加载数据文件
                YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
                //创建数据值
                Boolean created = ConfigFileUtils.createNewKey(args[1] + "." + "owner", player.getName(), data, dataFile);
                ConfigFileUtils.createNewKey(args[1] + "." + "xp", 0, data, dataFile);
                List<String> aliases = new ArrayList<>();
                aliases.add("CrystalNekoooo");
                ConfigFileUtils.createNewKey(args[1] + "." + "aliases", aliases, data, dataFile);
                if (created) {
                    killPlayer(player,args[1]);
                    player.sendMessage(getMessage("command.toneko.player.success",new String[]{args[1]}));
                    //判断猫娘是否在线
                    if(isPlayerOnline(args[1])) {
                        Player neko = Bukkit.getPlayer(args[1]);
                        if (neko != null) {
                            ChatPrefix.addPrivatePrefix(neko.getName(), getMessage("chat.neko.prefix"));
                        }
                    }
                } else {
                    player.sendMessage(getMessage("command.toneko.player.nekoed",new String[]{data.getString(args[1] + ".owner")}));
                }
            }else {
                player.sendMessage(getMessage("command.no-permission"));
            }

        } else if (args[0].equalsIgnoreCase("help")) {
            player.sendMessage(getMessage("command.toneko.help"));
        } else if (args[0].equalsIgnoreCase("item")) {
            if (player.hasPermission("toneko.command.item")){
                StickItemWrapper.giveStickToPlayer(player);
            }else {
                player.sendMessage(getMessage("command.no-permission"));
            }
        }else if (args.length == 2 && args[0].equalsIgnoreCase("remove")){
            if (player.hasPermission("toneko.command.remove")) {
                if (confirmMap.getOrDefault(player, false)) {
                    //创建数据文件实例
                    File dataFile = new File("plugins/toNeko/nekos.yml");
                    // 加载数据文件
                    YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
                    //判断玩家是否为主人
                    if (data.getString(args[1] + ".owner") != null) {
                        if (Objects.requireNonNull(data.getString(args[1] + ".owner")).equalsIgnoreCase(player.getName())) {
                            //获取被删除的猫娘对象
                            Player neko = Bukkit.getPlayer(args[1]);
                            if (neko != null) {
                                ChatPrefix.removePrivatePrefix(neko.getName(), getMessage("chat.neko.prefix"));
                                data.set(args[1] + ".owner",null);
                                data.set(args[1] + ".aliases",null);
                                ConfigFileUtils.setValue(args[1] + ".xp", 0, dataFile);
                                player.sendMessage(getMessage("command.toneko.remove.confirm") +args[1]);
                            } else {
                                player.sendMessage("§c猫娘不存在或不在线");
                            }
                        } else {
                            player.sendMessage(getMessage("command.toneko.notOwner",new String[]{args[1]}));
                        }
                    }
                    confirmMap.remove(player);  // 执行完毕后移除确认状态
                } else {
                    // 发出确认提醒
                    player.sendMessage(getMessage("command.toneko.remove.confirm") +": /toneko remove " + args[1]);
                    confirmMap.put(player, true);
                }
                return true;
            } else {
                player.sendMessage(getMessage("command.no-permission"));
            }
        }else if(args.length == 2 && args[0].equalsIgnoreCase("xp")){
            if(player.hasPermission("toneko.command.xp")) {
                //创建数据文件实例
                File dataFile = new File("plugins/toNeko/nekos.yml");
                // 加载数据文件
                YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
                //判断玩家是否为主人
                if (data.getString(args[1] + ".owner") != null) {
                    if (Objects.requireNonNull(data.getString(args[1] + ".owner")).equalsIgnoreCase(player.getName())) {
                        int xp = data.getInt(args[1] + ".xp");
                        player.sendMessage("§a你与§e" + args[1] + "§b的好感经验为§e" + xp);
                    } else {
                        player.sendMessage(getMessage("command.toneko.notOwner",new String[]{args[1]}));
                    }
                } else {
                    if (data.getString(args[1] + ".owner") != null) {
                        if (Objects.requireNonNull(data.getString(args[1] + ".owner")).equalsIgnoreCase(player.getName())) {
                            ConfigFileUtils.createNewKey(args[1] + "." + "xp", player.getName(), data, dataFile);
                        }
                    } else {
                        player.sendMessage(getMessage("command.toneko.notOwner",new String[]{args[1]}));
                    }
                }
            }else {
                player.sendMessage(getMessage("command.no-permission"));
            }
        }else if(args.length == 4 && args[0].equalsIgnoreCase("aliases")){
            if(player.hasPermission("toneko.command.aliases")){
                //创建数据文件实例
                File dataFile = new File("plugins/toNeko/nekos.yml");
                // 加载数据文件
                YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
                //判断玩家是否为主人
                if (data.getString(args[1] + ".owner") != null) {
                    if (Objects.requireNonNull(data.getString(args[1] + ".owner")).equalsIgnoreCase(player.getName())) {
                        //添加别名
                        if(args[2].equalsIgnoreCase("add")){
                            if(data.getList(args[1] + ".aliases") != null){
                                //获取别名
                                List<String> aliases = data.getStringList(args[1] + ".aliases");
                                //将新别名添加到别名
                                aliases.add(args[3]);
                                ConfigFileUtils.setValue(args[1] + ".aliases",aliases,dataFile);
                                player.sendMessage(getMessage("command.toneko.aliases.add.true") + args[3]);
                            } else {
                                //如果没有则创建值
                                List<String> aliases = new ArrayList<>();
                                aliases.add(args[3]);
                                ConfigFileUtils.createNewKey(args[1]+".aliases",aliases,data,dataFile);
                                player.sendMessage(getMessage("command.toneko.aliases.add.true") + args[3]);
                            }
                        } else if(args[2].equalsIgnoreCase("remove")){
                            //获取别名
                            List<String> aliases = data.getStringList(args[1] + ".aliases");
                            //判断别名是否存在
                            if(aliases.contains(args[3])) {
                                aliases.remove(args[3]);
                                ConfigFileUtils.setValue(args[1] + ".aliases",aliases,dataFile);
                                player.sendMessage(getMessage("command.toneko.aliases.remove.true"));
                            }else {player.sendMessage(getMessage("command.toneko.aliases.remove.no"));}
                        }else {player.sendMessage("§c你的命令/neko "+args[0]+" "+args[1]+"有误，请确认输入的是add或remove");}
                    } else {
                        player.sendMessage(getMessage("command.toneko.notOwner",new String[]{args[1]}));
                    }
                } else {
                        player.sendMessage(getMessage("command.toneko.notOwner",new String[]{args[1]}));
                }
            }else {
                player.sendMessage(getMessage("command.no-permission"));
            }
        }else if (args.length == 6 && args[0].equalsIgnoreCase("block")) {
            /*替换词 1=猫娘名称 2= add/remove 3=屏蔽词 4=替换词 5=all/word
            数据库结构
            | 名称 | 屏蔽词 | 替换词 | 替换方式 |
            | cat | b1,b2 | r1,r2 |word,all|
            | c2  |   b5  |   r5  |  all   |
             */
                File dataFile = new File("plugins/toNeko/nekos.yml");
                // 加载数据文件
                YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
                //判断玩家是否为主人
                if (data.getString(args[1] + ".owner") != null) {
                    if (Objects.requireNonNull(data.getString(args[1] + ".owner")).equalsIgnoreCase(player.getName())) {
                        if(args[5].equalsIgnoreCase("all") || args[5].equalsIgnoreCase("word")){
                            //写入数据库
                            if(args[2].equalsIgnoreCase("add")) {
                                sqlite.addColumn("nekoblockword","block");
                                sqlite.addColumn("nekoblockword","replace");
                                sqlite.addColumn("nekoblockword","method");
                                if (!sqlite.checkValueExists("nekoblockword", "neko", args[1])) {
                                    sqlite.saveData("nekoblockword", "neko", args[1]);
                                }
                                //读取数据
                                String block = sqlite.getColumnValue("nekoblockword", "block", "neko", args[1]);
                                String replace = sqlite.getColumnValue("nekoblockword", "replace", "neko", args[1]);
                                String method = sqlite.getColumnValue("nekoblockword", "method", "neko", args[1]);
                                if (block != null) {
                                    //将值转化为数组
                                    String[] blocks = block.split(",");
                                    if(!contains(blocks,args[3])) {
                                        //设置值，以逗号分阁
                                        block = block + "," + args[3];
                                        replace = replace + "," + args[4];
                                        method = method + "," + args[5];
                                    } else {player.sendMessage(getMessage("command.toneko.block.exists"));return true;}
                                } else {
                                    block = args[3];
                                    replace = args[4];
                                    method = args[5];
                                }
                                //保存屏蔽词
                                sqlite.saveDataWhere("nekoblockword", "block", "neko", args[1], block);
                                sqlite.saveDataWhere("nekoblockword", "replace", "neko", args[1], replace);
                                sqlite.saveDataWhere("nekoblockword", "method", "neko", args[1], method);
                                player.sendMessage(getMessage("command.toneko.block.add.success"));
                            } else if (args[2].equalsIgnoreCase("remove")) {
                                //判断猫娘数据是否存在
                                if (!sqlite.checkValueExists("nekoblockword", "neko", args[1])) {
                                    player.sendMessage(getMessage("command.toneko.block.no-exists"));
                                    return true;
                                }
                                //读取数据
                                String block = sqlite.getColumnValue("nekoblockword", "block", "neko", args[1]);
                                String replace = sqlite.getColumnValue("nekoblockword", "replace", "neko", args[1]);
                                String method = sqlite.getColumnValue("nekoblockword", "method", "neko", args[1]);
                                if (block != null) {
                                    //将值转换为数组
                                    String[] blocks = block.split(",");
                                    String[] replaces = replace.split(",");
                                    String[] methods = method.split(",");
                                    //寻找值是否存在并获取索引
                                    int index = Arrays.binarySearch(blocks,args[3]);
                                    if(index >=0){
                                        //删除屏蔽词
                                        blocks = deleteIndex(blocks,index);
                                        replaces = deleteIndex(replaces,index);
                                        methods = deleteIndex(methods,index);
                                        //保存屏蔽词
                                        sqlite.saveDataWhere("nekoblockword", "block", "neko", args[1], String.join(",",blocks));
                                        sqlite.saveDataWhere("nekoblockword", "replace", "neko", args[1], String.join(",",replaces));
                                        sqlite.saveDataWhere("nekoblockword", "method", "neko", args[1], String.join(",",methods));
                                        player.sendMessage(getMessage("command.toneko.block.remove.success"));
                                    } else {player.sendMessage(getMessage("command.toneko.block.no-exists"));return true;}
                                }

                            }
                        }else {
                           player.sendMessage(getMessage("command.parameter.error"));
                        }
                    } else {
                        player.sendMessage(getMessage("command.toneko.notOwner",new String[]{args[1]}));
                    }
                } else {
                        player.sendMessage(getMessage("command.toneko.notOwner",new String[]{args[1]}));
                }
        } else {
            player.sendMessage(getMessage("command.neko.Invalid"));
        }
        return true;
    }


    //祭献操作
    public static void killPlayer(Player player, String neko) {
        //死亡提示
        Bukkit.getServer().broadcastMessage(getMessage("command.toneko.player.death",new String[]{player.getName(),neko}));
        // 执行玩家死亡操作
        player.setHealth(0);
    }
    public static boolean isPlayerOnline(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        return (player != null && player.isOnline());
    }
    public static String[] deleteIndex(String[] arr,int index){
        // 将目标索引后面的元素向前移动一位
        for (int i = index; i < arr.length - 1; i++) {
            arr[i] = arr[i + 1];
        }
        // 将数组的长度减一
        return Arrays.copyOf(arr, arr.length - 1);
    }
    //判断数组的某个值是否存在
    public static boolean contains(String[] array, String value) {
        for (String str : array) {
            if (str.equals(value)) {
                return true;
            }
        }
        return false;
    }

}
