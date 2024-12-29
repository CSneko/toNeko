package org.cneko.toneko.common.mod.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.quirks.ModQuirk;
import org.cneko.toneko.common.mod.quirks.QuirkContext;
import org.cneko.toneko.common.quirks.Quirk;

public class CommonPlayerInteractionEvent {
    public static InteractionResult useEntity(Player player, Level world, InteractionHand hand, Entity entity, EntityHitResult hitResult) {
        // 目标实体为玩家
        if(entity instanceof ServerPlayer nekoPlayer && player instanceof ServerPlayer sp){
            NekoQuery.Neko neko = NekoQuery.getNeko(nekoPlayer.getUUID());
            // 如果是猫娘且玩家是主人
            if(neko.isNeko() && neko.hasOwner(player.getUUID())){
                boolean success = false;
                // 如果有抚摸癖好且为玩家空手
                for (Quirk q : neko.getQuirks()) {
                    if (q instanceof ModQuirk mq){
                        if(mq.onNekoInteraction(player, world, hand, nekoPlayer, hitResult)==InteractionResult.SUCCESS){
                            neko.addXp(player.getUUID(), mq.getInteractionValue(QuirkContext.Builder.create(nekoPlayer).build()));
                            success = true;
                        };
                    }
                }
                if(success){
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.PASS;
    }


    public static boolean onDamage(LivingEntity entity, DamageSource damageSource, float v) {
        if (entity instanceof INeko nekoPlayer){
            NekoQuery.Neko neko = nekoPlayer.getNeko();
            for (Quirk q : neko.getQuirks()){
                if (q instanceof ModQuirk mq){
                    mq.onDamage(nekoPlayer, damageSource, v);
                }
            }
        }
        return true;
    }

    public static InteractionResult onAttackEntity(Player nekoPlayer, Level level, InteractionHand interactionHand, Entity entity, EntityHitResult entityHitResult) {
        NekoQuery.Neko neko = NekoQuery.getNeko(nekoPlayer.getUUID());
        boolean success = false;
        if (!(entity instanceof LivingEntity le)) return InteractionResult.PASS;
        for (Quirk q : neko.getQuirks()){
            if (q instanceof ModQuirk mq){
                if(mq.onNekoAttack(nekoPlayer, level, interactionHand, le, entityHitResult)==InteractionResult.SUCCESS){
                    success = true;
                }
            }
        }
        if(success){
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
