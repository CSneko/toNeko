package org.cneko.toneko.common.mod.advencements;

import net.minecraft.advancements.CriteriaTriggers;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoCriteria {
    public static final NekoLevelTrigger NEKO_LV100 = CriteriaTriggers.register(MODID+"/neko_lv100", new NekoLevelTrigger());
    public static final GiftNekoTrigger GIFT_NEKO= CriteriaTriggers.register(MODID+"/gift_neko", new GiftNekoTrigger());

    public static void init(){}
}
