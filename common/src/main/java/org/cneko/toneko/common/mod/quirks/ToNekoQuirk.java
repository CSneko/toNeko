package org.cneko.toneko.common.mod.quirks;

import org.cneko.toneko.common.quirks.Quirk;

public abstract class ToNekoQuirk extends Quirk implements ModQuirk {
    public ToNekoQuirk(String id) {
        super(id);
    }

    abstract public int getInteractionValue(QuirkContext context);
}
