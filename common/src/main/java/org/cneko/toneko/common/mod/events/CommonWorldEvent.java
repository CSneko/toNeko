package org.cneko.toneko.common.mod.events;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.cneko.toneko.common.api.NekoQuery;

public class CommonWorldEvent {
    public static void onWorldUnLoad(MinecraftServer minecraftServer, ServerLevel serverLevel) {
        NekoQuery.NekoData.saveAll();
    }

}
