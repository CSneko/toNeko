package org.cneko.toneko.common.mod.effects;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class BewitchedEffect extends MobEffect {
    public static final String ID = "bewitched";
    public static final ResourceLocation LOCATION = ResourceLocation.fromNamespaceAndPath(MODID, ID);

    public BewitchedEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFB6C1);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED,LOCATION,-0.03, AttributeModifier.Operation.ADD_VALUE);
        this.addAttributeModifier(Attributes.ATTACK_SPEED,LOCATION,-0.1, AttributeModifier.Operation.ADD_VALUE);
        this.addAttributeModifier(Attributes.JUMP_STRENGTH,LOCATION,-0.1, AttributeModifier.Operation.ADD_VALUE);
        this.addAttributeModifier(Attributes.ATTACK_DAMAGE,LOCATION,-0.5, AttributeModifier.Operation.ADD_VALUE);
        this.addAttributeModifier(Attributes.ENTITY_INTERACTION_RANGE, LOCATION, -1.0, AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        // 10%的概率冒出一颗爱心
        if (entity.level().getRandom().nextFloat() < 0.1f) {
            entity.level().addParticle(
                ParticleTypes.HEART,
                entity.getX() + (entity.getRandom().nextDouble() - 0.5) * entity.getBbWidth(),
                entity.getY() + entity.getBbHeight() / 2,
                entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * entity.getBbWidth(),
                0, 0, 0
            );
        }
        return super.applyEffectTick(entity, amplifier);
    }
}
