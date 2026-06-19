package org.cneko.toneko.fabric.msic;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import org.cneko.toneko.common.mod.effects.BewitchedEffect;
import org.cneko.toneko.common.mod.effects.ExcitingEffect;
import org.cneko.toneko.common.mod.effects.HissIntimidationEffect;

import static org.cneko.toneko.common.mod.effects.ToNekoEffects.*;
public class ToNekoEffectFabric {

    public static void init(){
        NEKO_EFFECT = Registry.register(BuiltInRegistries.MOB_EFFECT, ExcitingEffect.LOCATION, new ExcitingEffect());
        BEWITCHED_EFFECT = Registry.register(BuiltInRegistries.MOB_EFFECT, BewitchedEffect.LOCATION, new BewitchedEffect());
        HISS_INTIMIDATION_EFFECT = Registry.register(BuiltInRegistries.MOB_EFFECT, HissIntimidationEffect.LOCATION, new HissIntimidationEffect());
    }
}
