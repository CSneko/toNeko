package org.cneko.toneko.neoforge.fabric;

import net.minecraft.server.MinecraftServer;

public class ModMeta {
    public static ModMeta INSTANCE = new ModMeta();
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
