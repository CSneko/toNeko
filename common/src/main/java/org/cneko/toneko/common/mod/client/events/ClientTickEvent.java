package org.cneko.toneko.common.mod.client.events;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.api.TickTasks;
import org.cneko.toneko.common.mod.client.ToNekoKeyBindings;
import org.cneko.toneko.common.mod.client.api.ClientEntityPoseManager;
import org.cneko.toneko.common.mod.client.screens.NekoInfoScreen;
import org.cneko.toneko.common.mod.client.screens.RouletteScreen;
import org.cneko.toneko.common.mod.client.screens.ToNekoHubScreen;
import org.cneko.toneko.common.mod.packets.interactives.DismountPassengerPayload;
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
        while (ToNekoKeyBindings.NEKO_INFO_KEY.consumeClick()) {
            NekoInfoScreen.open();
        }
        while (ToNekoKeyBindings.DISMOUNT_PASSENGER_KEY.consumeClick()) {
            var player = client.player;
            if (player != null && !player.getPassengers().isEmpty()) {
                ClientPlayNetworking.send(new DismountPassengerPayload());
            }
        }
        while (ToNekoKeyBindings.TONEKO_MANAGEMENT_KEY.consumeClick()) {
            client.player.connection.sendUnsignedCommand("toneko gui");
        }
        while (ToNekoKeyBindings.HUB_KEY.consumeClick()) {
            ToNekoHubScreen.open();
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
        }
    }
}
