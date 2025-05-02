package org.cneko.toneko.fabric.msic;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import org.cneko.toneko.common.mod.effects.ExcitingEffect;

import static org.cneko.toneko.common.mod.effects.ToNekoEffects.*;
public class ToNekoEffectFabric {

    public static void init(){
        NEKO_EFFECT = Registry.register(BuiltInRegistries.MOB_EFFECT, ExcitingEffect.LOCATION, new ExcitingEffect());
    }
}
