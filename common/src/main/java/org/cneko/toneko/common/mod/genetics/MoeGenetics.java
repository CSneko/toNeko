package org.cneko.toneko.common.mod.genetics;

import net.minecraft.resources.ResourceLocation;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.genetics.api.Allele;
import org.cneko.toneko.common.mod.genetics.api.GeneticsRegistry;
import org.cneko.toneko.common.mod.genetics.api.Locus;

import java.util.ArrayList;
import java.util.List;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

public class MoeGenetics {
    // 将萌属性绑定在第 2 号染色体上
    public static final int MOE_CHROMOSOME_ID = 2;

    public static void registerAll() {

        // 注册 3 个萌属性基因座 (坑位)
        Locus moeSlot0 = new Locus(toNekoLoc("moe_slot_0"), MOE_CHROMOSOME_ID);
        Locus moeSlot1 = new Locus(toNekoLoc("moe_slot_1"), MOE_CHROMOSOME_ID);
        Locus moeSlot2 = new Locus(toNekoLoc("moe_slot_2"), MOE_CHROMOSOME_ID);

        GeneticsRegistry.registerLocus(moeSlot0);
        GeneticsRegistry.registerLocus(moeSlot1);
        GeneticsRegistry.registerLocus(moeSlot2);

        //  一键批量生成并注册所有的萌属性基因
        List<ResourceLocation> registeredMoeAlleles = new ArrayList<>();

        for (String tagName : NekoEntity.MOE_TAGS) {
            ResourceLocation alleleId = toNekoLoc("moe_" + tagName);

            Allele moeAllele = new Allele(alleleId, 20,
                    // onExpress: 表达时添加萌属性标签
                    (entity, tagCompound) -> {
                        if (entity instanceof NekoEntity neko) {
                            List<String> tags = new ArrayList<>(neko.getMoeTags());
                            // 避免重复添加 (如果是纯合子，两个槽位都是傲娇，只加一次)
                            if (!tags.contains(tagName)) {
                                tags.add(tagName);
                                neko.setMoeTags(tags);
                            }
                        }
                    },
                    // onRemove: 被清理时移除该标签
                    (entity, tagCompound) -> {
                        if (entity instanceof NekoEntity neko) {
                            List<String> tags = new ArrayList<>(neko.getMoeTags());
                            if (tags.contains(tagName)) {
                                tags.remove(tagName);
                                neko.setMoeTags(tags);
                            }
                        }
                    }
            );

            GeneticsRegistry.registerAllele(moeAllele);
            registeredMoeAlleles.add(alleleId);
        }

        // 一键配置野生猫娘的基因池 (让刷怪蛋生成的猫娘带有随机萌属性)
        // 假设共有 15 个萌属性。为了让猫娘大多有 1~2 个属性，少数有 3 个属性，我们分配不同的空白权重。

        // 坑位 0：基本必有一个萌属性 (空白权重 0)
        populateMoeWildPool(moeSlot0.id(), registeredMoeAlleles, 0);

        // 坑位 1：有 30% 概率是空白 (没有第二个属性)
        // 假设 15个属性，每个权重 10，总属性权重 150。让空白占 30%，则空白权重设为 65
        populateMoeWildPool(moeSlot1.id(), registeredMoeAlleles, 65);

        // 坑位 2：有 70% 概率是空白 (极少有第三个属性)
        // 让空白占 70%，总属性权重 150，则空白权重设为 350
        populateMoeWildPool(moeSlot2.id(), registeredMoeAlleles, 350);
    }

    /**
     * 辅助方法：把所有的萌属性均匀地放入某个坑位的野生基因池中
     */
    private static void populateMoeWildPool(ResourceLocation locusId, List<ResourceLocation> moeAlleles, int blankWeight) {
        // 如果允许有概率为空，添加空白基因
        if (blankWeight > 0) {
            GeneticsRegistry.addWildAllele(locusId, ToNekoAlleles.WILD_TYPE.getId(), blankWeight);
        }

        // 每个具体的萌属性赋予相等的权重 (例如 10)
        int singleTagWeight = 10;
        for (ResourceLocation alleleId : moeAlleles) {
            GeneticsRegistry.addWildAllele(locusId, alleleId, singleTagWeight);
        }
    }
}