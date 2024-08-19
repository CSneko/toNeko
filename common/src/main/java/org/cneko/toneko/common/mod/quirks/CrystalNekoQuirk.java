package org.cneko.toneko.common.mod.quirks;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class CrystalNekoQuirk extends ToNekoQuirk{
    public static final String ID = "crystal_neko";
    public CrystalNekoQuirk() {
        super(ID);
    }

    @Nullable
    @Override
    public Component getTooltip() {
        return Component.translatable("quirk.toneko.crystal_neko.des");
    }

    @Override
    public int getInteractionValue(QuirkContext context) {
        return getInteractionValue();
    }

    @Override
    public int getInteractionValue() {
        return 1;
    }
}
