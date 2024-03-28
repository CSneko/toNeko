package com.crystalneko.tonekofabric.command;

import com.crystalneko.tonekofabric.libs.base;
import com.crystalneko.tonekofabric.libs.lp;
import com.crystalneko.tonekofabric.util.TextUtil;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;

import java.util.HashMap;
import java.util.Map;

import static com.crystalneko.tonekofabric.command.ToNekoCommand.noPS;
import static com.crystalneko.tonekofabric.api.Messages.translatable;
import static org.cneko.ctlib.common.util.LocalDataBase.Connections.sqlite;

public class AINekoCommand {
    private static Map<String,Boolean> remove = new HashMap();
    public static int add(CommandContext<ServerCommandSource> context){
        final ServerCommandSource source = context.getSource();
        final String worldName = TextUtil.getWorldName(source.getWorld());
        final PlayerEntity player = source.getPlayer();
        final String owner = TextUtil.getPlayerName(player);
        if(!lp.hasPermission(player, "aineko.command.add")){return noPS(player);}
        // 使用 getArgument 方法获取玩家名称
        String target = context.getArgument("neko", String.class);
        //判断是否有主人
        if (base.isNekoHasOwner(target, worldName) == null) {
            //设置AI为猫娘
            base.setPlayerNeko(target, worldName, source.getName());
            //设置值
            sqlite.saveData(worldName+"Nekos","neko",target);
            sqlite.saveDataWhere(worldName+"Nekos","xp","neko",target,"0");
            sqlite.saveDataWhere(worldName+"Nekos","type","neko",target,"AI");
            //设置主人的值
            sqlite.saveDataWhere(worldName+"Nekos","owner","neko",target,owner);
            //发送成功消息
            source.sendMessage(translatable("command.toneko.player.success", new String[]{target}));
        } else {
            source.sendMessage(base.getStringLanguage("command.toneko.player.nekoed", new String[]{base.isNekoHasOwner(target, worldName)}));
        }
        return 1;
    }
    public static int remove(CommandContext<ServerCommandSource> context){
        final ServerCommandSource source = context.getSource();
        final PlayerEntity player = source.getPlayer();
        final String worldName = TextUtil.getWorldName(player.getWorld());
        if(!lp.hasPermission(player, "toneko.command.remove")){return noPS(player);}
        //获取关键信息
        String playerName = TextUtil.getPlayerName(player); //玩家名称
        String neko = context.getArgument("neko", String.class); //猫娘的名称
        if (!base.getOwner(neko, worldName).equalsIgnoreCase(playerName)) {
            player.sendMessage(translatable("command.toneko.notOwner",new String[]{neko}));
            return 1; //不是主人，直接结束
        }
        //查看是否二次确认
        if(!remove.containsKey(playerName) || !remove.get(playerName)){
            player.sendMessage(translatable("command.toneko.remove.confirm"));
            remove.put(playerName,true);
            return 1;
        }
        player.sendMessage(translatable("command.toneko.remove.success",new String[]{neko}));
        sqlite.deleteLine(worldName+"Nekos","neko",neko);
        return 1;
    }
}
