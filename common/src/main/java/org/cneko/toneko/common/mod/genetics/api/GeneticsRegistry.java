package org.cneko.toneko.common.mod.genetics.api;

import net.minecraft.resources.ResourceLocation;
import java.util.*;

/**
 * 遗传学注册表，任何模组都可以通过这里添加基因或给实体打坑位。
 */
public class GeneticsRegistry {
    public static final Map<ResourceLocation, Allele> ALLELES = new HashMap<>();
    public static final Map<ResourceLocation, Locus> LOCI = new HashMap<>();
    public static final Map<Integer, List<ResourceLocation>> CHROMOSOME_TEMPLATES = new HashMap<>();

    // 野生基因库： 基因座ID -> 该坑位可能出现的基因及其权重
    public static final Map<ResourceLocation, List<WeightedAllele>> WILD_POOLS = new HashMap<>();

    public static void registerAllele(Allele allele) {
        ALLELES.put(allele.getId(), allele);
    }

    public static void registerLocus(Locus locus) {
        LOCI.put(locus.id(), locus);
        CHROMOSOME_TEMPLATES.computeIfAbsent(locus.chromosomeId(), k -> new ArrayList<>()).add(locus.id());
    }

    /**
     * 向某个基因座的野生基因库中添加一个可能性
     * @param locusId 基因座ID
     * @param alleleId 基因ID
     * @param weight 权重（例如：基础为70，慢速为20，超速为10）
     */
    public static void addWildAllele(ResourceLocation locusId, ResourceLocation alleleId, int weight) {
        WILD_POOLS.computeIfAbsent(locusId, k -> new ArrayList<>())
                .add(new WeightedAllele(alleleId, weight));
    }

    public static Allele getAllele(ResourceLocation id) {
        return ALLELES.get(id);
    }

    // 内部记录类，用于权重计算
    public record WeightedAllele(ResourceLocation alleleId, int weight) {}
}