package org.cneko.toneko.common.mod.genetics.api;

import net.minecraft.resources.ResourceLocation;

/**
 * 基因座，定义在染色体上的坑位。
 */
public record Locus(ResourceLocation id, int chromosomeId) {}