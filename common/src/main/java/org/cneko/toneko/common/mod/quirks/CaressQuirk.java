package org.cneko.toneko.common.mod.quirks;

public class CaressQuirk extends ToNekoQuirk{
    public CaressQuirk(String id) {
        super(id);
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
