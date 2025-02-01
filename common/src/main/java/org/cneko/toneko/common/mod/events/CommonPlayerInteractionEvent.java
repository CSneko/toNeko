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
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.quirks.ModQuirk;
import org.cneko.toneko.common.quirks.Quirk;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CommonPlayerInteractionEvent {

    // 基础事件上下文接口
    private sealed interface EventContext permits DamageContext, AttackContext, InteractionContext {
        LivingEntity entity();
        NekoQuery.Neko neko();
    }

    // 交互事件上下文
    private record InteractionContext(
            ServerPlayer player,
            Level world,
            InteractionHand hand,
            INeko targetEntity,
            EntityHitResult hitResult,
            NekoQuery.Neko neko
    ) implements EventContext {
        @Override
        public LivingEntity entity() {
            return targetEntity.getEntity();
        }
    }

    // 伤害事件上下文
    private record DamageContext(
            LivingEntity entity,
            DamageSource damageSource,
            float damageValue,
            NekoQuery.Neko neko
    ) implements EventContext {}

    // 攻击事件上下文
    private record AttackContext(
            ServerPlayer attacker,
            Level level,
            InteractionHand hand,
            LivingEntity target,
            EntityHitResult hitResult,
            NekoQuery.Neko neko
    ) implements EventContext {
        @Override
        public LivingEntity entity() {
            return attacker;
        }
    }

    // 通用事件处理器
    private static <T extends EventContext> boolean processEvent(
            T context,
            BiConsumer<ModQuirk, T> eventAction,
            @Nullable Consumer<ModQuirk> xpHandler
    ) {
        boolean triggered = false;
        for (Quirk q : context.neko().getQuirks()) {
            if (q instanceof ModQuirk mq) {
                eventAction.accept(mq, context);
                if (xpHandler != null) {
                    xpHandler.accept(mq);
                }
                triggered = true;
            }
        }
        return triggered;
    }

    // 实体交互事件处理
    public static InteractionResult useEntity(Player player, Level world, InteractionHand hand,
                                              Entity entity, EntityHitResult hitResult) {
        if (!(entity instanceof INeko targetNekoEntity) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }

        NekoQuery.Neko targetNeko = NekoQuery.getNeko(targetNekoEntity.getEntity().getUUID());
        if (targetNeko == null) {
            return InteractionResult.PASS;
        }

        InteractionContext context = new InteractionContext(
                sp, world, hand, targetNekoEntity, hitResult, targetNeko
        );

        // 处理目标neko的特性
        if (targetNeko.hasOwner(player.getUUID())) {
            boolean success = processEvent(context, (mq, ctx) ->
                    mq.onNekoInteraction(ctx.player(), ctx.world(), ctx.hand(),
                            ctx.targetEntity(), ctx.hitResult()), null);

            if (success) return InteractionResult.SUCCESS;
        }

        // 处理玩家自身的特性
        NekoQuery.Neko playerNeko = NekoQuery.getNeko(player.getUUID());
        if (playerNeko != null) {
            processEvent(new InteractionContext(
                    sp, world, hand, targetNekoEntity, hitResult, playerNeko
            ), (mq, ctx) ->
                    mq.onInteractionOther(ctx.player(), ctx.world(), ctx.hand(),
                            ctx.targetEntity(), ctx.hitResult()), null);
        }

        return InteractionResult.PASS;
    }

    // 伤害事件处理
    public static boolean onDamage(LivingEntity entity, DamageSource source, float damage) {
        if (!(entity instanceof INeko nekoEntity)) return true;

        NekoQuery.Neko neko = nekoEntity.getNeko();
        if (neko == null) return true;

        processEvent(
                new DamageContext(entity, source, damage, neko),
                (mq, ctx) ->{
                    if (ctx.entity() instanceof INeko iNeko) {
                        mq.onDamage(iNeko, ctx.damageSource(), ctx.damageValue());
                    }
                },
                null
        );

        // 无论是否有quirk处理，都允许伤害
        return true;
    }

    // 攻击事件处理
    public static InteractionResult onAttackEntity(Player attacker, Level level, InteractionHand hand,
                                                   Entity target, EntityHitResult hitResult) {
        if (!(attacker instanceof ServerPlayer sp) || !(target instanceof LivingEntity le)) {
            return InteractionResult.PASS;
        }

        NekoQuery.Neko neko = NekoQuery.getNeko(attacker.getUUID());
        if (neko == null) {
            return InteractionResult.PASS;
        }

        for (Quirk q : neko.getQuirks()) {
            if (q instanceof ModQuirk mq) {
                InteractionResult result = mq.onNekoAttack(sp, level, hand, le, hitResult);
                if (result == InteractionResult.SUCCESS) {
                    neko.addXp(sp.getUUID(), mq.getInteractionValue());
                }
            }
        }

        return InteractionResult.PASS;
    }
}