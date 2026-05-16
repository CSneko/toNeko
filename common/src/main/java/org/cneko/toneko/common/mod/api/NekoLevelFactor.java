package org.cneko.toneko.common.mod.api;

import org.cneko.toneko.common.mod.entities.INeko;

public interface NekoLevelFactor {
    String getId();

    double getLevel(double rawValue);

    default double getDefaultRawValue() { return 0; }

    default void addRaw(INeko neko, double amount) {
        neko.setNekoLevelFactorRaw(getId(), neko.getNekoLevelFactorRaw(getId()) + amount);
    }

    default void setRaw(INeko neko, double value) {
        neko.setNekoLevelFactorRaw(getId(), value);
    }
}
