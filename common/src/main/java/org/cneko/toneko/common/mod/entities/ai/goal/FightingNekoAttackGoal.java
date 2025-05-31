package org.cneko.toneko.common.mod.entities.ai.goal;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.cneko.toneko.common.mod.entities.FightingNekoEntity;
import org.cneko.toneko.common.mod.items.BazookaItem;

import java.util.EnumSet;
import java.util.List;

import static org.cneko.toneko.common.mod.entities.FightingNekoEntity.NEKO_WEAPON;

public class FightingNekoAttackGoal extends Goal {
    private final FightingNekoEntity neko;
    private LivingEntity target;
    private final TargetingConditions targetConditions;
    private static final double RANGED_ATTACK_RANGE = 15.0; // 远程武器使用距离
    private static final double MELEE_ATTACK_RANGE = 3.0;   // 近战武器使用距离
    private static final double TARGETING_RANGE = 30.0;
    private int attackCooldown;
    private int seeTime; // 记录看到目标的时间
    private int attackInterval = 20; // 基础攻击间隔

    public FightingNekoAttackGoal(FightingNekoEntity neko) {
        this.neko = neko;
        this.targetConditions = TargetingConditions.forCombat()
                .range(20.0) // 目标选择范围
                .ignoreLineOfSight();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // 优先选择攻击过自己的目标
        LivingEntity revengeTarget = neko.getLastHurtByMob();
        if (revengeTarget != null && revengeTarget.isAlive() && !revengeTarget.isAlliedTo(neko)) {
            this.target = revengeTarget;
            return true;
        }

        // 查找附近怪物作为目标
        AABB area = neko.getBoundingBox().inflate(TARGETING_RANGE);
        List<Monster> monsters = neko.level().getEntitiesOfClass(
                Monster.class, area, e -> e.isAlive() && !e.isAlliedTo(neko)
        );

        LivingEntity closestVisible = null;
        double closestDistSq = Double.MAX_VALUE;

        for (Monster m : monsters) {
            if (neko.hasLineOfSight(m)) {
                double distSq = neko.distanceToSqr(m);
                if (distSq < closestDistSq) {
                    closestVisible = m;
                    closestDistSq = distSq;
                }
            }
        }
        this.target = closestVisible;
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (target == null || !target.isAlive()) return false;

        // 如果目标太远则放弃
        if (neko.distanceToSqr(target) > TARGETING_RANGE * TARGETING_RANGE) return false;

        // 如果目标不可见但距离较近，继续追击
        return neko.hasLineOfSight(target) || neko.distanceToSqr(target) < MELEE_ATTACK_RANGE * MELEE_ATTACK_RANGE * 4;
    }

    @Override
    public void start() {
        neko.getNavigation().moveTo(target, 1.3); // 提高移动速度
        attackCooldown = 0;
        seeTime = 0;
        neko.setAggressive(true); // 设置为敌对状态
    }

    @Override
    public void stop() {
        target = null;
        neko.getNavigation().stop();
        neko.setAggressive(false); // 取消敌对状态
    }

    @Override
    public void tick() {
        if (target == null || !target.isAlive()) return;

        // 更新朝向
        neko.getLookControl().setLookAt(target, 30.0F, 30.0F);

        double distanceSqr = neko.distanceToSqr(target);
        boolean canSee = neko.hasLineOfSight(target);

        if (canSee) {
            ++this.seeTime;
        } else {
            this.seeTime = 0;
        }

        // 更积极的移动策略
        if (distanceSqr > MELEE_ATTACK_RANGE * MELEE_ATTACK_RANGE) {
            // 如果能看到目标或刚刚看到过，则继续追击
            neko.getNavigation().moveTo(target, neko.getAttributeValue(Attributes.MOVEMENT_SPEED)*1.3);
        }

        // 武器切换逻辑
        if (distanceSqr > RANGED_ATTACK_RANGE * RANGED_ATTACK_RANGE) {
            switchToRangedWeapon();
        } else if (distanceSqr <= MELEE_ATTACK_RANGE * MELEE_ATTACK_RANGE) {
            switchToMeleeWeapon();
        }

        // 攻击逻辑
        attackCooldown = Math.max(attackCooldown - 1, 0);
        if (attackCooldown == 0 && canSee) {
            if (distanceSqr <= MELEE_ATTACK_RANGE * MELEE_ATTACK_RANGE) {
                performMeleeAttack();
                attackCooldown = attackInterval;
            } else if (isUsingRangedWeapon() && distanceSqr <= RANGED_ATTACK_RANGE * RANGED_ATTACK_RANGE) {
                performRangedAttack();
                attackCooldown = attackInterval * 2;
            }
        }
    }

    // 切换到远程武器（火箭筒）
    private void switchToRangedWeapon() {
        for (int i = 0; i < neko.getInventory().getContainerSize(); i++) {
            ItemStack stack = neko.getInventory().getItem(i);
            if (stack.getItem() instanceof BazookaItem) {
                neko.getInventory().selected = i;
                return;
            }
        }
        // 找不到火箭筒时尝试切换回近战武器
        switchToMeleeWeapon();
    }

    // 切换到近战武器
    private void switchToMeleeWeapon() {
        for (int i = 0; i < neko.getInventory().getContainerSize(); i++) {
            ItemStack stack = neko.getInventory().getItem(i);
            if (stack.is(NEKO_WEAPON) && !(stack.getItem() instanceof BazookaItem)) {
                neko.getInventory().selected = i;
                return;
            }
        }
    }

    // 检查当前是否使用远程武器
    private boolean isUsingRangedWeapon() {
        ItemStack held = neko.getInventory().getSelected();
        return held.getItem() instanceof BazookaItem;
    }

    // 执行近战攻击
    private void performMeleeAttack() {
        neko.doHurtTarget(target);
    }

    // 执行远程攻击
    private void performRangedAttack() {
        ItemStack bazooka = neko.getInventory().getSelected();
        if (bazooka.getItem() instanceof BazookaItem) {
            ItemStack ammo = findAmmo();
            if (!ammo.isEmpty()) {
                ((BazookaItem) bazooka.getItem()).fire(neko, bazooka, ammo);
            }
        }
    }

    // 查找弹药
    private ItemStack findAmmo() {
        for (int i = 0; i < neko.getInventory().getContainerSize(); i++) {
            ItemStack stack = neko.getInventory().getItem(i);
            if (stack.getItem() instanceof BazookaItem.Ammunition) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}