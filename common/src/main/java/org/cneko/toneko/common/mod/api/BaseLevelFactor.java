package org.cneko.toneko.common.mod.api;

public class BaseLevelFactor implements NekoLevelFactor {
    @Override
    public String getId() {
        return "base";
    }

    @Override
    public double getLevel(double rawValue) {
        return rawValue;
    }
}
