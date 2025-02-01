package org.cneko.toneko.common.mod.events;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.cneko.toneko.common.api.TickTasks;
import org.cneko.toneko.common.mod.advencements.ToNekoCriteria;

public class CommonPlayerTickEvent {
    private static final int SLOW_TICK_TIMES = 100;
    private static int tickTimes = 0;
    public static void startTick(MinecraftServer server) {
        TickTasks.executeDefault();
        if(tickTimes++ >= SLOW_TICK_TIMES){
            tickTimes = 0;
            // 触发进度
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                ToNekoCriteria.NEKO_LV100.trigger(p);
            }
        }
    }
}
