package com.crystalneko.tonekofabric.event;

import com.crystalneko.ctlibPublic.sql.sqlite;
import com.crystalneko.tonekofabric.ToNekoFabric;
import com.crystalneko.tonekofabric.libs.base;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

import java.util.Random;

public class playerAttack {
    public playerAttack() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // 注册玩家攻击实体事件监听器
            AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
                // 判断被攻击的实体是否为生物实体
                if (entity instanceof PlayerEntity attacker) {
                    handlePlayerAttackEntity(player, attacker);
                }
                return ActionResult.PASS; // 允许其他mod处理该事件
            });
        });
    }
    private void handlePlayerAttackEntity(PlayerEntity player, PlayerEntity neko) {
        String nekoName = base.getPlayerName(neko);
        String playerName = base.getPlayerName(player);
        String worldName = base.getWorldName(player.getWorld());
        if(!sqlite.isTableExists(worldName + "Nekos")){
            sqlite.createTable(worldName + "Nekos");
            sqlite.addColumn(worldName + "Nekos","neko");
            sqlite.addColumn(worldName + "Nekos","owner");
        }
        sqlite.addColumn(worldName + "Nekos","xp");
        //判断玩家手中的物品
        if(player.isUsingItem() && player.getMainHandStack().equals(ToNekoFabric.STICK.getDefaultStack())){
            //判断对方是否为猫娘
            if(sqlite.getColumnValue(worldName + "Nekos","owner","neko",nekoName) != null){
                // 创建药水效果实例（持续时间：10秒，效果等级：1
                StatusEffectInstance weakness = new StatusEffectInstance(StatusEffects.WEAKNESS, 200, 0);
                //给予效果
                neko.addStatusEffect(weakness);
                //判断是否为主人
                if(base.getOwner(nekoName,worldName).equalsIgnoreCase(playerName)){
                    //生成随机数
                    Random random = new Random();
                    int randomNumber = random.nextInt(6) - 2;
                    sqlite.saveDataWhere(worldName + "Nekos","xp","neko",nekoName, String.valueOf(Integer.parseInt(sqlite.getColumnValue(worldName + "Nekos","xp","neko",nekoName)) + randomNumber));
                    player.sendMessage(Text.translatable("attack.xp.add", nekoName, String.valueOf(randomNumber)));
                    neko.sendMessage(Text.translatable("attack.xp.add", playerName, String.valueOf(randomNumber)));
                }
            }
        }
    }
}
