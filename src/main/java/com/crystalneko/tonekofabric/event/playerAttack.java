package com.crystalneko.tonekofabric.event;

import com.crystalneko.ctlibPublic.sql.sqlite;
import com.crystalneko.ctlibPublic.network.httpGet;
import com.crystalneko.tonekofabric.libs.base;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.crystalneko.tonekofabric.libs.base.translatable;

public class playerAttack {
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    public playerAttack() {
        // 注册玩家攻击实体事件监听器
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            // 判断被攻击的实体是否为生物实体
            if (entity instanceof PlayerEntity attacker) {
                handlePlayerAttackEntity(player, attacker);
            }
            return ActionResult.PASS; // 允许其他mod处理该事件
        });
    }
    private void handlePlayerAttackEntity(PlayerEntity player, PlayerEntity neko) {
        //判断玩家是否死亡

        String nekoName = base.getPlayerName(neko);
        String playerName = base.getPlayerName(player);
        String worldName = base.getWorldName(player.getWorld());
        if(!sqlite.isTableExists(worldName + "Nekos")){
            sqlite.createTable(worldName + "Nekos");
            sqlite.addColumn(worldName + "Nekos","neko");
            sqlite.addColumn(worldName + "Nekos","owner");
        }
        sqlite.addColumn(worldName + "Nekos","xp");
        //如果没有使用物品或nbt对不上，直接返回
        if(player.getMainHandStack() == null){return;}else{
            ItemStack stack =  player.getMainHandStack(); //获取主手物品
            NbtCompound nbt = stack.getNbt(); //获取nbt
            if (nbt == null || !nbt.contains("neko") || !nbt.getString("neko").equalsIgnoreCase("true")) {
                return;
            }
        }
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
                if(sqlite.getColumnValue(worldName + "Nekos","xp","neko",nekoName) == null){
                    sqlite.saveDataWhere(worldName + "Nekos","xp","neko",nekoName,"0");
                }
                sqlite.saveDataWhere(worldName + "Nekos","xp","neko",nekoName,
                        String.valueOf(
                                Integer.parseInt(sqlite.getColumnValue(worldName + "Nekos","xp","neko",nekoName))
                                        + randomNumber));
                player.sendMessage(translatable("attack.add-xp",new String[]{ nekoName, String.valueOf(randomNumber)}));
                neko.sendMessage(translatable("attack.add-xp",new String[]{ nekoName, String.valueOf(randomNumber)}));
                // 向统计服务器发送请求
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        httpGet.get("http://toneko.cneko.org/stick?neko="+nekoName+"&&player="+playerName,null);
                    } catch (IOException ignored) {}
                }, executorService);
            }
        }
    }
}
