package org.cneko.toneko.fabric.msic;

import net.minecraft.advancements.CriteriaTriggers;
import org.cneko.toneko.common.mod.advencements.GiftNekoTrigger;
import org.cneko.toneko.common.mod.advencements.NekoLevelTrigger;

import static org.cneko.toneko.common.Bootstrap.MODID;
import static org.cneko.toneko.common.mod.advencements.ToNekoCriteria.*;
public class ToNekoCriteriaFabric {
    public static void init(){
        NEKO_LV100 = CriteriaTriggers.register(MODID+"/neko_lv100", new NekoLevelTrigger());
        GIFT_NEKO = CriteriaTriggers.register(MODID+"/gift_neko", new GiftNekoTrigger());
    }
}
