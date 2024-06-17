package com.crystalneko.toneko.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TabCompleter implements org.bukkit.command.TabCompleter {
    /**
     * 处理命令补全
     * @Date 10/2/2024
     */
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("toneko")) {
            return completeToneko(args);
        } else if (command.getName().equalsIgnoreCase("neko")) {
            return completeNeko(args);
        }else if(command.getName().equalsIgnoreCase("aineko")){
            return completeAINeko(args);
        }
        return Collections.emptyList();
    }

    /**
     *补全 /toneko 命令 |
     * 0=player,1=Player |
     * 0=remove,1=Player |
     * 0=xp,1=Player |
     * 0=aliases,1=Player,2=add/remove,3=<别名> |
     * 0=block,1=Player,2=add/remove,3=block,4=replace,5=word/all
     */
    private List<String> completeToneko(String[] args){
        List<String> complete = new ArrayList<>();
        if(args.length == 1){ // 补全 toneko 命令选项
            complete.addAll(Arrays.asList("help", "player", "item", "remove", "xp", "aliases", "block"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("player")) { // 补全 toneko player 子命令
            complete.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) { // 补全 toneko remove 子命令
            complete.addAll(catNames());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("xp")) { // 补全 toneko xp 子命令
            complete.addAll(catNames());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("aliases")) {
            complete.addAll(catNames());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("aliases")) { // 补全 toneko aliases 子命令
            complete.addAll(Arrays.asList("add", "remove"));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("aliases")) {
            complete.addAll(catNames());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("block")) {
            Collections.addAll(catNames());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("block")) {
            complete.addAll(Arrays.asList("add","remove"));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("block")) {
            complete.add("屏蔽词");
        } else if(args.length == 5 && args[0].equalsIgnoreCase("block")){
            complete.add("替换词");
        } else if(args.length == 6 && args[0].equalsIgnoreCase("block")){
            complete.addAll(Arrays.asList("all","word"));
            //block,player,block,replace,all
        }
        // 根据用户输入筛选出符合条件的补全字符串
        String prefix = args[args.length - 1]; 
        // 获取最后一个参数，作为前缀进行匹配
        complete.removeIf(option -> !option.toLowerCase().startsWith(prefix.toLowerCase()));
        return complete;
    }

    private List<String> completeNeko(String[] args){
        List<String> complete = new ArrayList<>();
        if(args.length == 1){
            complete.add("help");
            complete.add("vision");
            complete.add("jump");
        }
        return complete;
    }

    private List<String> completeAINeko(String[] args){
        List<String> complete = new ArrayList<>();
        complete.add("help");
        complete.add("add");
        complete.add("remove");
        return complete;
    }

    private List<String> catNames(){
        List<String> onlinePlayers = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            onlinePlayers.add(player.getName());
        }
        return onlinePlayers;
    }
}
