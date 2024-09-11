package org.cneko.toneko.fabric.client.screens;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.api.EntityPoseManager;
import org.cneko.toneko.common.mod.client.api.ClientEntityPoseManager;
import org.cneko.toneko.common.mod.packets.interactives.FollowOwnerPayload;
import org.cneko.toneko.common.mod.packets.interactives.GiftItemPayload;
import org.cneko.toneko.common.mod.packets.interactives.NekoPosePayload;
import org.cneko.toneko.common.mod.packets.interactives.RideEntityPayload;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.cneko.toneko.fabric.entities.NekoEntity;
import org.jetbrains.annotations.NotNull;

public class NekoEntityInteractiveScreen extends DynamicScreen{
    private final @NotNull NekoEntity neko;
    public NekoEntityInteractiveScreen(@NotNull NekoEntity neko) {
        super(Component.empty());
        this.neko = neko;
    }

    @Override
    public void init() {
        super.init();
        // 仅在屏幕x轴70%外的屏幕中绘制
        int x = (int) (this.width * 0.7);
        int y = (int) (this.height * 0.1);
        int buttonWidth = (int)(this.width * 0.25);
        int buttonHeight = (int)(this.height * 0.08);
        int buttonBound = (int)(this.height * 0.15);

        // -------------------------------------------
        Button giftButton = Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.gift"),(btn)->{
            ItemStack stack = Minecraft.getInstance().player.getMainHandItem();
            int slot = Minecraft.getInstance().player.getInventory().findSlotMatchingItem(stack);
            if(!stack.isEmpty()){
                ClientPlayNetworking.send(new GiftItemPayload(neko.getUUID().toString(), slot));
            }
        }).size(buttonWidth,buttonHeight).pos(x,y).build();
        giftButton.setTooltip(Tooltip.create(Component.translatable("screen.toneko.neko_entity_interactive.button.gift.des")));
        this.addRenderableWidget(giftButton);
        y += buttonBound;

        // --------------------------------------------
        Button followButton = Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.follow"),(btn)->{
            ClientPlayNetworking.send(new FollowOwnerPayload(neko.getUUID().toString()));
            neko.followOwner(Minecraft.getInstance().player);
        }).size(buttonWidth,buttonHeight).pos(x,y).build();
        followButton.setTooltip(Tooltip.create(Component.translatable("screen.toneko.neko_entity_interactive.button.follow.des")));
        this.addRenderableWidget(followButton);
        y += buttonBound;

        // --------------------------------------------
        Button rideButton = Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.ride"),(btn)->{
            // 让猫娘骑在3格内最近的实体身上
            LivingEntity entity = EntityUtil.findNearestEntityInRange(neko, Minecraft.getInstance().player.level(),3.0f);
            if (entity != null){
                if (neko.isSitting()){
                    neko.stopRiding();
                }else {
                    neko.startRiding(entity, true);
                }
                // 向服务器发包
                ClientPlayNetworking.send(new RideEntityPayload(neko.getUUID().toString(),entity.getUUID().toString()));
            }
        }).size(buttonWidth,buttonHeight).pos(x,y).build();
        rideButton.setTooltip(Tooltip.create(Component.translatable("screen.toneko.neko_entity_interactive.button.ride.des")));
        this.addRenderableWidget(rideButton);
        y += buttonBound;

        // -------------------------------------------
        Button lieButton = Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.lie"),(btn)->{
            // 把猫娘设置为躺
            if (ClientEntityPoseManager.contains(neko)){
                ClientEntityPoseManager.remove(neko);
                neko.setPose(Pose.STANDING);
            }else {
                ClientEntityPoseManager.setPose(neko, Pose.SLEEPING);
            }
            ClientPlayNetworking.send(new NekoPosePayload(Pose.SLEEPING,neko.getUUID().toString(),true));
        }).size(buttonWidth,buttonHeight).pos(x,y).build();
        lieButton.setTooltip(Tooltip.create(Component.translatable("screen.toneko.neko_entity_interactive.button.lie.des")));
        this.addRenderableWidget(lieButton);


//        // ------------------------------------------
//        Button getDownButton = Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.get_down"),(btn)->{
//            // 把猫娘设置为趴
//            if (ClientEntityPoseManager.contains(neko)){
//                ClientEntityPoseManager.remove(neko);
//            }else {
//                ClientEntityPoseManager.setPose(neko, Pose.SWIMMING);
//            }
//            ClientPlayNetworking.send(new NekoPosePayload(Pose.SWIMMING,neko.getUUID().toString(),true));
//        }).size(buttonWidth,buttonHeight).pos(x,y).build();
//        getDownButton.setTooltip(Tooltip.create(Component.translatable("screen.toneko.neko_entity_interactive.button.get_down.des")));
//        this.addRenderableWidget(getDownButton);
//        y += buttonBound;


    }
}
