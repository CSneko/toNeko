package org.cneko.toneko.common.mod.effects;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoEffects {
    public static final MobEffect NEKO_EFFECT = Registry.register(BuiltInRegistries.MOB_EFFECT, ResourceLocation.fromNamespaceAndPath(MODID, "exciting"), new ExcitingEffect());

    public static void init(){}
}
