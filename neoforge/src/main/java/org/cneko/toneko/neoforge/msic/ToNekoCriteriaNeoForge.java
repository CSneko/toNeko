package org.cneko.toneko.neoforge.msic;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.cneko.toneko.common.mod.advencements.GiftNekoTrigger;
import org.cneko.toneko.common.mod.advencements.NekoLevelTrigger;

import static org.cneko.toneko.common.Bootstrap.MODID;
import static org.cneko.toneko.neoforge.ToNekoNeoForge.CRITERION_TRIGGERS;
import static org.cneko.toneko.common.mod.advencements.ToNekoCriteria.*;

public class ToNekoCriteriaNeoForge {
    public static final DeferredHolder<CriterionTrigger<?>,CriterionTrigger<NekoLevelTrigger.TriggerInstance>> NEKO_LV100_HOLDER = CRITERION_TRIGGERS.register(MODID+"/neko_lv100", NekoLevelTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>,CriterionTrigger<GiftNekoTrigger.TriggerInstance>> GIFT_NEKO_HOLDER = CRITERION_TRIGGERS.register(MODID+"/gift_neko", GiftNekoTrigger::new);
    public static void init(){
    }

    public static void reg(){
        NEKO_LV100 = (NekoLevelTrigger) NEKO_LV100_HOLDER.get();
        GIFT_NEKO = (GiftNekoTrigger) GIFT_NEKO_HOLDER.get();
    }
}
