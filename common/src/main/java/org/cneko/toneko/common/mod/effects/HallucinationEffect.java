package org.cneko.toneko.common.mod.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;

public class HallucinationEffect extends MobEffect {
    public static final Identifier LOCATION = Identifier.fromNamespaceAndPath("toneko", "hallucination");

    public HallucinationEffect() {
        super(MobEffectCategory.HARMFUL, 0xAD86D6);
        // Mobs also experience disorientation; players additionally see client-only illusions.
        addAttributeModifier(Attributes.MOVEMENT_SPEED, LOCATION, -0.15,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.FOLLOW_RANGE, LOCATION, -0.35,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    public static float projectileInaccuracy(LivingEntity shooter, float original) {
        if (ToNekoEffects.HALLUCINATION == null) return original;
        var effect = shooter.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ToNekoEffects.HALLUCINATION));
        return effect == null ? original : original + 8f * (effect.getAmplifier() + 1);
    }
}
