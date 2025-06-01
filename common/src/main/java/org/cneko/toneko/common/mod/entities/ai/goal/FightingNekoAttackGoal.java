package org.cneko.toneko.common.mod.entities.ai.goal;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.cneko.toneko.common.mod.entities.FightingNekoEntity;
import org.cneko.toneko.common.mod.items.BazookaItem;

import java.util.EnumSet;
import java.util.List;

public class FightingNekoAttackGoal extends Goal {
    private final FightingNekoEntity neko;
    private LivingEntity target;
    private final TargetingConditions targetConditions;
    private static final double RANGED_ATTACK_RANGE = 15.0; // 远程武器使用距离
    private static final double MELEE_ATTACK_RANGE = 3.0;   // 近战武器使用距离
    private static final double TARGETING_RANGE = 30.0;
    private static final double RANGED_MAINTAIN_DISTANCE = 10.0; // 远程战斗理想距离
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
        List<LivingEntity> monsters = neko.level().getEntitiesOfClass(
                LivingEntity.class, area,
                e -> e instanceof Enemy && e.isAlive() && !e.isAlliedTo(neko)
        );

        LivingEntity closestVisible = null;
        double closestDistSq = Double.MAX_VALUE;

        for (LivingEntity m : monsters) {
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

        // 检查是否有远程武器
        boolean hasRangedWeapon = hasRangedWeaponInInventory();

        // 修改移动逻辑
        if (canSee) {
            if (hasRangedWeapon) {
                // 有远程武器时的移动策略
                if (distanceSqr > RANGED_ATTACK_RANGE * RANGED_ATTACK_RANGE) {
                    // 距离过远，向目标移动
                    neko.getNavigation().moveTo(target, 1.3);
                } else if (distanceSqr < RANGED_MAINTAIN_DISTANCE * RANGED_MAINTAIN_DISTANCE) {
                    // 距离过近，后退保持距离
                    moveAwayFromTarget();
                } else {
                    // 在理想距离内，停止移动
                    neko.getNavigation().stop();
                }
            } else {
                // 没有远程武器，只能近战
                if (distanceSqr > MELEE_ATTACK_RANGE * MELEE_ATTACK_RANGE) {
                    neko.getNavigation().moveTo(target, 1.3);
                } else {
                    neko.getNavigation().stop();
                }
            }
        } else {
            // 目标不可见时的移动策略
            if (distanceSqr > MELEE_ATTACK_RANGE * MELEE_ATTACK_RANGE) {
                neko.getNavigation().moveTo(target, 1.3);
            }
        }
        // 移动逻辑结束

        // 武器切换逻辑
        if (hasRangedWeapon && distanceSqr > MELEE_ATTACK_RANGE * MELEE_ATTACK_RANGE) {
            switchToRangedWeapon();

            // 切换到火箭筒时自动换弹
            ItemStack held = neko.getInventory().getSelected();
            if (held.getItem() instanceof BazookaItem bazooka) {
                reloadIfNeeded(held);
            }
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

    // 检查背包中是否有远程武器
    private boolean hasRangedWeaponInInventory() {
        for (int i = 0; i < neko.getInventory().getContainerSize(); i++) {
            ItemStack stack = neko.getInventory().getItem(i);
            if (stack.getItem() instanceof BazookaItem) {
                return true;
            }
        }
        return false;
    }

    // 后退保持距离
    private void moveAwayFromTarget() {
        // 计算从目标指向自己的方向向量
        double dx = neko.getX() - target.getX();
        double dz = neko.getZ() - target.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);

        if (len > 0) {
            // 单位化向量
            dx /= len;
            dz /= len;

            // 计算后退位置（当前位置后退2格）
            double backX = neko.getX() + dx * 2;
            double backZ = neko.getZ() + dz * 2;

            // 移动到后退位置
            neko.getNavigation().moveTo(backX, neko.getY(), backZ, 1.3);
        }
    }

    private void switchToRangedWeapon() {
        // 检查当前是否已使用远程武器
        if (isUsingRangedWeapon()) {
            return;
        }

        // 遍历背包寻找火箭筒
        for (int i = 0; i < neko.getInventory().getContainerSize(); i++) {
            ItemStack stack = neko.getInventory().getItem(i);
            if (stack.getItem() instanceof BazookaItem) {
                // 切换选中槽位到远程武器
                neko.getInventory().selected = i;
                // 强制更新手持物品
                neko.setItemSlot(EquipmentSlot.MAINHAND, stack);
                return;
            }
        }
        // 找不到火箭筒时尝试切换回近战武器
        switchToMeleeWeapon();
    }

    private void switchToMeleeWeapon() {
        // 检查当前是否已使用近战武器
        if (isUsingMeleeWeapon()) {
            return;
        }

        // 遍历背包寻找近战武器
        for (int i = 0; i < neko.getInventory().getContainerSize(); i++) {
            ItemStack stack = neko.getInventory().getItem(i);
            if (stack.is(FightingNekoEntity.MELEE_WEAPON) && !(stack.getItem() instanceof BazookaItem)) {
                // 切换选中槽位到近战武器
                neko.getInventory().selected = i;
                // 强制更新手持物品
                neko.setItemSlot(EquipmentSlot.MAINHAND, stack);
                return;
            }
        }
    }

    // 检查是否使用近战武器
    private boolean isUsingMeleeWeapon() {
        ItemStack held = neko.getInventory().getSelected();
        return held.is(FightingNekoEntity.MELEE_WEAPON) && !(held.getItem() instanceof BazookaItem);
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
        // 强制面向目标
        double dx = target.getX() - neko.getX();
        double dz = target.getZ() - neko.getZ();
        float targetYaw = (float)(Math.atan2(dz, dx) * (180 / Math.PI)) - 90.0F;

        // 计算俯仰角（上下角度）
        double dy = target.getY() - neko.getY();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        float targetPitch = (float)(-Math.atan2(dy, horizontalDistance) * (180 / Math.PI));

        // 设置实体旋转
        neko.setYRot(targetYaw);
        neko.setXRot(targetPitch);
        neko.yHeadRot = targetYaw;
        neko.yBodyRot = targetYaw;

        ItemStack bazooka = neko.getInventory().getSelected();
        if (bazooka.getItem() instanceof BazookaItem) {
            // 换弹检查
            reloadIfNeeded(bazooka);

            ItemStack ammo = findAmmo();
            if (!ammo.isEmpty()) {
                ((BazookaItem) bazooka.getItem()).fire(neko, bazooka, ammo);
            }
        }
    }

    private void reloadIfNeeded(ItemStack bazooka) {
        BazookaItem bazookaItem = (BazookaItem) bazooka.getItem();
        BazookaItem.Ammunition currentAmmo = bazookaItem.getAmmunition(bazooka);

        // 检查是否需要换弹（无弹药或当前弹药无害）
        if (currentAmmo == null || !currentAmmo.isHarmful(bazooka, ItemStack.EMPTY)) {
            // 在背包中寻找有害弹药
            for (int i = 0; i < neko.getInventory().getContainerSize(); i++) {
                ItemStack stack = neko.getInventory().getItem(i);
                if (stack.getItem() instanceof BazookaItem.Ammunition ammo &&
                        ammo.isHarmful(bazooka, stack)) {

                    // 设置弹药类型并消耗弹药
                    bazookaItem.setAmmunitionType(stack.getItem(), bazooka);
                    stack.shrink(1);
                    return;
                }
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