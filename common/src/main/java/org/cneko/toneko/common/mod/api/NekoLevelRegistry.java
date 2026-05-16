package org.cneko.toneko.common.mod.api;

import org.cneko.toneko.common.mod.entities.INeko;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class NekoLevelRegistry {
    private static final Map<String, NekoLevelFactor> FACTORS = new LinkedHashMap<>();

    public static void register(NekoLevelFactor factor) {
        FACTORS.put(factor.getId(), factor);
    }

    public static NekoLevelFactor getFactor(String id) {
        return FACTORS.get(id);
    }

    public static Collection<NekoLevelFactor> getFactors() {
        return FACTORS.values();
    }

    public static boolean hasFactor(String id) {
        return FACTORS.containsKey(id);
    }

    public static NekoLevelFactor interaction() { return FACTORS.get("interaction"); }
    public static NekoLevelFactor combat() { return FACTORS.get("combat"); }
    public static NekoLevelFactor base() { return FACTORS.get("base"); }

    public static double computeTotal(INeko neko) {
        double total = 0;
        for (NekoLevelFactor factor : FACTORS.values()) {
            total += factor.getLevel(neko.getNekoLevelFactorRaw(factor.getId()));
        }
        return total;
    }
}
