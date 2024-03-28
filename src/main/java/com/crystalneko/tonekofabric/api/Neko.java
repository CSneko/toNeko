package com.crystalneko.tonekofabric.api;

import com.crystalneko.tonekofabric.entity.nekoEntity;
import com.crystalneko.tonekofabric.util.TextUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import static org.cneko.ctlib.common.util.LocalDataBase.Connections.sqlite;
public class Neko {
    public LivingEntity entity;
    public PlayerEntity player;
    public nekoEntity nekoEnt;
    public String name;
    public Neko(LivingEntity entity){
        //设置全局实体
        this.entity = entity;
        if(entity instanceof PlayerEntity){
            player = (PlayerEntity) entity;
            name = TextUtil.getPlayerName(player);
        }
        if(entity instanceof nekoEntity){
            nekoEnt = (nekoEntity) entity;
            name = nekoEnt.getNekoName();
        }
    }

    // 判断是否有主人
    public boolean hasOwner(){
        String worldName = TextUtil.getWorldName(entity.getWorld());
        return  sqlite.checkValueExists(worldName+"Nekos","neko",name);
    }
    //获取主人
    public String getOwner(){
        String worldName = TextUtil.getWorldName(entity.getWorld());
        return sqlite.getColumnValue(worldName+"Nekos","owner","name",name);
    }
    //获取猫娘名称
    public String getName(){
        if(entity instanceof PlayerEntity){
            player = (PlayerEntity) entity;
            return TextUtil.getPlayerName(player);
        }
        if(entity instanceof nekoEntity){
            nekoEnt = (nekoEntity) entity;
            return nekoEnt.getNekoName();
        }
        return "unnamed";
    }

}
