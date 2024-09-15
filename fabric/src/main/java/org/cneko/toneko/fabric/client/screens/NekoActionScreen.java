package org.cneko.toneko.fabric.client.screens;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.cneko.toneko.common.mod.client.api.ClientEntityPoseManager;
import org.cneko.toneko.common.mod.packets.interactives.FollowOwnerPayload;
import org.cneko.toneko.common.mod.packets.interactives.NekoPosePayload;
import org.cneko.toneko.common.mod.packets.interactives.RideEntityPayload;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.cneko.toneko.fabric.entities.NekoEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class NekoActionScreen extends InteractionScreen implements INekoScreen{
    private final NekoEntity neko;

    public NekoActionScreen(@NotNull NekoEntity neko, @Nullable Screen lastScreen) {
        super(Component.empty(), lastScreen, () -> getButtonBuilders(neko));
        this.neko = neko;
    }

    public static Map<String, Button.Builder> getButtonBuilders(NekoEntity neko) {
        Map<String, Button.Builder> builders = new LinkedHashMap<>();

        builders.put("screen.toneko.neko_entity_interactive.button.follow",Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.follow"),(btn)->{
            ClientPlayNetworking.send(new FollowOwnerPayload(neko.getUUID().toString()));
            neko.followOwner(Minecraft.getInstance().player);
        }));

        builders.put("screen.toneko.neko_entity_interactive.button.ride",Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.ride"),(btn)->{
            // 让猫娘骑在最近的实体身上
            LivingEntity entity = EntityUtil.findNearestEntityInRange(neko, Minecraft.getInstance().player.level(),NekoEntity.DEFAULT_RIDE_RANGE);
            if (entity != null){
                if (neko.isSitting()){
                    neko.stopRiding();
                }else {
                    neko.startRiding(entity, true);
                }
                // 向服务器发包
                ClientPlayNetworking.send(new RideEntityPayload(neko.getUUID().toString(),entity.getUUID().toString()));
            }
        }));

        builders.put("screen.toneko.neko_entity_interactive.button.lie",Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.lie"),(btn)->{
            // 把猫娘设置为躺
            if (ClientEntityPoseManager.contains(neko)){
                ClientEntityPoseManager.remove(neko);
                neko.setPose(Pose.STANDING);
            }else {
                ClientEntityPoseManager.setPose(neko, Pose.SLEEPING);
                neko.setPose(Pose.SLEEPING);
            }
            ClientPlayNetworking.send(new NekoPosePayload(Pose.SLEEPING,neko.getUUID().toString(),true));
        }));

        builders.put("screen.toneko.neko_entity_interactive.button.get_down",Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.get_down"),(btn)->{
            // 把猫娘设置为趴
            if (ClientEntityPoseManager.contains(neko)){
                ClientEntityPoseManager.remove(neko);
                neko.setPose(Pose.STANDING);
            }else {
                ClientEntityPoseManager.setPose(neko, Pose.SWIMMING);
                neko.setPose(Pose.SWIMMING);
            }
            ClientPlayNetworking.send(new NekoPosePayload(Pose.SWIMMING,neko.getUUID().toString(),true));
        }));
        return builders;
    }

    public static void open(NekoEntity neko) {
        Minecraft.getInstance().setScreen(new NekoActionScreen(neko, Minecraft.getInstance().screen));
    }

    @Override
    public NekoEntity getNeko() {
        return this.neko;
    }
}
