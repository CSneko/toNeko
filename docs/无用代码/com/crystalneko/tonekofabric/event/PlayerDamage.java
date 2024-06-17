package com.crystalneko.tonekofabric.event;

import com.crystalneko.tonekofabric.api.Messages;
import com.crystalneko.tonekofabric.util.TextUtil;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;

import static com.crystalneko.tonekofabric.api.Messages.translatable;

public class PlayerDamage {
    public static void init(){
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if(entity instanceof PlayerEntity player){
                onPlayerDamage(player, source, amount);
            }
            return true;
        });    
    }
    
    public static void onPlayerDamage(PlayerEntity player, DamageSource source, float amount){
        String playerName = TextUtil.getPlayerName(player);
        player.sendMessage(translatable("msg.toneko.hurt",playerName),true);
        if(amount > 10){
            player.sendMessage(translatable("msg.toneko.high-hurt",playerName),true);
        }
        if(player.getHealth() <= 5 && player.getHealth() > 0 ){
            player.sendMessage(translatable("msg.toneko.low-heart",playerName),true);
        }
        if(player.getHealth() <= 0){
            Messages.setTrash(playerName,true);
            player.sendMessage(translatable("msg.toneko.win",playerName),true);
            player.sendMessage(translatable("msg.toneko.death",playerName),true);
        }
    }
}
