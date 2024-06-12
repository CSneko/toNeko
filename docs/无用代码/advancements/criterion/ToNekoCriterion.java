package org.cneko.toneko.fabric.advancements.criterion;

import net.minecraft.advancement.criterion.Criteria;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoCriterion {
    public static final BecomeByPotionCriterion BECOME_BY_POTION = Criteria.register(MODID+"/become_by_potion",new BecomeByPotionCriterion());
}
