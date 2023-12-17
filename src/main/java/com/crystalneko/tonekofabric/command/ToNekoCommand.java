package com.crystalneko.tonekofabric.command;

import com.crystalneko.ctlibPublic.inGame.chatPrefix;
import com.crystalneko.ctlibPublic.sql.sqlite;
import com.crystalneko.tonekofabric.items.stick;
import com.crystalneko.tonekofabric.libs.base;
import com.crystalneko.tonekofabric.libs.lp;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.List;

import static com.crystalneko.tonekofabric.libs.base.translatable;

public class ToNekoCommand {
    //toneko player <neko>
    public static int Player(CommandContext<ServerCommandSource> context){
        final ServerCommandSource source = context.getSource();
        final String worldName = base.getWorldName(source.getWorld());
        final PlayerEntity player = source.getPlayer();
        String prefix = translatable("chat.neko.prefix").getString();
        if(!lp.hasPermission(player, "toneko.command.player")){return noPS(player);}
        // 使用 getArgument 方法获取玩家名称
        String target = context.getArgument("neko", String.class);
        //判断是否有主人
        if (base.isNekoHasOwner(target, worldName) == null) {
            //设置玩家为猫娘
            base.setPlayerNeko(target, worldName, source.getName());
            //给予玩家前缀
            chatPrefix.addPrivatePrefix(target,prefix);
            //发送成功消息
            source.sendMessage(translatable("command.toneko.player.success", new String[]{target}));
        } else {
            source.sendMessage(base.getStringLanguage("command.toneko.player.nekoed", new String[]{base.isNekoHasOwner(target, worldName)}));
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
            player.sendMessage(translatable("command.toneko.notOwner",new String[]{neko}));
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
                player.sendMessage(translatable("command.toneko.aliases.add.exists"));
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
        player.sendMessage(translatable("command.toneko.aliases.add.true"));
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
            player.sendMessage(translatable("command.toneko.notOwner",new String[]{neko}));
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
                player.sendMessage(translatable("command.toneko.aliases.remove.no"));
            }else {
                list.remove(aliases);
                player.sendMessage(translatable("command.toneko.aliases.remove.true"));
            }
        }else {
            //直接返回失败
            player.sendMessage(translatable("command.toneko.aliases.remove.no"));
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
    public static int addBlock(CommandContext<ServerCommandSource> context){
        final ServerCommandSource source = context.getSource();
        final PlayerEntity player = source.getPlayer();
        final String worldName = base.getWorldName(player.getWorld());
        if(!lp.hasPermission(player, "toneko.command.block")){return noPS(player);}

        //获取关键信息
        String playerName = base.getPlayerName(player); //玩家名称
        String neko = context.getArgument("neko", String.class); //猫娘的名称
        String block = context.getArgument("block", String.class); //屏蔽词
        String replace = context.getArgument("replace", String.class); //替换词
        String method = context.getArgument("method", String.class); //all or word

        blockTable(worldName,neko);

        if (!base.getOwner(neko, worldName).equalsIgnoreCase(playerName)) {
            player.sendMessage(translatable("command.toneko.notOwner",new String[]{neko}));
            return 1; //不是主人，直接结束
        }

        //读取原值
        String savedBlock = sqlite.getColumnValue(worldName+"Nekos","block","neko",neko);
        String savedReplace = sqlite.getColumnValue(worldName+"Nekos","replace","neko",neko);
        String savedMethod = sqlite.getColumnValue(worldName+"Nekos","method","neko",neko);
        //转换为数组
        String[] blockArr = savedBlock.split(",");

        if(contains(blockArr,block)){player.sendMessage(translatable("command.toneko.block.exists"));return 1;} //屏蔽词存在

        //保存数据
        savedBlock = savedBlock + "," + block;
        savedReplace = savedReplace + "," + replace;
        savedMethod = savedMethod + "," + method;
        sqlite.saveDataWhere(worldName+"Nekos","block","neko",neko,savedBlock);
        sqlite.saveDataWhere(worldName+"Nekos","replace","neko",neko,savedReplace);
        sqlite.saveDataWhere(worldName+"Nekos","method","neko",neko,savedMethod);

        player.sendMessage(translatable("command.toneko.block.add.success"));
        return 1;
    }

    public static int removeBlock(CommandContext<ServerCommandSource> context){
        final ServerCommandSource source = context.getSource();
        final PlayerEntity player = source.getPlayer();
        final String worldName = base.getWorldName(player.getWorld());
        if(!lp.hasPermission(player, "toneko.command.block")){return noPS(player);}

        //获取关键信息
        String playerName = base.getPlayerName(player); //玩家名称
        String neko = context.getArgument("neko", String.class); //猫娘的名称
        String block = context.getArgument("block", String.class); //屏蔽词

        blockTable(worldName,neko);

        if (!base.getOwner(neko, worldName).equalsIgnoreCase(playerName)) {
            player.sendMessage(translatable("command.toneko.notOwner",new String[]{neko}));
            return 1; //不是主人，直接结束
        }

        //读取原值
        String savedBlock = sqlite.getColumnValue(worldName+"Nekos","block","neko",neko);
        String savedReplace = sqlite.getColumnValue(worldName+"Nekos","replace","neko",neko);
        String savedMethod = sqlite.getColumnValue(worldName+"Nekos","method","neko",neko);
        //转换为数组
        String[] blockArr = savedBlock.split(",");
        String[] replaceArr = savedReplace.split(",");
        String[] methodArr = savedMethod.split(",");

        if(!contains(blockArr,block)){player.sendMessage(translatable("command.toneko.block.no-exists"));return 1;} //屏蔽词不存在

        int index = Arrays.binarySearch(blockArr,block); //获取引导

        //删除引导
        blockArr = deleteIndex(blockArr,index);
        replaceArr = deleteIndex(replaceArr,index);
        methodArr = deleteIndex(methodArr,index);

        //保存屏蔽词
        sqlite.saveDataWhere(worldName+"Nekos","block","neko",neko,String.join(",",blockArr));
        sqlite.saveDataWhere(worldName+"Nekos","replace","neko",neko,String.join(",",replaceArr));
        sqlite.saveDataWhere(worldName+"Nekos","method","neko",neko,String.join(",",methodArr));

        player.sendMessage(translatable("command.toneko.block.remove.success"));
        return 1;
    }

    public static int noPS(PlayerEntity player){
        player.sendMessage(translatable("command.no-permission"));
        return 1;
    }
    public static void blockTable(String worldName,String neko){
        sqlite.addColumn(worldName+"Nekos","block");
        sqlite.addColumn(worldName+"Nekos","replace");
        sqlite.addColumn(worldName+"Nekos","method");
        if(sqlite.getColumnValue(worldName+"Nekos","block","neko",neko) == null){
            sqlite.saveDataWhere(worldName+"Nekos","block","neko",neko,"CrystalNeko");
            sqlite.saveDataWhere(worldName+"Nekos","replace","neko",neko,"CrystalNeko");
            sqlite.saveDataWhere(worldName+"Nekos","method","neko",neko,"word");
        }
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

    public static String[] deleteIndex(String[] arr,int index){
        // 将目标索引后面的元素向前移动一位
        for (int i = index; i < arr.length - 1; i++) {
            arr[i] = arr[i + 1];
        }
        // 将数组的长度减一
        String[] newArr = Arrays.copyOf(arr, arr.length - 1);
        return newArr;
    }
}
