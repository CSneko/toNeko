package com.crystalneko.tonekofabric.command;

import com.crystalneko.tonekofabric.libs.base;
import com.crystalneko.tonekofabric.util.TextUtil;
import com.crystalneko.tonekofabric.libs.lp;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;

import static com.crystalneko.tonekofabric.command.ToNekoCommand.noPS;
import static com.crystalneko.tonekofabric.api.Messages.translatable;
import static org.cneko.ctlib.common.util.LocalDataBase.Connections.sqlite;

public class NekoCommand {
    public static int jump(CommandContext<ServerCommandSource> context){
        final ServerCommandSource source = context.getSource();
        final PlayerEntity player = source.getPlayer();
        final String worldName = TextUtil.getWorldName(player.getWorld());
        if(!lp.hasPermission(player, "neko.command.jump")){return noPS(player);}
        String playerName = TextUtil.getPlayerName(player); //玩家名称
        if(!isNeko(playerName,worldName,player)){
            return 1; //不是猫娘，直接返回
        }
        //持续时间
        int duration = getDuration(playerName,worldName);
        //效果等级
        int amplifier = getAmplifier(playerName,worldName);
        StatusEffectInstance effect = new StatusEffectInstance(StatusEffects.JUMP_BOOST, duration, amplifier);
        player.addStatusEffect(effect);
        return 1;
    }
    public static int vision(CommandContext<ServerCommandSource> context){
        final ServerCommandSource source = context.getSource();
        final PlayerEntity player = source.getPlayer();
        final String worldName = TextUtil.getWorldName(player.getWorld());
        if(!lp.hasPermission(player, "neko.command.vision")){return noPS(player);}
        String playerName = TextUtil.getPlayerName(player); //玩家名称
        if(!isNeko(playerName,worldName,player)){
            return 1; //不是猫娘，直接返回
        }
        //持续时间
        int duration = getDuration(playerName,worldName);
        //效果等级
        int amplifier = getAmplifier(playerName,worldName);
        StatusEffectInstance effect = new StatusEffectInstance(StatusEffects.NIGHT_VISION, duration, amplifier);
        player.addStatusEffect(effect);
        return 1;
    }
    public static Boolean isNeko(String neko,String worldName,PlayerEntity player){
        if(base.isNekoHasOwner(neko,worldName)== null){
            player.sendMessage(translatable("command.neko.not-neko"));
            return false;
        }
        return true;
    }
    private static int getAmplifier(String  player,String worldName){
        int xp = Integer.parseInt(sqlite.getColumnValue(worldName+"Nekos","xp","neko",player));
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
    private static int getDuration(String  player,String worldName){
        int xp = Integer.parseInt(sqlite.getColumnValue(worldName+"Nekos","xp","neko",player));
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
