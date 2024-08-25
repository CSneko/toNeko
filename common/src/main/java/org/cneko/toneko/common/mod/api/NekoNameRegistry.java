package org.cneko.toneko.common.mod.api;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NekoNameRegistry {
    public static final CopyOnWriteArrayList<String> NEKO_NAMES = new CopyOnWriteArrayList<>();

    public static void register(String name){
        NEKO_NAMES.add(name);
    }
    public static void register(Collection<String> names){
        NEKO_NAMES.addAll(names);
    }
    public static String getRandomName(){
        return NEKO_NAMES.get((int)(Math.random()* NEKO_NAMES.size()));
    }
}
