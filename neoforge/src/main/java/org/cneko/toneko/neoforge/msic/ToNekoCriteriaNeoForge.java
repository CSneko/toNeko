package org.cneko.toneko.neoforge.msic;

import net.minecraft.advancements.CriterionTrigger;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.cneko.toneko.common.mod.advencements.*;

import static org.cneko.toneko.common.Bootstrap.MODID;
import static org.cneko.toneko.common.mod.advencements.ToNekoCriteria.*;
import static org.cneko.toneko.neoforge.ToNekoNeoForge.CRITERION_TRIGGERS;

public class ToNekoCriteriaNeoForge {
    public static final DeferredHolder<CriterionTrigger<?>,CriterionTrigger<NekoLevelTrigger.TriggerInstance>> NEKO_LV100_HOLDER = CRITERION_TRIGGERS.register(MODID+"/neko_lv100", NekoLevelTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>,CriterionTrigger<GiftNekoTrigger.TriggerInstance>> GIFT_NEKO_HOLDER = CRITERION_TRIGGERS.register(MODID+"/gift_neko", GiftNekoTrigger::new);

    // === 新增 Trigger ===
    public static final DeferredHolder<CriterionTrigger<?>,CriterionTrigger<NekoBecomeTrigger.TriggerInstance>> NEKO_BECOME_HOLDER = CRITERION_TRIGGERS.register(MODID+"/neko_become", NekoBecomeTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>,CriterionTrigger<HissComboTrigger.TriggerInstance>> HISS_COMBO_HOLDER = CRITERION_TRIGGERS.register(MODID+"/hiss_combo", HissComboTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>,CriterionTrigger<HissFirstUseTrigger.TriggerInstance>> HISS_FIRST_USE_HOLDER = CRITERION_TRIGGERS.register(MODID+"/hiss_first_use", HissFirstUseTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>,CriterionTrigger<NekoClimbTrigger.TriggerInstance>> NEKO_CLIMB_HOLDER = CRITERION_TRIGGERS.register(MODID+"/neko_climb", NekoClimbTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>,CriterionTrigger<TameNekoTrigger.TriggerInstance>> TAME_NEKO_HOLDER = CRITERION_TRIGGERS.register(MODID+"/tame_neko", TameNekoTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>,CriterionTrigger<NekoBreedTrigger.TriggerInstance>> NEKO_BREED_HOLDER = CRITERION_TRIGGERS.register(MODID+"/neko_breed", NekoBreedTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>,CriterionTrigger<NineLivesTrigger.TriggerInstance>> NINE_LIVES_HOLDER = CRITERION_TRIGGERS.register(MODID+"/nine_lives", NineLivesTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>,CriterionTrigger<EasterEggTrigger.TriggerInstance>> EASTER_EGG_HOLDER = CRITERION_TRIGGERS.register(MODID+"/easter_egg", EasterEggTrigger::new);

    public static void init(){
    }

    public static void reg(){
        NEKO_LV100 = (NekoLevelTrigger) NEKO_LV100_HOLDER.get();
        GIFT_NEKO = (GiftNekoTrigger) GIFT_NEKO_HOLDER.get();

        // === 新增 Trigger ===
        NEKO_BECOME = (NekoBecomeTrigger) NEKO_BECOME_HOLDER.get();
        HISS_COMBO = (HissComboTrigger) HISS_COMBO_HOLDER.get();
        HISS_FIRST_USE = (HissFirstUseTrigger) HISS_FIRST_USE_HOLDER.get();
        NEKO_CLIMB = (NekoClimbTrigger) NEKO_CLIMB_HOLDER.get();
        TAME_NEKO = (TameNekoTrigger) TAME_NEKO_HOLDER.get();
        NEKO_BREED = (NekoBreedTrigger) NEKO_BREED_HOLDER.get();
        NINE_LIVES = (NineLivesTrigger) NINE_LIVES_HOLDER.get();
        EASTER_EGG = (EasterEggTrigger) EASTER_EGG_HOLDER.get();
    }
}
