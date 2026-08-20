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

    // === 丝袜 ===
    public static final DeferredHolder<CriterionTrigger<?>,CriterionTrigger<FirstLegwearDyeTrigger.TriggerInstance>> LEGWEAR_FIRST_DYE_HOLDER = CRITERION_TRIGGERS.register(MODID+"/legwear_first_dye", FirstLegwearDyeTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>,CriterionTrigger<LegwearGradeSTrigger.TriggerInstance>> LEGWEAR_GRADE_S_HOLDER = CRITERION_TRIGGERS.register(MODID+"/legwear_grade_s", LegwearGradeSTrigger::new);

    // === 气味 ===
    public static final DeferredHolder<CriterionTrigger<?>,CriterionTrigger<FirstLegwearScentTrigger.TriggerInstance>> LEGWEAR_FIRST_SCENT_HOLDER = CRITERION_TRIGGERS.register(MODID+"/legwear_first_scent", FirstLegwearScentTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>,CriterionTrigger<LegwearDriedTrigger.TriggerInstance>> LEGWEAR_DRIED_HOLDER = CRITERION_TRIGGERS.register(MODID+"/legwear_dried", LegwearDriedTrigger::new);

    // === 变质水 ===
    public static final DeferredHolder<CriterionTrigger<?>,CriterionTrigger<SpoiledWaterFirstTrigger.TriggerInstance>> SPOILED_WATER_FIRST_HOLDER = CRITERION_TRIGGERS.register(MODID+"/spoiled_water_first", SpoiledWaterFirstTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>,CriterionTrigger<SpoiledWaterCollectorTrigger.TriggerInstance>> SPOILED_WATER_COLLECTOR_HOLDER = CRITERION_TRIGGERS.register(MODID+"/spoiled_water_collector", SpoiledWaterCollectorTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>,CriterionTrigger<SpoiledWaterDrinkTrigger.TriggerInstance>> SPOILED_WATER_DRINK_HOLDER = CRITERION_TRIGGERS.register(MODID+"/spoiled_water_drink", SpoiledWaterDrinkTrigger::new);

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

        // === 丝袜 ===
        LEGWEAR_FIRST_DYE = (FirstLegwearDyeTrigger) LEGWEAR_FIRST_DYE_HOLDER.get();
        LEGWEAR_GRADE_S = (LegwearGradeSTrigger) LEGWEAR_GRADE_S_HOLDER.get();

        // === 气味 ===
        LEGWEAR_FIRST_SCENT = (FirstLegwearScentTrigger) LEGWEAR_FIRST_SCENT_HOLDER.get();
        LEGWEAR_DRIED = (LegwearDriedTrigger) LEGWEAR_DRIED_HOLDER.get();

        // === 变质水 ===
        SPOILED_WATER_FIRST = (SpoiledWaterFirstTrigger) SPOILED_WATER_FIRST_HOLDER.get();
        SPOILED_WATER_COLLECTOR = (SpoiledWaterCollectorTrigger) SPOILED_WATER_COLLECTOR_HOLDER.get();
        SPOILED_WATER_DRINK = (SpoiledWaterDrinkTrigger) SPOILED_WATER_DRINK_HOLDER.get();
    }
}
