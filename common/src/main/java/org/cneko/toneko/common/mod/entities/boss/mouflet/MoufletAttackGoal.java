package org.cneko.toneko.common.mod.entities.boss.mouflet;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import org.cneko.toneko.common.mod.entities.ai.goal.NekoAttackGoal;

public class MoufletAttackGoal extends NekoAttackGoal {
    public MoufletAttackGoal(MoufletNekoBoss neko) {
        super(neko);
    }
    protected MoufletNekoBoss neko = (MoufletNekoBoss) super.neko;

    @Override
    public boolean canUse() {
        if (neko.isPetMode() && target != null && neko.hasOwner(target.getUUID())) {
            // 如果是宠物模式且是主人，则不攻击
            return false;
        }
        //　魅惑技能下不攻击
        if (neko.isCharmed()) {
            return false;
        }
        return super.canUse();
    }

    @Override
    protected CombatStrategy determineCombatStrategy() {
        boolean hasRanged = hasUsableRangedWeapon();
        boolean hasMelee = hasMeleeWeapon();
        float selfHealthRatio = getHealthRatio(neko);
        float targetHealthRatio = getHealthRatio(target);

        // 血量低于20时必定逃跑
        if (neko.getHealth() < 20) {
            return CombatStrategy.FLEE;
        }

        // 只有远程武器且有子弹
        if (hasRanged && !hasMelee) {
            return CombatStrategy.RANGED;
        }

        // 只有近战武器
        if (!hasRanged && hasMelee) {
            return CombatStrategy.MELEE;
        }

        // 都没有武器
        if (!hasRanged) {
            // 对方生命值百分比-30% <= 自己时攻击，否则逃跑
            return (targetHealthRatio-0.3 <= selfHealthRatio) ?
                    CombatStrategy.MELEE : CombatStrategy.FLEE;
        }

        // 两种武器都有
        // 对方生命值百分比 > 自己时用远程，否则用近战
        return (targetHealthRatio > selfHealthRatio) ?
                CombatStrategy.RANGED : CombatStrategy.MELEE;
    }

    @Override
    protected void fleeFromTarget() {
        // 能量足够时飞行逃跑
        if (neko.getNekoEnergy() > 0.1f) {
            // 计算远离目标的方向
            double dx = neko.getX() - target.getX();
            double dz = neko.getZ() - target.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            // 判断距离地面的距离是否大于3
            if (neko.getY() - neko.level().getHeightmapPos(Heightmap.Types.WORLD_SURFACE,neko.getOnPos()).getY() < 3) {
                // 如果离地面小于3格，则向上飞行
                neko.setDeltaMovement(dx * 0.3, 0.2, dz * 0.3); // 向上+远离目标
                neko.fallDistance = 0.0f;
                neko.setNekoEnergy(neko.getNekoEnergy() - 0.1f);
                return;
            }
            if (distance > 0) {
                dx /= distance;
                dz /= distance;
                // 飞行逃跑
                neko.setDeltaMovement(dx * 0.7, 0, dz * 0.7); // 向远离目标
                neko.fallDistance = 0.0f;
                neko.setNekoEnergy(neko.getNekoEnergy() - 0.1f);
                // 添加缓降效果
                neko.addEffect(new MobEffectInstance(
                        MobEffects.SLOW_FALLING, 40, 0, true, false));

            }
        } else {
            // 能量不足时普通逃跑
            super.fleeFromTarget();
        }
    }

    @Override
    protected void performRangedAttack() {
        // 强制面向目标
        double dx = target.getX() - neko.getX();
        double dz = target.getZ() - neko.getZ();
        float targetYaw = (float)(Math.atan2(dz, dx) * (180 / Math.PI)) - 90.0F;

        double dy = target.getY() - neko.getY();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        float targetPitch = (float)(-Math.atan2(dy, horizontalDistance) * (180 / Math.PI));

        neko.setYRot(targetYaw);
        neko.setXRot(targetPitch);
        neko.yHeadRot = targetYaw;
        neko.yBodyRot = targetYaw;

        ItemStack bazooka = neko.getInventory().getSelected();
        if (bazooka.getItem() instanceof org.cneko.toneko.common.mod.items.BazookaItem) {
            reloadIfNeeded(bazooka);

            ItemStack ammo = findAmmo();
            if (!ammo.isEmpty()) {
                // 创建弹药实体
                org.cneko.toneko.common.mod.entities.AmmunitionEntity projectile =
                        new org.cneko.toneko.common.mod.entities.AmmunitionEntity(
                                org.cneko.toneko.common.mod.entities.ToNekoEntities.AMMUNITION_ENTITY,
                                neko.level()
                        );
                projectile.setBazookaStack(bazooka);
                projectile.setAmmunitionStack(ammo);
                projectile.setOwner(neko);
                projectile.setHomingTarget(target); // 设置追踪目标

                // 设置初始位置和方向
                net.minecraft.world.phys.Vec3 spawnPos = neko.position()
                        .add(neko.getLookAngle().scale(0.5))
                        .add(0, neko.getEyeHeight(), 0);
                projectile.setPos(spawnPos);

                net.minecraft.world.phys.Vec3 lookAngle = neko.getLookAngle().normalize();
                float speed = 2.0f; // 可根据弹药类型调整
                projectile.shootWithInitialPos(lookAngle.x, lookAngle.y, lookAngle.z, speed, 0.0f);

                neko.level().addFreshEntity(projectile);

                // 消耗弹药
                ammo.shrink(1);
            }
        }
    }
}
