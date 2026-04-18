package org.cneko.toneko.common.mod.genetics;

import org.cneko.toneko.common.mod.genetics.api.GeneticsRegistry;
import org.cneko.toneko.common.mod.genetics.api.Locus;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

public class ToNekoLocus {
    public static final Locus SPEED_SLOT_0 = new Locus(toNekoLoc("speed_slot_0"), 0);
    public static final Locus SPEED_SLOT_1 = new Locus(toNekoLoc("speed_slot_1"), 1);
    public static final Locus ATTACK_SLOT_0 = new Locus(toNekoLoc("attack_slot_0"), 2);
    public static final Locus AGILITY_SLOT_0 = new Locus(toNekoLoc("agility_slot_0"), 1);
    public static final Locus BODY_SIZE_SLOT_0 = new Locus(toNekoLoc("body_size_slot_0"), 0);
    public static final Locus HEALTH_SLOT_0 = new Locus(toNekoLoc("health_slot_0"), 2);
    // 新增稀有属性基因座
    public static final Locus RESISTANCE_SLOT_0 = new Locus(toNekoLoc("resistance_slot_0"), 3);
    public static final Locus RESISTANCE_SLOT_1 = new Locus(toNekoLoc("resistance_slot_1"), 3);
    public static final Locus WEAKNESS_SLOT_0 = new Locus(toNekoLoc("weakness_slot_0"), 4);
    // 新增行为基因座
    public static final Locus BEHAVIOR_SLOT_0 = new Locus(toNekoLoc("behavior_slot_0"), 5);
    public static final Locus BEHAVIOR_SLOT_1 = new Locus(toNekoLoc("behavior_slot_1"), 5);

    public static void init(){
        GeneticsRegistry.registerLocus(SPEED_SLOT_0);
        GeneticsRegistry.registerLocus(SPEED_SLOT_1);
        GeneticsRegistry.registerLocus(ATTACK_SLOT_0);
        GeneticsRegistry.registerLocus(AGILITY_SLOT_0);
        GeneticsRegistry.registerLocus(BODY_SIZE_SLOT_0);
        GeneticsRegistry.registerLocus(HEALTH_SLOT_0);
        // 注册新增基因座
        GeneticsRegistry.registerLocus(RESISTANCE_SLOT_0);
        GeneticsRegistry.registerLocus(RESISTANCE_SLOT_1);
        GeneticsRegistry.registerLocus(WEAKNESS_SLOT_0);
        // 注册行为基因座
        GeneticsRegistry.registerLocus(BEHAVIOR_SLOT_0);
        GeneticsRegistry.registerLocus(BEHAVIOR_SLOT_1);
    }
}
