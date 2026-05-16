package org.cneko.toneko.common.mod.genetics.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GeneticsRegistry {
    // 基因库
    public static final Map<ResourceLocation, Allele> ALLELES = new HashMap<>();

    // 基因座库 (这里就是存储 Locus 的地方)
    public static final Map<ResourceLocation, Locus> LOCI = new HashMap<>();

    // 野生基因库： 基因座ID -> 该坑位可能出现的基因及其权重
    public static final Map<ResourceLocation, List<WeightedAllele>> WILD_POOLS = new HashMap<>();

    // 核型字典 (实体类 -> 核型)
    private static final Map<Class<? extends LivingEntity>, SpeciesKaryotype> KARYOTYPES = new HashMap<>();
    // 继承树查找缓存，避免性能损耗
    private static final Map<Class<? extends LivingEntity>, SpeciesKaryotype> KARYOTYPE_CACHE = new ConcurrentHashMap<>();

    // ================== 注册方法 ==================

    public static void registerAllele(Allele allele) {
        ALLELES.put(allele.getId(), allele);
    }

    public static void registerLocus(Locus locus) {
        LOCI.put(locus.id(), locus);
    }

    public static void registerKaryotype(Class<? extends LivingEntity> entityClass, SpeciesKaryotype karyotype) {
        KARYOTYPES.put(entityClass, karyotype);
        KARYOTYPE_CACHE.clear(); // 注册新核型时清空缓存，确保继承树刷新
    }

    public static void addWildAllele(ResourceLocation locusId, ResourceLocation alleleId, int weight) {
        WILD_POOLS.computeIfAbsent(locusId, k -> new ArrayList<>())
                .add(new WeightedAllele(alleleId, weight));
    }

    // ================== 获取方法 ==================

    public static Allele getAllele(ResourceLocation id) {
        return ALLELES.get(id);
    }

    public static Locus getLocus(ResourceLocation id) {
        return LOCI.get(id);
    }

    /**
     * 获取实体的核型（自动向上追溯父类并缓存）
     */
    public static SpeciesKaryotype getKaryotype(LivingEntity entity) {
        return getKaryotype(entity.getClass());
    }

    public static SpeciesKaryotype getKaryotype(Class<? extends LivingEntity> targetClass) {
        if (KARYOTYPE_CACHE.containsKey(targetClass)) {
            return KARYOTYPE_CACHE.get(targetClass);
        }

        Class<?> currentClass = targetClass;
        while (currentClass != null && LivingEntity.class.isAssignableFrom(currentClass)) {
            if (KARYOTYPES.containsKey(currentClass)) {
                SpeciesKaryotype found = KARYOTYPES.get(currentClass);
                KARYOTYPE_CACHE.put(targetClass, found);
                return found;
            }
            currentClass = currentClass.getSuperclass();
        }
        return null;
    }

    public record WeightedAllele(ResourceLocation alleleId, int weight) {}
}