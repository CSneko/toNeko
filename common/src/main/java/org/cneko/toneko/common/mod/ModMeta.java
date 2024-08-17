package org.cneko.toneko.common.mod;

import net.minecraft.server.MinecraftServer;

public class ModMeta {
    public static ModMeta INSTANCE = new ModMeta();
    private MinecraftServer server;
    public void setServer(MinecraftServer server){
        this.server = server;
    }
    public MinecraftServer getServer(){
        return this.server;
    }
}
