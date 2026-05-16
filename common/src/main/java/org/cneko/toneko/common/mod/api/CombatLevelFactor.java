package org.cneko.toneko.common.mod.api;

public class CombatLevelFactor implements NekoLevelFactor {
    @Override
    public String getId() {
        return "combat";
    }

    @Override
    public double getLevel(double rawValue) {
        // 每级所需经验递增（第1级100，第2级200，第3级300……）
        double C = 100.0;
        return (Math.sqrt(1 + 8 * rawValue / C) - 1) / 2;
    }
}
