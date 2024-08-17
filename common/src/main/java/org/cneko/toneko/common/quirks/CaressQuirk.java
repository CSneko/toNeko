package org.cneko.toneko.common.quirks;

public class CaressQuirk extends Quirk {
    public CaressQuirk(String id) {
        super(id);
    }

    @Override
    public int getInteractionValue() {
        return 1;
    }
}
