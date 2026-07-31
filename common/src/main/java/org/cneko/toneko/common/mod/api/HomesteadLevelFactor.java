package org.cneko.toneko.common.mod.api;

public class HomesteadLevelFactor implements NekoLevelFactor {
    @Override
    public String getId() {
        return "homestead";
    }

    @Override
    public double getLevel(double rawValue) {
        // C=180：家园因子升级节奏适中，比战斗(C=60)慢，比互动(C=300)快
        double C = 180.0;
        return (Math.sqrt(1 + 8 * rawValue / C) - 1) / 2;
    }
}
