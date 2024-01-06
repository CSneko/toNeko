package com.crystalneko.tonekofabric.libs;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.luckperms.api.node.Node;
import net.minecraft.entity.player.PlayerEntity;


public class lp {
    private static Boolean enable;
    private static Boolean built = false;

    public lp(){
        //判断mod是否启用
        enable = FabricLoader.getInstance().isModLoaded("luckperms");
    }
    public static Boolean hasPermission(PlayerEntity player,String value){
        if(!enable){return true;}
        return Permissions.check(player,value);
    }
    public static Boolean build(){
        //如果已经注册过了或者没安装luckperms，直接返回
        if(!enable || built){return false;}
        built = true;
        Node.builder("toneko.command.player").value(true).build();
        Node.builder("toneko.command.aliases").value(true).build();
        Node.builder("toneko.command.item").value(true).build();
        Node.builder("toneko.command.block").value(true).build();
        Node.builder("toneko.command.xp").value(true).build();
        Node.builder("neko.command.jump").value(true).build();
        Node.builder("neko.command.vision").value(true).build();
        Node.builder("aineko.command.add").value(true).build();
        return true;
    }

}
