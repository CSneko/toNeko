package org.cneko.toneko.common.mod.genetics.api;

import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map; /**
 * 同源染色体对
 */
public class ChromosomePair {
    public Map<Identifier, Identifier> strandA = new HashMap<>(); // 父源
    public Map<Identifier, Identifier> strandB = new HashMap<>(); // 母源
}
