package org.cneko.toneko.common.mod.genetics.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;
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
    // 核型ID -> 核型（数据包引用）
    public static final Map<ResourceLocation, SpeciesKaryotype> KARYOTYPES_BY_ID = new HashMap<>();

    // ================== 动态数据追踪（数据包加载/重载用） ==================

    /** 由数据包添加的等位基因 ID 集合 */
    public static final Set<ResourceLocation> DYNAMIC_ALLELES = new HashSet<>();
    /** 由数据包添加的基因座 ID 集合 */
    public static final Set<ResourceLocation> DYNAMIC_LOCI = new HashSet<>();
    /** 由数据包添加的野生基因池条目：locusId -> set of alleleIds */
    public static final Map<ResourceLocation, Set<ResourceLocation>> DYNAMIC_WILD_POOLS = new HashMap<>();

    // ================== 注册方法 ==================

    public static void registerAllele(Allele allele) {
        ALLELES.put(allele.getId(), allele);
    }

    public static void registerLocus(Locus locus) {
        LOCI.put(locus.id(), locus);
    }

    /**
     * 注册核型并设置其 ID（数据包引用用）
     */
    public static void registerKaryotype(ResourceLocation id, Class<? extends LivingEntity> entityClass, SpeciesKaryotype karyotype) {
        karyotype.setId(id);
        KARYOTYPES.put(entityClass, karyotype);
        KARYOTYPES_BY_ID.put(id, karyotype);
        KARYOTYPE_CACHE.clear();
    }

    public static void registerKaryotype(Class<? extends LivingEntity> entityClass, SpeciesKaryotype karyotype) {
        KARYOTYPES.put(entityClass, karyotype);
        KARYOTYPE_CACHE.clear();
    }

    public static void addWildAllele(ResourceLocation locusId, ResourceLocation alleleId, int weight) {
        WILD_POOLS.computeIfAbsent(locusId, k -> new ArrayList<>())
                .add(new WeightedAllele(alleleId, weight));
    }

    // ================== 动态数据清理 ==================

    /**
     * 清除所有由数据包添加的动态数据（用于 /reload 时重新加载）
     */
    public static void clearDynamicData() {
        // 删除动态等位基因
        DYNAMIC_ALLELES.forEach(ALLELES::remove);
        DYNAMIC_ALLELES.clear();

        // 删除动态基因座及其野生池
        DYNAMIC_LOCI.forEach(locusId -> {
            LOCI.remove(locusId);
            WILD_POOLS.remove(locusId);
        });
        DYNAMIC_LOCI.clear();

        // 从已有（硬编码）基因座的野生池中移除动态条目
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : DYNAMIC_WILD_POOLS.entrySet()) {
            ResourceLocation locusId = entry.getKey();
            Set<ResourceLocation> alleleIds = entry.getValue();
            List<WeightedAllele> pool = WILD_POOLS.get(locusId);
            if (pool != null) {
                pool.removeIf(wa -> alleleIds.contains(wa.alleleId()));
            }
        }
        DYNAMIC_WILD_POOLS.clear();

        // 注意：核型修改（添加基因座/染色体）是幂等的，不做清理，/reload 时重新应用即可
    }

    // ================== 获取方法 ==================

    public static Allele getAllele(ResourceLocation id) {
        return ALLELES.get(id);
    }

    public static Locus getLocus(ResourceLocation id) {
        return LOCI.get(id);
    }

    /**
     * 根据核型 ID 获取核型
     */
    public static SpeciesKaryotype getKaryotypeById(ResourceLocation id) {
        return KARYOTYPES_BY_ID.get(id);
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