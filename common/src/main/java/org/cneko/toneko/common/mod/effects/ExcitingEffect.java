package org.cneko.toneko.common.mod.effects;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.cneko.toneko.common.mod.misc.ToNekoAttributes;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ExcitingEffect extends MobEffect {
    public static final String ID = "exciting";
    public static final ResourceLocation LOCATION = ResourceLocation.fromNamespaceAndPath(MODID, ID);

    protected ExcitingEffect() {
        super(MobEffectCategory.NEUTRAL, 0xFF00FF);
        this.addAttributeModifier(ToNekoAttributes.NEKO_DEGREE,LOCATION,10, AttributeModifier.Operation.ADD_VALUE);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED,LOCATION,0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        this.addAttributeModifier(Attributes.ATTACK_SPEED,LOCATION,0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        this.addAttributeModifier(Attributes.JUMP_STRENGTH,LOCATION,0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        this.addAttributeModifier(Attributes.ATTACK_DAMAGE,LOCATION,0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        return super.applyEffectTick(entity, amplifier);
    }
}
