package org.cneko.toneko.common.mod.misc;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

public class ToNekoDamageTypes {
    public static final ResourceKey<DamageType> BAZOOKA_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,toNekoLoc("bazooka"));
    public static final ResourceKey<DamageType> NEKO_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,toNekoLoc("neko"));

    public static DamageSource bazookaDamage(LivingEntity source){
        return new DamageSource(source.level().registryAccess().lookup(Registries.DAMAGE_TYPE).flatMap(lookup -> lookup.get(ToNekoDamageTypes.BAZOOKA_DAMAGE)).get());
    }
    public static DamageSource nekoDamage(LivingEntity source){
        return new DamageSource(source.level().registryAccess().lookup(Registries.DAMAGE_TYPE).flatMap(lookup -> lookup.get(ToNekoDamageTypes.NEKO_DAMAGE)).get());
    }
}
