package org.cneko.toneko.fabric.client.screens;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.packets.interactives.FollowOwnerPayload;
import org.cneko.toneko.common.mod.packets.interactives.GiftItemPayload;
import org.cneko.toneko.fabric.entities.NekoEntity;
import org.jetbrains.annotations.NotNull;

public class NekoEntityInteractiveScreen extends DynamicScreen{
    private final @NotNull NekoEntity neko;
    public NekoEntityInteractiveScreen(@NotNull NekoEntity neko) {
        super(Component.empty());
        this.neko = neko;
    }

    @Override
    protected void init() {
        super.init();
        // 仅在屏幕x轴70%外的屏幕中绘制
        int x = (int) (this.width * 0.7);
        int y = (int) (this.height * 0.1);
        int buttonWidth = (int)(this.width * 0.2);
        int buttonHeight = (int)(this.height * 0.08);
        int buttonBound = (int)(this.height * 0.1);

        Button giftButton = Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.gift"),(btn)->{
            ItemStack stack = Minecraft.getInstance().player.getMainHandItem();
            int slot = Minecraft.getInstance().player.getInventory().findSlotMatchingItem(stack);
            if(!stack.isEmpty()){
                ClientPlayNetworking.send(new GiftItemPayload(neko.getUUID().toString(), slot));
            }
        }).size(buttonWidth,buttonHeight).pos(x,y).build();
        giftButton.setTooltip(Tooltip.create(Component.translatable("screen.toneko.neko_entity_interactive.button.gift.des")));
        this.addRenderableWidget(giftButton);

        y += buttonHeight+buttonBound;

        Button followButton = Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.follow"),(btn)->{
            ClientPlayNetworking.send(new FollowOwnerPayload(neko.getUUID().toString()));
            neko.followOwner(Minecraft.getInstance().player);
        }).size(buttonWidth,buttonHeight).pos(x,y).build();
        followButton.setTooltip(Tooltip.create(Component.translatable("screen.toneko.neko_entity_interactive.button.follow.des")));
        this.addRenderableWidget(followButton);

        y += buttonHeight+buttonBound;
    }
}
