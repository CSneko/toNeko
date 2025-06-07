package org.cneko.toneko.common.mod.events;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.cneko.toneko.common.api.TickTasks;
import org.cneko.toneko.common.mod.advencements.ToNekoCriteria;
import org.cneko.toneko.common.mod.quirks.ModQuirk;
import org.cneko.toneko.common.mod.quirks.Quirk;

public class CommonPlayerEvent {
    private static final int SLOW_TICK_TIMES = 100;
    private static int tickTimes = 0;
    public static void startTick(MinecraftServer server) {
        TickTasks.executeDefault();
        if(tickTimes++ >= SLOW_TICK_TIMES){
            tickTimes = 0;
            // 触发进度
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                ToNekoCriteria.NEKO_LV100.trigger(p);
                // 处理玩家潜行
                if(p.isShiftKeyDown()){
                    p.getPassengers().forEach(Entity::stopRiding);
                }
            }
        }
    }

    public static void startSleep(LivingEntity entity, BlockPos pos) {
        if(entity instanceof ServerPlayer player){
            for (Quirk quirk : player.getQuirks()){
                if(quirk instanceof ModQuirk modQuirk){
                    modQuirk.startSleep(player,pos);
                }
            }
        }
    }

    public static void stopSleep(LivingEntity entity, BlockPos pos) {
        if(entity instanceof ServerPlayer player){
            for (Quirk quirk : player.getQuirks()){
                if(quirk instanceof ModQuirk modQuirk){
                    modQuirk.stopSleep(player,pos);
                }
            }
        }
    }
}
