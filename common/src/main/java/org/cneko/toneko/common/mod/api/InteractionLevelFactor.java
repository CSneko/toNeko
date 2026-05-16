package org.cneko.toneko.common.mod.api;

public class InteractionLevelFactor implements NekoLevelFactor {
    @Override
    public String getId() {
        return "interaction";
    }

    @Override
    public double getLevel(double rawValue) {
        // 每级所需经验递增（第1级500，第2级1000，第3级1500……）
        // level = (-1 + sqrt(1 + 8*raw/C)) / 2，其中 C 是1级所需经验
        double C = 500.0;
        return (Math.sqrt(1 + 8 * rawValue / C) - 1) / 2;
    }
}
