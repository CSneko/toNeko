package org.cneko.toneko.common.mod.events;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.mod.quirks.ModQuirk;

public class CommonWorldEvent {
    public static void onWorldUnLoad(MinecraftServer minecraftServer, ServerLevel serverLevel) {
        NekoQuery.NekoData.saveAll();
    }

    public static void onWeatherChange(ServerLevel serverLevel, int clearTime, int weatherTime, boolean isRaining, boolean isThundering) {
        var players = serverLevel.players();
        for (var player : players){
            for (var quirk: player.getNeko().getQuirks()){
                if (quirk instanceof ModQuirk mq){
                    mq.onWeatherChange(player,serverLevel,clearTime,weatherTime,isRaining,isThundering);
                }
            }
        }
    }
}
