package org.cneko.toneko.common.mod.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.cneko.toneko.common.api.NekoQuery;
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
                // 如果有抚摸癖好且为玩家空手
                for (Quirk q : neko.getQuirks()) {
                    if (q instanceof ModQuirk mq){
                        if(mq.onNekoInteraction(player, world, hand, nekoPlayer, hitResult)==InteractionResult.SUCCESS){
                            neko.addXp(player.getUUID(), mq.getInteractionValue(QuirkContext.Builder.create(nekoPlayer).build()));
                            return InteractionResult.SUCCESS;
                        };
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }

    public static void afterDeath(LivingEntity livingEntity, DamageSource damageSource) {

    }
}
