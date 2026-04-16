package org.cneko.toneko.common.mod.genetics.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.List;

public interface IGeneticEntity {
    Genome getGenome();
    void setGenome(Genome genome);

    CompoundTag getGeneticData();

    // 用于追踪当前生效的基因，以便在重新表达时移除它们
    List<ExpressedTrait> getActiveTraits();

    // 用于追踪基因注入的 AI，以便清理
    List<Goal> getActiveGeneticGoals();

    void expressTraits();

    // 记录某个基因座当前表达的基因
    record ExpressedTrait(Locus locus, Allele allele) {}
}