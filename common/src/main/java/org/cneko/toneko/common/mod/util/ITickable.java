package org.cneko.toneko.common.mod.util;

public interface ITickable {
    void addTick(int tick);
    void addRemoveTask();
    boolean isRemoved();
}
