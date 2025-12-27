package org.cneko.toneko.common.mod.client.events;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.api.TickTasks;
import org.cneko.toneko.common.mod.client.ToNekoKeyBindings;
import org.cneko.toneko.common.mod.client.api.ClientEntityPoseManager;
import org.cneko.toneko.common.mod.client.screens.RouletteScreen;
import org.cneko.toneko.common.mod.util.EntityUtil;

import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class ClientTickEvent {
    public static void init(){
        ClientTickEvents.START_CLIENT_TICK.register(ClientTickEvent::onTick);
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvent::processKeyInput);
    }

    public static void processKeyInput(Minecraft client) {
        while (ToNekoKeyBindings.LIE_KEY.consumeClick()) {
            client.player.connection.sendUnsignedCommand("neko lie");
        }
        while (ToNekoKeyBindings.GET_DOWN_KEY.consumeClick()) {
            client.player.connection.sendUnsignedCommand("neko getDown");
        }
        while (ToNekoKeyBindings.RIDE_KEY.consumeClick()) {
            client.player.connection.sendUnsignedCommand("neko ride");
        }
        while (ToNekoKeyBindings.QUIRK_KEY.consumeClick()) {
            client.player.connection.sendUnsignedCommand("quirk gui");
        }
        while (ToNekoKeyBindings.SPEED_KEY.consumeClick()) {
            client.player.connection.sendUnsignedCommand("neko speed");
        }
        while (ToNekoKeyBindings.JUMP_KEY.consumeClick()) {
            client.player.connection.sendUnsignedCommand("neko jump");
        }
        while (ToNekoKeyBindings.VISION_KEY.consumeClick()) {
            client.player.connection.sendUnsignedCommand("neko vision");
        }
        while (ToNekoKeyBindings.RIDE_HEAD_KEY.consumeClick()) {
            client.player.connection.sendUnsignedCommand("neko rideHead");
        }
        while (ToNekoKeyBindings.ROULETTE_KEY.consumeClick()) {
            RouletteScreen.open();
        }
    }


    private static int tick = 0;
    public static void onTick(Minecraft client) {
        TickTasks.executeDefaultClient();
        // 寻找16格内的生物
        Player p = Minecraft.getInstance().player;
        if (p != null) {
            var entities = EntityUtil.getLivingEntitiesInRange(p,p.level(),16);
            tick++;
            if (tick==100){
                tick = 0;
                // 删除16格外实体的所有姿势
                ClientEntityPoseManager.poseMap.entrySet().removeIf(entry -> {
                    Entity entity = entry.getKey();
                    return entity == null || entity.distanceTo(p) > 16;
                });
            }
            // 如果是被骑乘的玩家，并且潜行，则取消骑乘
            if(p.isShiftKeyDown()){
                // 1%的概率取消骑乘
                if(Math.random() < 0.01){
                    p.getPassengers().forEach(Entity::stopRiding);
                }
            }
        }
    }
}
