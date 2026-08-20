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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.entities.boss.mouflet.MoufletNekoBoss;
import org.cneko.toneko.common.mod.misc.ScentedWaterUtil;
import org.cneko.toneko.common.mod.misc.ToNekoComponents;
import org.cneko.toneko.common.mod.quirks.Quirk;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Consumer;

public class CommonPlayerInteractionEvent {

    public static InteractionResult useBlock(Player player, Level level, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        if (level.getEntitiesOfClass(MoufletNekoBoss.class, player.getBoundingBox().inflate(32)).stream()
                .anyMatch(neko -> !neko.isPetMode())) {
            var block = level.getBlockEntity(blockHitResult.getBlockPos());
            if (block instanceof Container) {
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }

    private sealed interface EventContext permits DamageContext, AttackContext, InteractionContext {
        LivingEntity entity();
    }

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

    private record DamageContext(
            LivingEntity entity,
            DamageSource damageSource,
            float damageValue
    ) implements EventContext {}

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

    private static <T extends EventContext> InteractionResult processInteractEvent(
            T context,
            BiFunction<Quirk, T, InteractionResult> eventAction,
            @Nullable Consumer<Quirk> xpHandler
    ) {
        INeko neko = null;
        if (context.entity() instanceof INeko n) {
            neko = n;
        }
        if (neko == null) return InteractionResult.PASS;

        for (Quirk q : neko.getQuirks()) {
            if (q != null) {
                // 捕获 Quirk 执行的结果
                InteractionResult result = eventAction.apply(q, context);

                // 如果 Quirk 返回了 SUCCESS 或 CONSUME (消耗了此次操作)
                if (result.consumesAction()) {
                    if (xpHandler != null && result == InteractionResult.SUCCESS) {
                        xpHandler.accept(q);
                    }
                    // 立即拦截并返回给游戏引擎，阻止副手触发或后续冲突
                    return result;
                }
            }
        }
        return InteractionResult.PASS;
    }

    public static InteractionResult useEntity(Player player, Level world, InteractionHand hand,
                                              Entity entity, EntityHitResult hitResult) {
        if (!(entity instanceof INeko targetNeko) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }

        InteractionResult waterResult = handleScentedWaterOnNeko(sp, world, hand, targetNeko);
        if (waterResult.consumesAction()) return waterResult;

        InteractionContext context = new InteractionContext(sp, world, hand, targetNeko, hitResult);

        // 【修改点】：创建一个统一的 XP 处理器，在这里判断是否有主人
        Consumer<Quirk> xpHandler = q -> {
            if (targetNeko.hasOwner(sp.getUUID())) {
                sp.setXpWithOwner(sp.getUUID(), q.getInteractionValue() + sp.getXpWithOwner(sp.getUUID()));
            }
        };

        // 处理目标 neko 的特性 (无条件触发，但传进去的 xpHandler 会做主人判断)
        InteractionResult targetResult = processInteractEvent(context,
                (mq, ctx) -> mq.onNekoInteraction(ctx.player(), ctx.world(), ctx.hand(), ctx.targetEntity(), ctx.hitResult()),
                xpHandler
        );

        if (targetResult.consumesAction()) return targetResult;

        // 处理玩家自身的特性 (无条件触发，但传进去的 xpHandler 会做主人判断)
        InteractionResult playerResult = processInteractEvent(context,
                (mq, ctx) -> mq.onInteractionOther(ctx.player(), ctx.world(), ctx.hand(), ctx.targetEntity(), ctx.hitResult()),
                xpHandler
        );

        if (playerResult.consumesAction()) return playerResult;

        return InteractionResult.PASS;
    }

    /** 把变质水/香水给猫娘闻：主人味加好感，别人味会吃醋；无来源则只是好奇。 */
    private static InteractionResult handleScentedWaterOnNeko(ServerPlayer player, Level level,
                                                              InteractionHand hand, INeko targetNeko) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.has(ToNekoComponents.SPOILED_WATER_SPOILAGE_COMPONENT)
                || !stack.has(ToNekoComponents.SPOILED_WATER_WEARER_COMPONENT)) {
            return InteractionResult.PASS;
        }
        int spoilage = ScentedWaterUtil.getSpoilage(stack);
        if (spoilage <= 0) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        String wearer = ScentedWaterUtil.getWearer(stack);
        String playerName = player.getName().getString();
        boolean ownerScent = !wearer.isEmpty() && wearer.equals(playerName);

        if (targetNeko instanceof NekoEntity neko) {
            boolean owned = neko.hasOwner(player.getUUID());
            if (owned && ownerScent) {
                int xp = neko.getXpWithOwner(player.getUUID());
                neko.setXpWithOwner(player.getUUID(), Math.max(0, xp + 2));
            }
        }

        stack.shrink(1);
        level.playSound(null, player.blockPosition(), SoundEvents.CAT_STRAY_AMBIENT,
                SoundSource.PLAYERS, 1.0f, 1.2f);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART,
                    targetNeko.getEntity().getX(), targetNeko.getEntity().getY() + 1.2,
                    targetNeko.getEntity().getZ(), 7, 0.3, 0.2, 0.3, 0.05);
        }

        Component msg;
        if (ownerScent) {
            msg = Component.translatable("item.toneko.spoiled_water.neko.owner_scent", wearer);
        } else if (!wearer.isEmpty()) {
            msg = Component.translatable("item.toneko.spoiled_water.neko.other_scent", wearer);
        } else {
            msg = Component.translatable("item.toneko.spoiled_water.neko.unknown_scent");
        }
        player.displayClientMessage(msg, true);
        return InteractionResult.SUCCESS;
    }

    // 伤害事件由于原版要求返回 boolean，单独处理循环
    public static boolean onDamage(LivingEntity entity, DamageSource source, float damage) {
        if (!(entity instanceof INeko neko)) return true;

        for (Quirk q : neko.getQuirks()) {
            if (q != null) {
                q.onDamage(neko, source, damage);
            }
        }
        return true;
    }

    public static InteractionResult onAttackEntity(Player attacker, Level level, InteractionHand hand,
                                                   Entity target, EntityHitResult hitResult) {
        if (!(attacker instanceof ServerPlayer sp) || !(target instanceof LivingEntity le)) {
            return InteractionResult.PASS;
        }

        // 猫娘潜行：攻击后暂时解除潜行
        if (sp.isNeko()) {
            sp.breakStealth();
        }

        for (Quirk q : sp.getQuirks()) {
            if (q != null) {
                InteractionResult result = q.onNekoAttack(sp, level, hand, le, hitResult);
                if (result == InteractionResult.SUCCESS) {
                    sp.setXpWithOwner(sp.getUUID(), q.getInteractionValue() + sp.getXpWithOwner(sp.getUUID()));
                }
            }
        }
        return InteractionResult.PASS;
    }
}