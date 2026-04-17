package org.cneko.toneko.common.mod.genetics;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.cneko.toneko.common.mod.genetics.api.Allele;
import org.cneko.toneko.common.mod.genetics.api.GeneticsRegistry;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;
import static org.cneko.toneko.common.mod.genetics.ToNekoLocus.*;

public class ToNekoAlleles {
    // 全局通用的野生型占位基因。不修改任何属性，权重默认为 10 (通常作为隐性)
    public static final Allele WILD_TYPE = new Allele(toNekoLoc("wild_type"), 10, null, null);
    // 慢速基因，减0.05移速
    public static final Allele SLOW_SPEED = new Allele(toNekoLoc("slow_speed"), 20, null, null)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED,"speed_boost",-0.05, AttributeModifier.Operation.ADD_VALUE);
    // 超速基因，加0.05移速
    public static final Allele SUPER_SPEED = new Allele(toNekoLoc("super_speed"), 20, null, null)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED,"speed_boost",0.05, AttributeModifier.Operation.ADD_VALUE);
    // 大力基因，加3攻击力
    public static final Allele STRONG_ATTACK = new Allele(toNekoLoc("strong_attack"), 20, null, null)
            .addAttributeModifier(Attributes.ATTACK_DAMAGE,"attack_boost",3, AttributeModifier.Operation.ADD_VALUE);
    // 敏捷基因，加3攻击速度
    public static final Allele FAST_ATTACK = new Allele(toNekoLoc("fast_attack"), 20, null, null)
            .addAttributeModifier(Attributes.ATTACK_SPEED,"attack_boost",3, AttributeModifier.Operation.ADD_VALUE);
    // 大体型基因，+0.3体型
    public static final Allele LARGE_BODY = new Allele(toNekoLoc("large_body"), 20, null, null)
            .addAttributeModifier(Attributes.SCALE,"body_size_boost",0.3, AttributeModifier.Operation.ADD_VALUE);
    // 小体型基因，-0.3体型
    public static final Allele SMALL_BODY = new Allele(toNekoLoc("small_body"), 20, null, null)
            .addAttributeModifier(Attributes.SCALE,"body_size_boost",-0.3, AttributeModifier.Operation.ADD_VALUE);
    // 健康基因，增加5最大生命
    public static final Allele HEALTHY = new Allele(toNekoLoc("healthy"), 20, null, null)
            .addAttributeModifier(Attributes.MAX_HEALTH,"health_boost",5, AttributeModifier.Operation.ADD_VALUE);
    // 虚弱基因，减少5最大生命
    public static final Allele WEAK = new Allele(toNekoLoc("weak"), 20, null, null)
            .addAttributeModifier(Attributes.MAX_HEALTH,"health_boost",-5, AttributeModifier.Operation.ADD_VALUE);

    public static void init(){
        GeneticsRegistry.registerAllele(WILD_TYPE);
        GeneticsRegistry.registerAllele(SLOW_SPEED);
        GeneticsRegistry.registerAllele(SUPER_SPEED);
        GeneticsRegistry.registerAllele(STRONG_ATTACK);
        GeneticsRegistry.registerAllele(FAST_ATTACK);
        GeneticsRegistry.registerAllele(LARGE_BODY);
        GeneticsRegistry.registerAllele(SMALL_BODY);
        GeneticsRegistry.registerAllele(HEALTHY);
        GeneticsRegistry.registerAllele(WEAK);


        // 速度槽位 0
        GeneticsRegistry.addWildAllele(SPEED_SLOT_0.id(), WILD_TYPE.getId(), 70); // 70% 是普通猫娘
        GeneticsRegistry.addWildAllele(SPEED_SLOT_0.id(), SLOW_SPEED.getId(), 20);
        GeneticsRegistry.addWildAllele(SPEED_SLOT_0.id(), SUPER_SPEED.getId(), 10);
        // 速度槽位 1
        GeneticsRegistry.addWildAllele(SPEED_SLOT_1.id(), WILD_TYPE.getId(), 60); // 60% 是普通猫娘
        GeneticsRegistry.addWildAllele(SPEED_SLOT_1.id(), SLOW_SPEED.getId(), 15);
        GeneticsRegistry.addWildAllele(SPEED_SLOT_1.id(), SUPER_SPEED.getId(), 25);
        // 攻击槽位 0
        GeneticsRegistry.addWildAllele(ATTACK_SLOT_0.id(), WILD_TYPE.getId(), 75); // 75% 是普通猫娘
        GeneticsRegistry.addWildAllele(ATTACK_SLOT_0.id(), STRONG_ATTACK.getId(), 25);
        // 敏捷槽位 0
        GeneticsRegistry.addWildAllele(AGILITY_SLOT_0.id(), WILD_TYPE.getId(), 80); // 80% 是普通猫娘
        GeneticsRegistry.addWildAllele(AGILITY_SLOT_0.id(), FAST_ATTACK.getId(), 20);
        // 体型槽位 0
        GeneticsRegistry.addWildAllele(BODY_SIZE_SLOT_0.id(), WILD_TYPE.getId(), 50); // 50% 是普通猫娘
        GeneticsRegistry.addWildAllele(BODY_SIZE_SLOT_0.id(), LARGE_BODY.getId(), 30);
        GeneticsRegistry.addWildAllele(BODY_SIZE_SLOT_0.id(), SMALL_BODY.getId(), 20);
        // 生命槽位 0
        GeneticsRegistry.addWildAllele(HEALTH_SLOT_0.id(), WILD_TYPE.getId(), 65); // 65% 是普通猫娘
        GeneticsRegistry.addWildAllele(HEALTH_SLOT_0.id(), HEALTHY.getId(), 25);
        GeneticsRegistry.addWildAllele(HEALTH_SLOT_0.id(), WEAK.getId(), 10);

        MoeGenetics.registerAll();
    }
}
