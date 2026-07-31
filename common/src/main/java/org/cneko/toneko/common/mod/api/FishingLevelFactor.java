package org.cneko.toneko.common.mod.api;

public class FishingLevelFactor implements NekoLevelFactor {
    @Override
    public String getId() {
        return "fishing";
    }

    @Override
    public double getLevel(double rawValue) {
        // C=100：每升1级需要100额外经验（第1级100，第2级200，第3级300……）
        // 比combat(C=60)升级慢，比interaction(C=300)快很多
        double C = 100.0;
        return (Math.sqrt(1 + 8 * rawValue / C) - 1) / 2;
    }
}
