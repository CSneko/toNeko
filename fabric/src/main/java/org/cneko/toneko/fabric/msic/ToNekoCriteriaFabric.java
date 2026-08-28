package org.cneko.toneko.fabric.msic;

import net.minecraft.advancements.CriteriaTriggers;
import org.cneko.toneko.common.mod.advencements.*;

import static org.cneko.toneko.common.Bootstrap.MODID;
import static org.cneko.toneko.common.mod.advencements.ToNekoCriteria.*;
public class ToNekoCriteriaFabric {
    private static <T extends net.minecraft.advancements.CriterionTrigger<?>> T register(String name, T trigger) {
        // 26.x：CriteriaTriggers.register 变为 private；改走 TRIGGER_TYPES 注册表
        return net.minecraft.core.Registry.register(
                net.minecraft.core.registries.BuiltInRegistries.TRIGGER_TYPES,
                net.minecraft.resources.Identifier.fromNamespaceAndPath(MODID, name),
                trigger);
    }

    public static void init(){
        NEKO_LV100 = register("neko_lv100", new NekoLevelTrigger());
        GIFT_NEKO = register("gift_neko", new GiftNekoTrigger());

        // === 新增 Trigger ===
        NEKO_BECOME = register("neko_become", new NekoBecomeTrigger());
        HISS_COMBO = register("hiss_combo", new HissComboTrigger());
        HISS_FIRST_USE = register("hiss_first_use", new HissFirstUseTrigger());
        NEKO_CLIMB = register("neko_climb", new NekoClimbTrigger());
        TAME_NEKO = register("tame_neko", new TameNekoTrigger());
        NEKO_BREED = register("neko_breed", new NekoBreedTrigger());
        NINE_LIVES = register("nine_lives", new NineLivesTrigger());
        EASTER_EGG = register("easter_egg", new EasterEggTrigger());

        // === 丝袜 ===
        LEGWEAR_FIRST_DYE = register("legwear_first_dye", new FirstLegwearDyeTrigger());
        LEGWEAR_GRADE_S = register("legwear_grade_s", new LegwearGradeSTrigger());

        // === 气味 ===
        LEGWEAR_FIRST_SCENT = register("legwear_first_scent", new FirstLegwearScentTrigger());
        LEGWEAR_DRIED = register("legwear_dried", new LegwearDriedTrigger());

        // === 变质水 ===
        SPOILED_WATER_FIRST = register("spoiled_water_first", new SpoiledWaterFirstTrigger());
        SPOILED_WATER_COLLECTOR = register("spoiled_water_collector", new SpoiledWaterCollectorTrigger());
        SPOILED_WATER_DRINK = register("spoiled_water_drink", new SpoiledWaterDrinkTrigger());
    }
}
