package com.crystalneko.toneko.command;


import com.crystalneko.toneko.ToNeko;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.*;

public class NekoCommand implements CommandExecutor{

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ToNeko.getMessage("command.only-player"));
            return true;
        }
        Player player = (Player) sender;
        // 处理子命令
        if (args[0].equalsIgnoreCase("help")) {
            player.sendMessage(ToNeko.getMessage("command.neko.help"));
        } else if(args[0].equalsIgnoreCase("jump")){
            //给予跳跃提升效果
            if(player.hasPermission("neko.command.jump")) {
                if (isPlayerNeko(player)) {
                    //药水效果
                    PotionEffectType effectType = PotionEffectType.JUMP;
                    //持续时间
                    int duration = getDuration(player);
                    //效果等级
                    int amplifier = getAmplifier(player);
                    givePlayerPotionEffect(player, effectType, duration, amplifier);
                }
            } else {player.sendMessage(ToNeko.getMessage("command.no-permission"));}
        }else if(args[0].equalsIgnoreCase("vision")){
            if(player.hasPermission("neko.command.vision")){
                if (isPlayerNeko(player)) {
                    //药水效果
                    PotionEffectType effectType = PotionEffectType.NIGHT_VISION;
                    //持续时间
                    int duration = getDuration(player);
                    //效果等级
                    int amplifier = getAmplifier(player);
                    givePlayerPotionEffect(player, effectType, duration, amplifier);
                }
            } else {player.sendMessage(ToNeko.getMessage("command.no-permission"));}
        } else {
            player.sendMessage(ToNeko.getMessage("command.neko.Invalid"));
        }
        return true;
    }

    // 给予玩家药水效果的方法
    private void givePlayerPotionEffect(Player player, PotionEffectType type, int duration, int amplifier) {
        PotionEffect effect = new PotionEffect(type, duration, amplifier);
        player.addPotionEffect(effect);
    }

    //判断玩家是否为猫娘(是否有主人)
    private Boolean isPlayerNeko(Player player){
        //创建数据文件实例
        File dataFile = new File("plugins/toNeko/nekos.yml");
        // 加载数据文件
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        if(data.getString(player.getName() + ".owner") != null){
            return true;
        } else {
            player.sendMessage(ToNeko.getMessage("command.neko.not-neko"));
            return false;
        }
    }

    //根据xp设置药水效果时间
    private int getAmplifier(Player player){
        //创建数据文件实例
        File dataFile = new File("plugins/toNeko/nekos.yml");
        // 加载数据文件
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        int xp =data.getInt(player.getName() + ".xp");
        int duration = 1;
        if(xp >= 500 && xp < 1000 ){
            duration = 2;
        } else if(xp >= 1000 && xp < 2000){
            duration = 3;
        } else if(xp >= 2000 && xp <4000){
            duration = 4;
        } else if(xp >= 4000 && xp <8000){
            duration = 5;
        } else if(xp >= 8000 && xp <16000){
            duration = 6;
        } else if(xp >= 16000){
            duration = 7;
        }
        return duration;
    }

    //根据xp设置药水效果时间
    private int getDuration(Player player){
        //创建数据文件实例
        File dataFile = new File("plugins/toNeko/nekos.yml");
        // 加载数据文件
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        int xp =data.getInt(player.getName() + ".xp");
        int duration = 12000;
        if(xp >= 500 && xp < 1000 ){
            duration = 36000;
        } else if(xp >= 1000 && xp < 2000){
            duration = 72000;
        } else if(xp >= 2000 && xp <4000){
            duration = 140000;
        } else if(xp >= 4000 && xp <8000){
            duration = 280000;
        } else if(xp >= 8000 && xp <16000){
            duration = 600000;
        } else if(xp >= 16000){
            duration = 1000000;
        }
        return duration;
    }

}
