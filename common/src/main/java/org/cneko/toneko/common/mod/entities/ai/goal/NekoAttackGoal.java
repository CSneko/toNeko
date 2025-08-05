package org.cneko.toneko.common.mod.entities.ai.goal;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.cneko.toneko.common.mod.entities.FightingNekoEntity;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.items.BazookaItem;

import java.util.EnumSet;
import java.util.List;

public class NekoAttackGoal extends Goal {
    protected final NekoEntity neko;
    @Getter @Setter
    protected LivingEntity target;
    private final TargetingConditions targetConditions;
    private static final double RANGED_ATTACK_RANGE = 15.0; // 远程武器使用距离
    private static final double MELEE_ATTACK_RANGE = 4.0;   // 近战武器使用距离
    private static final double TARGETING_RANGE = 100.0;
    private static final double RANGED_MAINTAIN_DISTANCE = 10.0; // 远程战斗理想距离
    protected int attackCooldown;
    protected int seeTime; // 记录看到目标的时间
    protected int attackInterval = 20; // 基础攻击间隔

    public NekoAttackGoal(NekoEntity neko) {
        this.neko = neko;
        this.targetConditions = TargetingConditions.forCombat()
                .range(100.0) // 目标选择范围
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

        // 判断是否有武器
        boolean hasWeapon = hasUsableRangedWeapon() || hasMeleeWeapon();

        if (hasWeapon) {
            // 有武器时主动攻击怪物
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
        } else {
            // 没有武器时不主动攻击怪物，只攻击仇恨目标
            this.target = null;
            return false;
        }
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
        neko.getNavigation().moveTo(target, neko.getAttributeValue(Attributes.MOVEMENT_SPEED)*1.2); // 提高移动速度
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

        // ========== 武器策略逻辑 ==========
        CombatStrategy strategy = determineCombatStrategy();

        // 逃跑策略优先处理
        if (strategy == CombatStrategy.FLEE) {
            fleeFromTarget();
            return; // 逃跑时不执行后续攻击逻辑
        }

        // 根据策略切换武器
        if (strategy == CombatStrategy.RANGED) {
            switchToRangedWeapon();
        } else if (strategy == CombatStrategy.MELEE) {
            switchToMeleeWeapon();
        }

        // ========== 移动逻辑 ==========
        if (canSee) {
            if (strategy == CombatStrategy.RANGED) {
                // 远程策略：仅在距离过近时后退
                if (distanceSqr > RANGED_ATTACK_RANGE * RANGED_ATTACK_RANGE) {
                    // 距离过远，向目标移动
                    neko.getNavigation().moveTo(target, neko.getAttributeValue(Attributes.MOVEMENT_SPEED)*1.2);
                } else {
                    // 计算当前距离与保持距离的比例
                    double currentDistance = Math.sqrt(distanceSqr);
                    if (currentDistance < RANGED_MAINTAIN_DISTANCE * 0.5) {
                        // 目标距离小于保持距离的50%，才后退
                        moveAwayFromTarget(2.0);
                    } else {
                        // 在理想距离内，停止移动
                        neko.getNavigation().stop();
                    }
                }
            } else { // 近战策略
                if (distanceSqr > MELEE_ATTACK_RANGE * MELEE_ATTACK_RANGE) {
                    neko.getNavigation().moveTo(target, neko.getAttributeValue(Attributes.MOVEMENT_SPEED)*1.2);
                } else {
                    neko.getNavigation().stop();
                }
            }
        } else {
            // 目标不可见时的移动策略
            if (distanceSqr > MELEE_ATTACK_RANGE * MELEE_ATTACK_RANGE) {
                neko.getNavigation().moveTo(target, neko.getAttributeValue(Attributes.MOVEMENT_SPEED)*1.1);
            }
        }
        // 移动逻辑结束

        // ========== 攻击逻辑 ==========
        attackCooldown = Math.max(attackCooldown - 1, 0);
        if (attackCooldown == 0 && canSee) {
            if (strategy == CombatStrategy.MELEE && distanceSqr <= MELEE_ATTACK_RANGE * MELEE_ATTACK_RANGE) {
                performMeleeAttack();
                attackCooldown = attackInterval;
            } else if (strategy == CombatStrategy.RANGED && distanceSqr <= RANGED_ATTACK_RANGE * RANGED_ATTACK_RANGE) {
                performRangedAttack();
                attackCooldown = attackInterval * 2;
            }
        }
    }

    protected CombatStrategy determineCombatStrategy() {
        boolean hasRanged = hasUsableRangedWeapon();
        boolean hasMelee = hasMeleeWeapon();
        float selfHealthRatio = getHealthRatio(neko);
        float targetHealthRatio = getHealthRatio(target);

        // 血量低于(12-护甲值)时必定逃跑
        double armorValue = neko.getArmorValue();
        double fleeThreshold = 12.0 - armorValue;
        if (neko.getHealth() < fleeThreshold) {
            return CombatStrategy.FLEE;
        }

        // 规则1: 只有远程武器且有子弹
        if (hasRanged && !hasMelee) {
            return CombatStrategy.RANGED;
        }

        // 规则2: 只有近战武器
        if (!hasRanged && hasMelee) {
            return CombatStrategy.MELEE;
        }

        // 规则3: 都没有武器
        if (!hasRanged) {
            // 对方生命值百分比 <= 自己时攻击，否则逃跑
            return (targetHealthRatio <= selfHealthRatio) ?
                    CombatStrategy.MELEE : CombatStrategy.FLEE;
        }

        // 规则4: 两种武器都有
        // 对方生命值百分比 > 自己时用远程，否则用近战
        return (targetHealthRatio > selfHealthRatio) ?
                CombatStrategy.RANGED : CombatStrategy.MELEE;

    }

    protected float getHealthRatio(LivingEntity entity) {
        return entity.getHealth() / entity.getMaxHealth();
    }

    protected boolean hasUsableRangedWeapon() {
        // 检查是否有火箭筒
        boolean hasBazooka = false;
        ItemStack ba = ItemStack.EMPTY;
        for (int i = 0; i < neko.getInventory().getContainerSize(); i++) {
            ItemStack stack = neko.getInventory().getItem(i);
            if (stack.getItem() instanceof BazookaItem) {
                hasBazooka = true;
                ba = stack;
                break;
            }
        }
        // 有火箭筒且至少有一个弹药
        return hasBazooka && !findHarmfulAmmo(ba).isEmpty();
    }

    protected boolean hasMeleeWeapon() {
        for (int i = 0; i < neko.getInventory().getContainerSize(); i++) {
            ItemStack stack = neko.getInventory().getItem(i);
            if (stack.is(FightingNekoEntity.MELEE_WEAPON) &&
                    !(stack.getItem() instanceof BazookaItem)) {
                return true;
            }
        }
        return false;
    }

    protected void fleeFromTarget() {
        // 计算远离目标的移动方向
        double dx = neko.getX() - target.getX();
        double dz = neko.getZ() - target.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance > 0) {
            // 计算单位向量并放大
            dx /= distance;
            dz /= distance;

            // 设置逃跑距离（10格）
            double fleeDistance = 10.0;
            double fleeX = neko.getX() + dx * fleeDistance;
            double fleeZ = neko.getZ() + dz * fleeDistance;

            // 移动到逃跑位置
            neko.getNavigation().moveTo(fleeX, neko.getY(), fleeZ, neko.getAttributeValue(Attributes.MOVEMENT_SPEED)*1.1);
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
    private void moveAwayFromTarget(double distance) {
        double dx = neko.getX() - target.getX();
        double dz = neko.getZ() - target.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);

        if (len > 0) {
            dx /= len;
            dz /= len;

            double backX = neko.getX() + dx * distance;
            double backZ = neko.getZ() + dz * distance;

            neko.getNavigation().moveTo(backX, neko.getY(), backZ, neko.getAttributeValue(Attributes.MOVEMENT_SPEED)*1.1);
        }
    }

    protected void switchToRangedWeapon() {
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

    protected void switchToMeleeWeapon() {
        // 检查当前是否已使用近战武器
        if (isUsingMeleeWeapon()) {
            return;
        }

        // 遍历背包寻找近战武器
        int i =0;
        for (ItemStack stack : neko.getInventory().items) {
            i++;
            if (stack.is(FightingNekoEntity.MELEE_WEAPON) && !(stack.getItem() instanceof BazookaItem)) {
                // 强制更新手持物品
                ItemStack oldStack = neko.getItemInHand();
                neko.setItemSlot(EquipmentSlot.MAINHAND, stack);
                neko.getInventory().setItem(i,oldStack);
                return;
            }
        }
    }

    // 检查是否使用近战武器
    private boolean isUsingMeleeWeapon() {
        ItemStack held = neko.getInventory().getItem(neko.getInventory().selected);
        return held.is(FightingNekoEntity.MELEE_WEAPON) && !(held.getItem() instanceof BazookaItem);
    }

    // 检查当前是否使用远程武器
    private boolean isUsingRangedWeapon() {
        ItemStack held = neko.getInventory().getSelected();
        return held.getItem() instanceof BazookaItem;
    }

    // 执行近战攻击
    protected void performMeleeAttack() {
        neko.doHurtTarget(target);
    }

    // 执行远程攻击
    protected void performRangedAttack() {
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

            ItemStack ammo = findHarmfulAmmo(bazooka);
            if (!ammo.isEmpty()) {
                ((BazookaItem) bazooka.getItem()).fire(neko, bazooka, ammo);
            }
        }
    }

    protected void reloadIfNeeded(ItemStack bazooka) {
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
    protected ItemStack findHarmfulAmmo(ItemStack bazooka) {
        for (int i = 0; i < neko.getInventory().getContainerSize(); i++) {
            ItemStack stack = neko.getInventory().getItem(i);
            if (stack.getItem() instanceof BazookaItem.Ammunition item && item.isHarmful(bazooka,stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public enum CombatStrategy {
        RANGED,
        MELEE,
        FLEE
    }
}
