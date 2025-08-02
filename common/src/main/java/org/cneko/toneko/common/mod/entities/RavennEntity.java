package org.cneko.toneko.common.mod.entities;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static org.cneko.toneko.common.mod.util.TextUtil.randomTranslatabledComponent;

public class RavennEntity extends NekoEntity{
    public static final List<String> MOE_TAGS = List.of(
            "tsundere",
            "yuri"
    );

    public RavennEntity(EntityType<? extends NekoEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public @Nullable NekoEntity getBreedOffspring(ServerLevel level, INeko otherParent) {
        return new RavennEntity(this.getType(), level);
    }

    @Override
    public List<String> getMoeTags() {
        return new ArrayList<>(MOE_TAGS);
    }

    @Override
    public @Nullable Component getCustomName() {
        return Component.literal("Ravenn591");
    }

    @Override
    public void tick() {
        super.tick();
        // 1/500概率发病
        if (this.getRandom().nextInt(500) == 0) {
            // 对周围玩家发送消息
            for (Player player : EntityUtil.getPlayersInRange(this,this.level(),16)) {
                sendIdleMessageToPlayer(player);
            }
        }
    }

    @Override
    public void sendHurtMessageToPlayer(Player player) {
        if (player instanceof ServerPlayer sp){
            var name = this.getName();
            player.sendSystemMessage(randomTranslatabledComponent(random,"message.toneko.neko.on_hurt.ravenn",5,name));
        }
    }
    public void sendIdleMessageToPlayer(Player player) {
        if (player instanceof ServerPlayer sp){
            var name = this.getName();
            player.sendSystemMessage(randomTranslatabledComponent(random,"message.toneko.neko.idle.ravenn",5,name));
        }
    }

    @Override
    public boolean isLikedItem(ItemStack stack) {
        return super.isLikedItem(stack) || stack.is(Items.END_ROD);
    }

    @Override
    public String getSkin() {
        return "ravenn591";
    }

    @Override
    public boolean canMove() {
        return super.canMove() && !this.isSleeping();
    }

    public static AttributeSupplier.Builder createRavennAttributes() {
        return NekoEntity.createNekoAttributes();
    }
}
