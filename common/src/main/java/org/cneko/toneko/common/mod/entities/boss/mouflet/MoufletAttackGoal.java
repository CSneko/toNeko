package org.cneko.toneko.common.mod.entities.boss.mouflet;

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
        return super.canUse();
    }

    @Override
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
}
