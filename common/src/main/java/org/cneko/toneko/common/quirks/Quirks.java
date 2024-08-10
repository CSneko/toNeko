package org.cneko.toneko.common.quirks;

public class Quirks {
    public static CaressQuirk CARESS = new CaressQuirk("caress");

    public static void init(){
        QuirkRegister.register(CARESS);
    }
}
