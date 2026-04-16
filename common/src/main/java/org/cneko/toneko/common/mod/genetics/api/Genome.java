package org.cneko.toneko.common.mod.genetics.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.HashMap;
import java.util.List;
import java.util.Map; /**
 * 完整基因组
 */
public class Genome {
    // 染色体ID -> 染色体对
    private final Map<Integer, ChromosomePair> pairs = new HashMap<>();

    /**
     * 减数分裂生成配子 (对应分离定律与自由组合定律)
     */
    public Gamete createGamete(RandomSource random) {
        Gamete gamete = new Gamete();
        for (Map.Entry<Integer, ChromosomePair> entry : pairs.entrySet()) {
            int chrId = entry.getKey();
            ChromosomePair pair = entry.getValue();

            // 自由组合定律：每一对染色体的选择（A或B）都是独立随机的
            boolean pickA = random.nextBoolean();
            // 分离定律：同源染色体分离开，只取其一
            gamete.chromosomes.put(chrId, new HashMap<>(pickA ? pair.strandA : pair.strandB));
        }
        return gamete;
    }

    /**
     * 受精作用：合并两个配子
     */
    public static Genome combine(Gamete paternal, Gamete maternal) {
        Genome genome = new Genome();
        // 遍历模板中的所有染色体ID，确保对齐
        for (Integer chrId : GeneticsRegistry.CHROMOSOME_TEMPLATES.keySet()) {
            ChromosomePair pair = new ChromosomePair();
            if (paternal.chromosomes.containsKey(chrId)) pair.strandA.putAll(paternal.chromosomes.get(chrId));
            if (maternal.chromosomes.containsKey(chrId)) pair.strandB.putAll(maternal.chromosomes.get(chrId));
            genome.pairs.put(chrId, pair);
        }
        return genome;
    }

    /**
     * 为没有遗传接口的实体生成降级配子（随机/野生型）
     */
    public static Gamete generateFallbackGamete(RandomSource random) {
        Gamete gamete = new Gamete();
        for (Map.Entry<Integer, List<ResourceLocation>> entry : GeneticsRegistry.CHROMOSOME_TEMPLATES.entrySet()) {
            Map<ResourceLocation, ResourceLocation> strand = new HashMap<>();

            for (ResourceLocation locusId : entry.getValue()) {
                List<GeneticsRegistry.WeightedAllele> pool = GeneticsRegistry.WILD_POOLS.get(locusId);

                if (pool != null && !pool.isEmpty()) {
                    // 权重随机算法 (Roulette Wheel Selection)
                    int totalWeight = pool.stream().mapToInt(GeneticsRegistry.WeightedAllele::weight).sum();
                    int randValue = random.nextInt(totalWeight);

                    for (GeneticsRegistry.WeightedAllele weightedAllele : pool) {
                        randValue -= weightedAllele.weight();
                        if (randValue < 0) {
                            strand.put(locusId, weightedAllele.alleleId());
                            break;
                        }
                    }
                }
            }
            gamete.chromosomes.put(entry.getKey(), strand);
        }
        return gamete;
    }

    /**
     * 基因表达（带有完整的生命周期：清理旧的 -> 叠加新的）
     */
    public void express(LivingEntity entity) {
        if (!(entity instanceof IGeneticEntity geneticEntity)) return;

        // 【第一步：彻底清理旧的遗传因子遗留】
        CompoundTag customData = geneticEntity.getGeneticData();

        // 1. 移除旧的 Modifier 和执行 onRemove 回调
        for (IGeneticEntity.ExpressedTrait oldTrait : geneticEntity.getActiveTraits()) {
            oldTrait.allele().remove(entity, oldTrait.locus(), customData);
        }
        geneticEntity.getActiveTraits().clear();

        // 2. 移除旧的动态 AI
        if (entity instanceof Mob mob) {
            for (Goal oldGoal : geneticEntity.getActiveGeneticGoals()) {
                mob.goalSelector.removeGoal(oldGoal);
            }
            geneticEntity.getActiveGeneticGoals().clear();
        }

        // 【第二步：计算并应用新的遗传因子】
        for (ChromosomePair pair : pairs.values()) {
            for (ResourceLocation locusId : pair.strandA.keySet()) {
                Locus locus = GeneticsRegistry.LOCI.get(locusId);
                if (locus == null) continue;

                ResourceLocation alleleIdA = pair.strandA.get(locusId);
                ResourceLocation alleleIdB = pair.strandB.get(locusId);

                Allele alleleA = GeneticsRegistry.getAllele(alleleIdA);
                Allele alleleB = GeneticsRegistry.getAllele(alleleIdB);

                // 判断显隐性
                Allele expressed = resolveDominance(alleleA, alleleB, entity.getRandom());

                if (expressed != null) {
                    // 应用新的 Modifier、自定义回调和 AI，并将 AI 加入追踪列表
                    expressed.apply(entity, locus, customData, geneticEntity.getActiveGeneticGoals());
                    // 记录下来，方便下次清理
                    geneticEntity.getActiveTraits().add(new IGeneticEntity.ExpressedTrait(locus, expressed));
                }
            }
        }
    }

    private Allele resolveDominance(Allele a, Allele b, RandomSource random) {
        if (a == null) return b;
        if (b == null) return a;
        if (a.getDominance() > b.getDominance()) return a;
        if (b.getDominance() > a.getDominance()) return b;
        // 共显性/累加效应等可以在这里扩展，目前默认同等显性时随机或视为同一个
        return random.nextBoolean() ? a : b;
    }

    // NBT 序列化
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<Integer, ChromosomePair> entry : pairs.entrySet()) {
            CompoundTag pairTag = new CompoundTag();

            CompoundTag strandA = new CompoundTag();
            entry.getValue().strandA.forEach((l, a) -> strandA.putString(l.toString(), a.toString()));

            CompoundTag strandB = new CompoundTag();
            entry.getValue().strandB.forEach((l, a) -> strandB.putString(l.toString(), a.toString()));

            pairTag.put("A", strandA);
            pairTag.put("B", strandB);
            tag.put(entry.getKey().toString(), pairTag);
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        pairs.clear();
        for (String chrKey : tag.getAllKeys()) {
            try {
                int chrId = Integer.parseInt(chrKey);
                CompoundTag pairTag = tag.getCompound(chrKey);
                ChromosomePair pair = new ChromosomePair();

                CompoundTag strandA = pairTag.getCompound("A");
                strandA.getAllKeys().forEach(k -> pair.strandA.put(ResourceLocation.parse(k), ResourceLocation.parse(strandA.getString(k))));

                CompoundTag strandB = pairTag.getCompound("B");
                strandB.getAllKeys().forEach(k -> pair.strandB.put(ResourceLocation.parse(k), ResourceLocation.parse(strandB.getString(k))));

                pairs.put(chrId, pair);
            } catch (NumberFormatException ignored) {}
        }
    }
}
