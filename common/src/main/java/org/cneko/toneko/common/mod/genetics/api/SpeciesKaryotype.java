package org.cneko.toneko.common.mod.genetics.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpeciesKaryotype {
    private final int chromosomePairs;
    private final Map<Integer, List<Locus>> chromosomes = new HashMap<>();

    // 基础构造函数
    public SpeciesKaryotype(int chromosomePairs) {
        this.chromosomePairs = chromosomePairs;
        for (int i = 1; i <= chromosomePairs; i++) {
            chromosomes.put(i, new ArrayList<>());
        }
    }

    /**
     * 【新增】：从一个已有的父类核型派生（继承）
     * @param parent 基础核型
     * @param extraChromosomePairs 额外增加的染色体对数（如果不增加传 0）
     */
    public SpeciesKaryotype(SpeciesKaryotype parent, int extraChromosomePairs) {
        this.chromosomePairs = parent.getChromosomePairs() + extraChromosomePairs;

        // 深度复制父类的基因座分布
        for (Map.Entry<Integer, List<Locus>> entry : parent.chromosomes.entrySet()) {
            this.chromosomes.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }

        // 初始化新增的染色体
        for (int i = parent.getChromosomePairs() + 1; i <= this.chromosomePairs; i++) {
            this.chromosomes.put(i, new ArrayList<>());
        }
    }

    public SpeciesKaryotype bindLocus(int chromosomeId, Locus locus) {
        if (chromosomeId < 1 || chromosomeId > chromosomePairs) {
            throw new IllegalArgumentException("染色体ID超出该物种的核型范围: " + chromosomeId);
        }
        // 防止重复绑定同一个坑位
        if (!this.chromosomes.get(chromosomeId).contains(locus)) {
            this.chromosomes.get(chromosomeId).add(locus);
        }
        return this;
    }

    public int getChromosomePairs() { return chromosomePairs; }

    public List<Locus> getLociOnChromosome(int chromosomeId) {
        return chromosomes.getOrDefault(chromosomeId, List.of());
    }
}