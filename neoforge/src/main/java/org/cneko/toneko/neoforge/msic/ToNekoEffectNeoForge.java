package org.cneko.toneko.neoforge.msic;

import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.cneko.toneko.common.mod.effects.ExcitingEffect;

import static org.cneko.toneko.common.mod.effects.ToNekoEffects.NEKO_EFFECT;
import static org.cneko.toneko.neoforge.ToNekoNeoForge.MOB_EFFECTS;
public class ToNekoEffectNeoForge {
    public static DeferredHolder<MobEffect, ExcitingEffect> NEKO_EFFECT_HOLDER;
    public static void init() {
        NEKO_EFFECT_HOLDER = MOB_EFFECTS.register("exciting", ExcitingEffect::new);
    }
    public static void reg() {
        NEKO_EFFECT = NEKO_EFFECT_HOLDER.get();
    }
}
