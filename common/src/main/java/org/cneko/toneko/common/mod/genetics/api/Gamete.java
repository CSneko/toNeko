package org.cneko.toneko.common.mod.genetics.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单倍体配子（精子/卵子）
 */
public class Gamete {
    // 染色体ID -> (基因座ID -> 等位基因ID)
    public final Map<Integer, Map<ResourceLocation, ResourceLocation>> chromosomes = new HashMap<>();
}

