package org.cneko.toneko.common.mod.genetics;

import org.cneko.toneko.common.mod.genetics.api.GeneticsRegistry;
import org.cneko.toneko.common.mod.genetics.api.Locus;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

public class ToNekoLocus {
    public static final Locus SPEED_SLOT_0 = new Locus(toNekoLoc("speed_slot_0"), 0);
    public static final Locus SPEED_SLOT_1 = new Locus(toNekoLoc("speed_slot_1"), 1);
    public static final Locus ATTACK_SLOT_0 = new Locus(toNekoLoc("attack_slot_0"), 2);
    public static final Locus AGILITY_SLOT_0 = new Locus(toNekoLoc("agility_slot_0"), 1);

    public static void init(){
        GeneticsRegistry.registerLocus(SPEED_SLOT_0);
        GeneticsRegistry.registerLocus(SPEED_SLOT_1);
        GeneticsRegistry.registerLocus(ATTACK_SLOT_0);
        GeneticsRegistry.registerLocus(AGILITY_SLOT_0);
    }
}
