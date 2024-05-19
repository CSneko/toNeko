package org.cneko.toneko.fabric;

import net.minecraft.server.MinecraftServer;

import java.util.logging.Logger;

public class ModMeta {
    public static ModMeta instance = new ModMeta();
    public static final String MOD_ID = "toneko";
    public static final String MOD_NAME = "ToNeko";
    private MinecraftServer server;
    public void setServer(MinecraftServer server){
        this.server = server;
    }
    public MinecraftServer getServer(){
        return this.server;
    }
}
