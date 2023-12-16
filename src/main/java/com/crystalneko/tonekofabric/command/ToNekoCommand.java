package com.crystalneko.tonekofabric.command;

import com.crystalneko.ctlibPublic.sql.sqlite;
import com.crystalneko.tonekofabric.items.stick;
import com.crystalneko.tonekofabric.libs.base;
import com.crystalneko.tonekofabric.libs.lp;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.List;

public class ToNekoCommand {
    //toneko player <neko>
    public static int Player(CommandContext<ServerCommandSource> context){
        final ServerCommandSource source = context.getSource();
        final String worldName = base.getWorldName(source.getWorld());
        final PlayerEntity player = source.getPlayer();
        if(!lp.hasPermission(player, "toneko.command.player")){return noPS(player);}
        // 使用 getArgument 方法获取玩家名称
        String target = context.getArgument("neko", String.class);
        //判断是否有主人
        if (base.isNekoHasOwner(target, worldName) == null) {
            //设置玩家为猫娘
            base.setPlayerNeko(target, worldName, source.getName());
            //发送成功消息
            source.sendMessage(base.getStringLanguage("message.toneko.player.success", new String[]{target}));
        } else {
            source.sendMessage(base.getStringLanguage("message.toneko.player.nekoed", new String[]{base.isNekoHasOwner(target, worldName)}));
        }
        return 1;
    }
    //toneko aliases <neko> add <aliases>
    public static int AliasesAdd(CommandContext<ServerCommandSource> context){
        final ServerCommandSource source = context.getSource();
        final PlayerEntity player = source.getPlayer();
        final String worldName = base.getWorldName(player.getWorld());
        if(!lp.hasPermission(player, "toneko.command.aliases")){return noPS(player);}
        sqlite.addColumn(worldName+"Nekos","aliases");
        String playerName = base.getPlayerName(player); //玩家名称
        String neko = context.getArgument("neko", String.class); //猫娘的名称
        String aliases = context.getArgument("aliases", String.class); //别名
        if (!base.getOwner(neko, worldName).equalsIgnoreCase(playerName)) {
            player.sendMessage(Text.translatable("message.toneko.notOwner", neko));
            return 1; //不是主人，直接结束
        }
        //设置别名
        String Aliases = sqlite.getColumnValue(worldName+"Nekos","aliases","neko",neko);
        if (Aliases != null && !Aliases.equalsIgnoreCase("null")) {
            // 使用split()方法将字符串按照逗号分隔成字符串数组
            String[] arr = Aliases.split(",");
            // 将数组转换为List
            List<String> list = Arrays.asList(arr);
            list.add(aliases);
            //判断别名是否存在
            if(list.contains(aliases)){
                //直接返回
                player.sendMessage(Text.translatable("message.toneko.aliases.add.exists"));
                return 1;
            }
            //将List转为String
            String result = String.join(",",list);
            //写入数据
            sqlite.saveDataWhere(worldName+"Nekos","aliases","neko",neko,result);
        }else {
            //直接设置别名
            sqlite.saveDataWhere(worldName+"Nekos","aliases","neko",neko,aliases);
        }
        player.sendMessage(Text.translatable("message.toneko.aliases.add.true"));
        return 1;
    }
    //toneko aliases <neko> remove <aliases>
    public static int AliasesRemove(CommandContext<ServerCommandSource> context){
        final ServerCommandSource source = context.getSource();
        final PlayerEntity player = source.getPlayer();
        final String worldName = base.getWorldName(player.getWorld());
        if(!lp.hasPermission(player, "toneko.command.aliases")){return noPS(player);}
        sqlite.addColumn(worldName+"Nekos","aliases");
        String playerName = base.getPlayerName(player); //玩家名称
        String neko = context.getArgument("neko", String.class); //猫娘的名称
        String aliases = context.getArgument("aliases", String.class); //别名
        if (!base.getOwner(neko, worldName).equalsIgnoreCase(playerName)) {
            player.sendMessage(Text.translatable("message.toneko.notOwner", neko));
            return 1; //不是主人，直接结束
        }
        //获取别名列表
        String Aliases = sqlite.getColumnValue(worldName+"Nekos","aliases","neko",neko);
        if (Aliases != null && !Aliases.equalsIgnoreCase("null")) {
            // 使用split()方法将字符串按照逗号分隔成字符串数组
            String[] arr = Aliases.split(",");
            // 将数组转换为List
            List<String> list = Arrays.asList(arr);
            list.add(aliases);
            //判断别名是否存在
            if(!list.contains(aliases)){
                //不存在
                player.sendMessage(Text.translatable("message.toneko.aliases.remove.no"));
            }else {
                list.remove(aliases);
                player.sendMessage(Text.translatable("message.toneko.aliases.remove.true"));
            }
        }else {
            //直接返回失败
            player.sendMessage(Text.translatable("message.toneko.aliases.remove.no"));
            return 1;
        }
        return 1;
    }
    public static int item(CommandContext<ServerCommandSource> context){
        final ServerCommandSource source = context.getSource();
        final PlayerEntity player = source.getPlayer();
        final World world = player.getWorld();
        if(!lp.hasPermission(player, "toneko.command.item")){return noPS(player);}
        //给予玩家撅猫棒
        Vec3d pos = player.getPos();
        ItemEntity itemEntity = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), stick.get()); //在玩家脚下生成一个掉落物
        world.spawnEntity(itemEntity);
        return 1;
    }
    public static int noPS(PlayerEntity player){
        player.sendMessage(Text.translatable("message.permission.no"));
        return 1;
    }
}
