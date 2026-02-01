package org.cneko.toneko.common.mod.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.entities.boss.mouflet.MoufletNekoBoss;
import org.cneko.toneko.common.mod.quirks.Quirk;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CommonPlayerInteractionEvent {

    public static InteractionResult useBlock(Player player, Level level, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        // 如果附近32格有战斗状态的MoufletNekoBoss实体，则不允许使用容器
        if (level.getEntitiesOfClass(MoufletNekoBoss.class, player.getBoundingBox().inflate(32)).stream()
                .anyMatch(neko -> !neko.isPetMode())) {
            // 获取方块实体
            var block = level.getBlockEntity(blockHitResult.getBlockPos());
            if (block instanceof Container) {
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }

    // 基础事件上下文接口
    private sealed interface EventContext permits DamageContext, AttackContext, InteractionContext {
        LivingEntity entity();
    }

    // 交互事件上下文
    private record InteractionContext(
            ServerPlayer player,
            Level world,
            InteractionHand hand,
            INeko targetEntity,
            EntityHitResult hitResult
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
            float damageValue
    ) implements EventContext {}

    // 攻击事件上下文
    private record AttackContext(
            ServerPlayer attacker,
            Level level,
            InteractionHand hand,
            LivingEntity target,
            EntityHitResult hitResult
    ) implements EventContext {
        @Override
        public LivingEntity entity() {
            return attacker;
        }
    }

    // 通用事件处理器
    private static <T extends EventContext> boolean processEvent(
            T context,
            BiConsumer<Quirk, T> eventAction,
            @Nullable Consumer<Quirk> xpHandler
    ) {
        boolean triggered = false;
        INeko neko = null;
        // 尝试从context.entity()获取INeko实例
        if (context.entity() instanceof INeko n) {
            neko = n;
        }
        if (neko == null) return false;
        for (Quirk q : neko.getQuirks()) {
            if (q instanceof Quirk mq) {
                eventAction.accept(mq, context);
                if (xpHandler != null) {
                    xpHandler.accept(mq);
                }
                triggered = true;
            }
        }
        return triggered;
    }

    // 实体交互件处理
    public static InteractionResult useEntity(Player player, Level world, InteractionHand hand,
                                              Entity entity, EntityHitResult hitResult) {
        if (!(entity instanceof INeko targetNeko) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }

        InteractionContext context = new InteractionContext(
                sp, world, hand, targetNeko, hitResult
        );

        // 处理目标neko的特性
        if (targetNeko.hasOwner(player.getUUID())) {
            boolean success = processEvent(context, (mq, ctx) ->
                    mq.onNekoInteraction(ctx.player(), ctx.world(), ctx.hand(),
                            ctx.targetEntity(), ctx.hitResult()),
                    q -> sp.setXpWithOwner(sp.getUUID(), q.getInteractionValue() + sp.getXpWithOwner(sp.getUUID()))
            );

            if (success) return InteractionResult.SUCCESS;
        }

        // 处理玩家自身的特性
        processEvent(context, (mq, ctx) ->
                mq.onInteractionOther(ctx.player(), ctx.world(), ctx.hand(),
                        ctx.targetEntity(), ctx.hitResult()),
                q -> sp.setXpWithOwner(sp.getUUID(), q.getInteractionValue() + sp.getXpWithOwner(sp.getUUID()))
        );

        return InteractionResult.PASS;
    }

    // 伤害事件处理
    public static boolean onDamage(LivingEntity entity, DamageSource source, float damage) {
        if (!(entity instanceof INeko neko)) return true;

        processEvent(
                new DamageContext(entity, source, damage),
                (mq, ctx) -> mq.onDamage(neko, ctx.damageSource(), ctx.damageValue()),
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

        for (Quirk q : sp.getQuirks()) {
            if (q instanceof Quirk mq) {
                InteractionResult result = mq.onNekoAttack(sp, level, hand, le, hitResult);
                if (result == InteractionResult.SUCCESS) {
                    sp.setXpWithOwner(sp.getUUID(), mq.getInteractionValue() + sp.getXpWithOwner(sp.getUUID()));
                }
            }
        }

        return InteractionResult.PASS;
    }
}
