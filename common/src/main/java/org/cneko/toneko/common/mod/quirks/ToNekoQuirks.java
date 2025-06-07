package org.cneko.toneko.common.mod.quirks;

public class ToNekoQuirks {
    public static final CaressQuirk CARESS = new CaressQuirk("caress");
    public static final CrystalNekoQuirk CRYSTAL_NEKO = new CrystalNekoQuirk();
    public static final ZakoQuirk ZAKO = new ZakoQuirk();

    public static void init(){
        QuirkRegister.register(CARESS);
        QuirkRegister.register(CRYSTAL_NEKO);
        QuirkRegister.register(ZAKO);
    }
}
