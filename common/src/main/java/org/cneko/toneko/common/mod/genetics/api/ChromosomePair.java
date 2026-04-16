package org.cneko.toneko.common.mod.genetics.api;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map; /**
 * 同源染色体对
 */
public class ChromosomePair {
    public Map<ResourceLocation, ResourceLocation> strandA = new HashMap<>(); // 父源
    public Map<ResourceLocation, ResourceLocation> strandB = new HashMap<>(); // 母源
}
