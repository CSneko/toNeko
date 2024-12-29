package org.cneko.toneko.common.mod.mixin;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
                if(player.level().isClientSide()){
                    // 如果是在客户端，就发送给服务端
                    ClientPlayNetworking.send(new PlayerLeadByPlayerPayload(holder.getUUID().toString(), player.getUUID().toString()));
                }
                cir.setReturnValue(false);
                cir.cancel();
            }
        }

    }
}
