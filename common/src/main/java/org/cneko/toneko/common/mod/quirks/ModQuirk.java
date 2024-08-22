package org.cneko.toneko.common.mod.quirks;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.cneko.toneko.common.mod.entities.Neko;

import javax.annotation.Nullable;

public interface ModQuirk {
    /**
     * 添加悬浮文本，可返回null
     */
    @Nullable
    Component getTooltip();

    int getInteractionValue(QuirkContext context);

    default InteractionResult onNekoInteraction(Player owner, Level world, InteractionHand hand, Neko neko, EntityHitResult hitResult) {
        return InteractionResult.PASS;
    }

    default void onDamage(Neko neko, DamageSource damageSource, float amount){}

    default void onJoin(Neko neko) {}

    default InteractionResult onNekoAttack(Neko neko, Level level, InteractionHand interactionHand, LivingEntity entity, EntityHitResult entityHitResult){
        return InteractionResult.PASS;
    }
}
