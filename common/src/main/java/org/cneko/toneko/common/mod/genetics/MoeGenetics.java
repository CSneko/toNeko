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

    // 暴露给外部进行核型绑定的萌属性坑位
    public static final Locus MOE_SLOT_0 = new Locus(toNekoLoc("moe_slot_0"));
    public static final Locus MOE_SLOT_1 = new Locus(toNekoLoc("moe_slot_1"));
    public static final Locus MOE_SLOT_2 = new Locus(toNekoLoc("moe_slot_2"));

    public static void registerAll() {
        GeneticsRegistry.registerLocus(MOE_SLOT_0);
        GeneticsRegistry.registerLocus(MOE_SLOT_1);
        GeneticsRegistry.registerLocus(MOE_SLOT_2);

        List<ResourceLocation> registeredMoeAlleles = new ArrayList<>();

        for (String tagName : NekoEntity.MOE_TAGS) {
            ResourceLocation alleleId = toNekoLoc("moe_" + tagName);

            Allele moeAllele = new Allele(alleleId, 20,
                    (entity, tagCompound) -> {
                        // 即使其他通用实体抽到了这个基因，也会因为类型不匹配而安全跳过
                        if (entity instanceof NekoEntity neko) {
                            List<String> tags = new ArrayList<>(neko.getMoeTags());
                            if (!tags.contains(tagName)) {
                                tags.add(tagName);
                                neko.setMoeTags(tags);
                            }
                        }
                    },
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

        populateMoeWildPool(MOE_SLOT_0.id(), registeredMoeAlleles, 0);
        populateMoeWildPool(MOE_SLOT_1.id(), registeredMoeAlleles, 65);
        populateMoeWildPool(MOE_SLOT_2.id(), registeredMoeAlleles, 350);
    }

    private static void populateMoeWildPool(ResourceLocation locusId, List<ResourceLocation> moeAlleles, int blankWeight) {
        if (blankWeight > 0) {
            GeneticsRegistry.addWildAllele(locusId, ToNekoAlleles.WILD_TYPE.getId(), blankWeight);
        }
        int singleTagWeight = 10;
        for (ResourceLocation alleleId : moeAlleles) {
            GeneticsRegistry.addWildAllele(locusId, alleleId, singleTagWeight);
        }
    }
}