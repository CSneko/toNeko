package org.cneko.toneko.fabric.msic;

import net.minecraft.advancements.CriteriaTriggers;
import org.cneko.toneko.common.mod.advencements.*;

import static org.cneko.toneko.common.Bootstrap.MODID;
import static org.cneko.toneko.common.mod.advencements.ToNekoCriteria.*;
public class ToNekoCriteriaFabric {
    public static void init(){
        NEKO_LV100 = CriteriaTriggers.register(MODID+"/neko_lv100", new NekoLevelTrigger());
        GIFT_NEKO = CriteriaTriggers.register(MODID+"/gift_neko", new GiftNekoTrigger());

        // === 新增 Trigger ===
        NEKO_BECOME = CriteriaTriggers.register(MODID+"/neko_become", new NekoBecomeTrigger());
        HISS_COMBO = CriteriaTriggers.register(MODID+"/hiss_combo", new HissComboTrigger());
        HISS_FIRST_USE = CriteriaTriggers.register(MODID+"/hiss_first_use", new HissFirstUseTrigger());
        NEKO_CLIMB = CriteriaTriggers.register(MODID+"/neko_climb", new NekoClimbTrigger());
        TAME_NEKO = CriteriaTriggers.register(MODID+"/tame_neko", new TameNekoTrigger());
        NEKO_BREED = CriteriaTriggers.register(MODID+"/neko_breed", new NekoBreedTrigger());
        NINE_LIVES = CriteriaTriggers.register(MODID+"/nine_lives", new NineLivesTrigger());
        EASTER_EGG = CriteriaTriggers.register(MODID+"/easter_egg", new EasterEggTrigger());
    }
}
