package org.cneko.toneko.common.mod.quirks;

import org.cneko.toneko.common.quirks.QuirkRegister;

public class ToNekoQuirks {
    public static final CaressQuirk CARESS = new CaressQuirk("caress");

    public static void init(){
        QuirkRegister.register(CARESS);
    }
}
