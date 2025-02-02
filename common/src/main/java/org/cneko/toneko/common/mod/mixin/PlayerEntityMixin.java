package org.cneko.toneko.common.mod.mixin;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.packets.PlayerLeadByPlayerPayload;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;

@Mixin(Player.class)
public abstract class PlayerEntityMixin implements INeko, Leashable {
    @Shadow public abstract void playNotifySound(SoundEvent sound, SoundSource source, float volume, float pitch);

    @Unique
    private LeashData leashData;
    @Override
    public LivingEntity getEntity() {
        return (Player)(Object) this;
    }

    @Override
    public boolean isPlayer() {
        return true;
    }

    @Nullable
    @Override
    public LeashData getLeashData() {
        return leashData;
    }

    @Override
    public void setLeashData(@Nullable Leashable.LeashData leashData) {
        this.leashData = leashData;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        Leashable.tickLeash((Player)(Object)this);
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    public void hurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player)(Object)this;
        if(source.getEntity() instanceof Player holder){
            ItemStack stack = holder.getMainHandItem();
            if (stack.is(Items.LEAD) && !player.isLeashed()){
                // 栓住玩家
                player.setLeashedTo(holder, true);
                // 减少栓绳
                holder.getMainHandItem().setCount(holder.getMainHandItem().getCount() - 1);
                // 在服务端运行的话呢同时发给客户端
                if (player instanceof ServerPlayer sp) {
                    ServerPlayNetworking.send(sp, new PlayerLeadByPlayerPayload(holder.getUUID().toString(), player.getUUID().toString()));
                    ServerPlayNetworking.send((ServerPlayer) holder,new PlayerLeadByPlayerPayload(holder.getUUID().toString(),player.getUUID().toString()));
                }
                cir.setReturnValue(false);
                cir.cancel();
            }
        }

    }

    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    public void interactOn(Entity entityToInteractOn, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if(entityToInteractOn instanceof INeko neko){
            Player player = (Player)(Object)this;
            ItemStack itemStack = player.getItemInHand(hand);
            if (itemStack.is(Items.BUCKET) && !neko.getEntity().isBaby()) {
                player.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
                ItemStack itemStack2 = ItemUtils.createFilledResult(itemStack, player, Items.MILK_BUCKET.getDefaultInstance());
                // 显示来源
                itemStack2.set(DataComponents.LORE, new ItemLore(Collections.singletonList(Component.translatable("item.minecraft.milk_bucket.source", neko.getEntity().getName()).withStyle(ChatFormatting.LIGHT_PURPLE))));
                player.setItemInHand(hand, itemStack2);
                cir.setReturnValue(InteractionResult.sidedSuccess(player.level().isClientSide));
                cir.cancel();
            }
        }
    }
}
