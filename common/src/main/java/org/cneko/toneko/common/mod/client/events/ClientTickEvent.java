package org.cneko.toneko.common.mod.client.events;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.api.TickTasks;
import org.cneko.toneko.common.mod.client.api.ClientEntityPoseManager;
import org.cneko.toneko.common.mod.client.ToNekoKeyBindings;
import org.cneko.toneko.common.mod.client.screens.RouletteScreen;

import java.util.List;

import static org.cneko.toneko.common.Bootstrap.MODID;

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
        while (ToNekoKeyBindings.ROULETTE_KEY.consumeClick()) {
            Minecraft.getInstance().setScreen(new RouletteScreen(getRouletteActions()));
        }
    }

    private static List<RouletteScreen.IRouletteAction> getRouletteActions() {
        return List.of(
                new RouletteScreen.IRouletteAction() {
                    @Override
                    public ResourceLocation getIcon() {
                        return ResourceLocation.withDefaultNamespace("textures/item/barrier.png");
                    }
                    @Override
                    public Component getName() {
                        return Component.literal("关闭");
                    }
                    @Override
                    public void rouletteAction() {
                        Minecraft.getInstance().setScreen(null);
                    }
                },
                new RouletteScreen.IRouletteAction() {
                    @Override
                    public ResourceLocation getIcon() {
                        return ResourceLocation.withDefaultNamespace("textures/mob_effect/speed.png");
                    }

                    @Override
                    public Component getName() {
                        return Component.literal("速度");
                    }

                    @Override
                    public void rouletteAction() {
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.connection.sendUnsignedCommand("neko speed");
                        }
                    }
                },
                new RouletteScreen.IRouletteAction() {
                    @Override
                    public ResourceLocation getIcon() {
                        return ResourceLocation.withDefaultNamespace("textures/mob_effect/jump_boost.png");
                    }
                    @Override
                    public Component getName() {
                        return Component.literal("跳跃提升");
                    }
                    @Override
                    public void rouletteAction() {
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.connection.sendUnsignedCommand("neko jump");
                        }
                    }
                },
                new RouletteScreen.IRouletteAction() {
                    @Override
                    public ResourceLocation getIcon() {
                        return ResourceLocation.withDefaultNamespace("textures/mob_effect/night_vision.png");
                    }
                    @Override
                    public Component getName() {
                        return Component.literal("夜视");
                    }
                    @Override
                    public void rouletteAction() {
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.connection.sendUnsignedCommand("neko vision");
                        }
                    }
                },
                new RouletteScreen.IRouletteAction() {
                    @Override
                    public ResourceLocation getIcon() {
                        return ResourceLocation.withDefaultNamespace("textures/item/leather.png");
                    }
                    @Override
                    public Component getName() {
                        return Component.literal("躺下");
                    }
                    @Override
                    public void rouletteAction() {
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.connection.sendUnsignedCommand("neko lie");
                        }
                    }
                },
                new RouletteScreen.IRouletteAction() {
                    @Override
                    public ResourceLocation getIcon() {
                        return ResourceLocation.withDefaultNamespace("textures/item/pink_dye.png");
                    }
                    @Override
                    public Component getName() {
                        return Component.literal("趴下");
                    }
                    @Override
                    public void rouletteAction() {
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.connection.sendUnsignedCommand("neko getDown");
                        }
                    }
                },
                new RouletteScreen.IRouletteAction() {
                    @Override
                    public ResourceLocation getIcon() {
                        return ResourceLocation.withDefaultNamespace("textures/item/saddle.png");
                    }
                    @Override
                    public Component getName() {
                        return Component.literal("骑乘");
                    }
                    @Override
                    public void rouletteAction() {
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.connection.sendUnsignedCommand("neko ride");
                        }
                    }
                }
        );
    }

    public static void onTick(Minecraft client) {
        TickTasks.executeDefaultClient();
        Player p = Minecraft.getInstance().player;
        if (p != null) {
            if (ClientEntityPoseManager.contains(p)) {
                p.setPose(ClientEntityPoseManager.getPose(p));
            }

            // 如果是被骑乘的玩家，并且潜行，则取消骑乘
            if(p.isShiftKeyDown()){
                p.getPassengers().forEach(Entity::stopRiding);
            }
        }
    }
}
