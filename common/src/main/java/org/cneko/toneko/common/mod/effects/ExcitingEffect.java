package org.cneko.toneko.common.mod.effects;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.cneko.toneko.common.mod.misc.ToNekoAttributes;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.common.Bootstrap.MODID;

/**
 * 兴奋：猫薄荷 / 猫薄荷水带来的强化效果。
 *
 * <p>按等级缩放（{@link MobEffect} 的属性表是固定值，这里重写 add/remove 手动按 amplifier 计算）：
 * <ul>
 *   <li>兴奋 I：移速 / 攻速 / 跳跃 / 攻击 +30%，猫娘度 +0.05；</li>
 *   <li>兴奋 II：以上全部 +45%，猫娘度 +0.15。</li>
 * </ul>
 */
public class ExcitingEffect extends MobEffect {
    public static final String ID = "exciting";
    public static final Identifier LOCATION = Identifier.fromNamespaceAndPath(MODID, ID);

    private static final float BONUS_I = 0.30f;
    private static final float BONUS_II = 0.45f;
    private static final float DEGREE_I = 0.05f;
    private static final float DEGREE_II = 0.15f;

    public ExcitingEffect() {
        super(MobEffectCategory.NEUTRAL, 0xFF00FF);
    }

    /** 该等级对应的属性加成比例（I = 30%，II 及以上 = 45%）。 */
    private static float bonus(int amplifier) {
        return amplifier >= 1 ? BONUS_II : BONUS_I;
    }

    /** 该等级对应的猫娘度加成（I = 0.05，II 及以上 = 0.15）。 */
    private static float degreeBonus(int amplifier) {
        return amplifier >= 1 ? DEGREE_II : DEGREE_I;
    }

    @Override
    public void addAttributeModifiers(@NotNull AttributeMap attributes, int amplifier) {
        float bonus = bonus(amplifier);
        add(attributes, Attributes.MOVEMENT_SPEED, bonus);
        add(attributes, Attributes.ATTACK_SPEED, bonus);
        add(attributes, Attributes.JUMP_STRENGTH, bonus);
        add(attributes, Attributes.ATTACK_DAMAGE, bonus);
        add(attributes, ToNekoAttributes.NEKO_DEGREE, degreeBonus(amplifier));
    }

    @Override
    public void removeAttributeModifiers(@NotNull AttributeMap attributes) {
        remove(attributes, Attributes.MOVEMENT_SPEED);
        remove(attributes, Attributes.ATTACK_SPEED);
        remove(attributes, Attributes.JUMP_STRENGTH);
        remove(attributes, Attributes.ATTACK_DAMAGE);
        remove(attributes, ToNekoAttributes.NEKO_DEGREE);
    }

    private static void add(AttributeMap attributes, Holder<Attribute> attribute, float amount) {
        AttributeInstance instance = attributes.getInstance(attribute);
        if (instance != null) {
            instance.addOrUpdateTransientModifier(
                    new AttributeModifier(LOCATION, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    private static void remove(AttributeMap attributes, Holder<Attribute> attribute) {
        AttributeInstance instance = attributes.getInstance(attribute);
        if (instance != null) {
            instance.removeModifier(LOCATION);
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(@NotNull net.minecraft.server.level.ServerLevel serverLevel, @NotNull LivingEntity entity, int amplifier) {
        return super.applyEffectTick(serverLevel, entity, amplifier);
    }
}
