package org.cneko.toneko.common.mod.mixin;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.cneko.toneko.common.mod.api.EntityPoseManager;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.misc.mixininterface.SlowTickable;
import org.cneko.toneko.common.mod.packets.EntityPosePayload;
import org.cneko.toneko.common.mod.packets.NekoInfoSyncPayload;
import org.cneko.toneko.common.mod.packets.PlayerLeadByPlayerPayload;
import org.cneko.toneko.common.mod.quirks.Quirk;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(Player.class)
public abstract class PlayerEntityMixin implements INeko, Leashable, SlowTickable {

    @Shadow private boolean reducedDebugInfo;

    @Shadow public abstract void remove(Entity.RemovalReason reason);

    // ------- 需要死亡不重置 --------
    @Unique
    boolean toneko$isNeko = false;
    @Unique
    float toneko$nekoLevel = 0;
    @Unique
    float toneko$nekoEnergy = 0;
    @Unique
    Map<UUID,Owner> toneko$owners = new HashMap<>();
    @Unique
    List<BlockedWord> toneko$blockedWords = new ArrayList<>();
    @Unique
    String toneko$nickName = "";
    @Unique
    List<Quirk> toneko$quirks = new ArrayList<>();
    // ---------------------------

    @Unique
    short toneko$tick = 20;

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
        Player player = (Player)(Object)this;
        Leashable.tickLeash(player);
        if (toneko$tick++>=20) {
            toneko$slowTick();
            toneko$tick = 0;
        }
        if (toneko$tick %2 == 0){
            if (player instanceof ServerPlayer sp){
                Pose pose = EntityPoseManager.getPose(player);
                boolean status;
                if (pose == null){
                    status = false;
                    pose = Pose.STANDING;
                }else {
                    status = true;
                }
                ServerPlayNetworking.send(sp,new EntityPosePayload(pose,player.getUUID().toString(),status));
            }
        }

    }


    @Override
    public void toneko$slowTick() {
        if ((Object)this instanceof ServerPlayer sp){
            // 同步信息给玩家
            toneko$syncNekoInfo(sp);
            this.serverNekoSlowTick();
            this.updateNekoLevelModifiers();
        }
    }

    @Unique
    private void toneko$syncNekoInfo(ServerPlayer sp){
        ServerPlayNetworking.send(sp,new NekoInfoSyncPayload(this.getNekoEnergy()));
    }

    @Override
    public boolean isNeko() {
        return toneko$isNeko;
    }

    @Override
    public void setNeko(boolean isNeko) {
        toneko$isNeko = isNeko;
    }

    @Override
    public float getNekoLevel() {
        return toneko$nekoLevel;
    }

    @Override
    public void setNekoLevel(float level) {
        toneko$nekoLevel = level;
    }

    @Override
    public float getNekoEnergy() {
        return toneko$nekoEnergy;
    }

    @Override
    public void setNekoEnergy(float energy) {
        toneko$nekoEnergy = energy;
    }

    @Override
    public Map<UUID, Owner> getOwners() {
        return toneko$owners;
    }

    @Override
    public List<BlockedWord> getBlockedWords() {
        return toneko$blockedWords;
    }

    @Override
    public @NotNull String getNickName() {
        return toneko$nickName;
    }

    @Override
    public List<Quirk> getQuirks() {
        return toneko$quirks;
    }
    @Override
    public void setNickName(@NotNull String nickName) {
        toneko$nickName = nickName;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    public void addAdditionalSaveData(CompoundTag compound, CallbackInfo ci) {
        this.saveNekoNBTData(compound);
    }
    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    public void readAdditionalSaveData(CompoundTag compound, CallbackInfo ci) {
        this.loadNekoNBTData(compound);
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
