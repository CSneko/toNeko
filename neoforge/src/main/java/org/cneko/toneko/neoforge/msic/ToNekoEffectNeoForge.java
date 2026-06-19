package org.cneko.toneko.neoforge.msic;

import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.cneko.toneko.common.mod.effects.BewitchedEffect;
import org.cneko.toneko.common.mod.effects.ExcitingEffect;
import org.cneko.toneko.common.mod.effects.HissIntimidationEffect;

import static org.cneko.toneko.common.mod.effects.ToNekoEffects.BEWITCHED_EFFECT;
import static org.cneko.toneko.common.mod.effects.ToNekoEffects.HISS_INTIMIDATION_EFFECT;
import static org.cneko.toneko.common.mod.effects.ToNekoEffects.NEKO_EFFECT;
import static org.cneko.toneko.neoforge.ToNekoNeoForge.MOB_EFFECTS;
public class ToNekoEffectNeoForge {
    public static DeferredHolder<MobEffect, ExcitingEffect> NEKO_EFFECT_HOLDER;
    public static DeferredHolder<MobEffect, BewitchedEffect> BEWITCHED_EFFECT_HOLDER;
    public static DeferredHolder<MobEffect, HissIntimidationEffect> HISS_INTIMIDATION_EFFECT_HOLDER;
    public static void init() {
        NEKO_EFFECT_HOLDER = MOB_EFFECTS.register("exciting", ExcitingEffect::new);
        BEWITCHED_EFFECT_HOLDER = MOB_EFFECTS.register("bewitched", BewitchedEffect::new);
        HISS_INTIMIDATION_EFFECT_HOLDER = MOB_EFFECTS.register("hiss_intimidation", HissIntimidationEffect::new);
    }
    public static void reg() {
        NEKO_EFFECT = NEKO_EFFECT_HOLDER.get();
        BEWITCHED_EFFECT = BEWITCHED_EFFECT_HOLDER.get();
        HISS_INTIMIDATION_EFFECT = HISS_INTIMIDATION_EFFECT_HOLDER.get();
    }
}
