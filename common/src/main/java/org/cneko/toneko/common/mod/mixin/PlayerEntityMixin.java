package org.cneko.toneko.common.mod.mixin;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import org.cneko.toneko.common.mod.ai.NekoTriggerManager;
import org.cneko.toneko.common.mod.api.EntityPoseManager;
import org.cneko.toneko.common.mod.api.ExplorationLevelFactor;
import org.cneko.toneko.common.mod.api.NekoLevelRegistry;
import org.cneko.toneko.common.mod.api.PoseStateSync;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.misc.mixininterface.SlowTickable;
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
    int toneko$nekoAge = 0;
    @Unique
    CompoundTag toneko$nekoLevelFactorData = new CompoundTag();
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
    @Unique
    Set<String> toneko$visitedBiomes = new HashSet<>();
    // ---------------------------

    // 猫娘潜行
    @Unique
    boolean toneko$stealthActive = false;
    @Unique
    int toneko$stealthCooldown = 0; // 攻击后解除潜行的冷却 tick

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
        // 26.x：Leashable.tickLeash 仅适用于实现 Leashable 的实体，Player 不再实现该接口，移除调用
        if (toneko$tick++>=20) {
            toneko$slowTick();
            toneko$tick = 0;
        }
        // 姿势钉定不再轮询发包：服务端写入走原版 DATA_POSE 同步，
        // 本人客户端的预测对齐只在钉定/解除瞬间发一次包（见 PoseStateSync）；
        // 趴/躺时按 Shift 起身规则统一移至 LivingEntityMixin 双端处理。

        // 自然成长：幼年猫娘每秒成长1 tick，约10个游戏日成年
        if (player instanceof ServerPlayer && this.isNeko() && this.getNekoAge() < 0) {
            this.setNekoAge(this.getNekoAge() + 1);
        }

        // 猫娘潜行：能量消耗 + 冷却倒计时
        if (player instanceof ServerPlayer && toneko$stealthActive && this.isNeko() && player.isCrouching()) {
            if (toneko$stealthCooldown > 0) {
                toneko$stealthCooldown--;
            } else {
                double cost = player.getDeltaMovement().horizontalDistance() > 0.01 ? 0.8 : 0.2;
                this.setNekoEnergy((float) Math.max(0, this.getNekoEnergy() - cost));
                if (this.getNekoEnergy() <= 0) {
                    toneko$stealthActive = false;
                }
            }
        } else if (toneko$stealthCooldown > 0) {
            toneko$stealthCooldown--;
        }

    }


    @Override
    public void toneko$slowTick() {
        if ((Object)this instanceof ServerPlayer sp){
            // 同步信息给玩家
            toneko$syncNekoInfo(sp);
            // 检查新群系探索
            if (this.isNeko()) {
                checkBiomeExploration(sp);
            }
            this.serverNekoSlowTick();
            this.updateNekoLevelModifiers();
        }
    }

    @Unique
    private void toneko$syncNekoInfo(ServerPlayer sp){
        ServerPlayNetworking.send(sp, new NekoInfoSyncPayload(
                this.getNekoEnergy(),
                this.getMaxNekoEnergy(),
                this.getNekoLevelFactorRaw("interaction"),
                this.getNekoLevelFactorRaw("combat"),
                this.getNekoLevelFactorRaw("base"),
                this.getNekoLevelFactorRaw("exploration"),
                this.getNekoLevelFactorRaw("fishing"),
                this.getNekoLevelFactorRaw("homestead"),
                this.isNeko(),
                this.getNekoAge()
        ));
    }

    @Unique
    private void checkBiomeExploration(ServerPlayer sp) {
        ((ServerLevel) sp.level()).getBiome(sp.blockPosition()).unwrapKey().ifPresent(key -> {
            String biomeId = key.identifier().toString();
            if (!toneko$visitedBiomes.contains(biomeId)) {
                toneko$visitedBiomes.add(biomeId);
                double xp = ExplorationLevelFactor.getBiomeXp(
                        ((ServerLevel) sp.level()).getBiome(sp.blockPosition()));
                NekoLevelRegistry.exploration().addRaw((INeko)(Object)this, xp);
            }
        });
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
    public int getNekoAge() {
        return toneko$nekoAge;
    }

    @Override
    public void setNekoAge(int age) {
        toneko$nekoAge = age;
    }

    @Override
    public int getMaxAge() {
        return 240000; // 10 game days for players
    }

    @Override
    @Deprecated
    public void setNekoLevel(float level) {
        // no-op — retained for binary compatibility only
    }

    @Override
    public CompoundTag getNekoLevelFactorData() {
        return toneko$nekoLevelFactorData;
    }

    @Override
    public void setNekoLevelFactorData(CompoundTag data) {
        toneko$nekoLevelFactorData = data;
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
    public Set<String> getVisitedBiomes() {
        return toneko$visitedBiomes;
    }
    @Override
    public void setVisitedBiomes(Set<String> biomes) {
        toneko$visitedBiomes = new HashSet<>(biomes);
    }
    @Override
    public void addVisitedBiome(String biomeKey) {
        toneko$visitedBiomes.add(biomeKey);
    }
    @Override
    public void setNickName(@NotNull String nickName) {
        toneko$nickName = nickName;
    }

    // 26.x：实体序列化改为 ValueInput/ValueOutput；自有数据块经 NbtBridge 以 CompoundTag 编解码
    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output, CallbackInfo ci) {
        CompoundTag data = new CompoundTag();
        this.saveNekoNBTData(data);
        org.cneko.toneko.common.mod.util.NbtBridge.store(data, output);
    }
    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input, CallbackInfo ci) {
        this.loadNekoNBTData(org.cneko.toneko.common.mod.util.NbtBridge.read(input));
    }

    // 26.x：LivingEntity#hurt 拆分为 hurtServer/hurtClient（服务端路径带 ServerLevel）
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    public void hurt(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player)(Object)this;
        if(source.getEntity() instanceof Player holder){
            ItemStack stack = holder.getMainHandItem();
            if (stack.is(Items.LEAD) && !((Leashable) player).isLeashed()){
                // 栓住玩家
                ((Leashable) player).setLeashedTo(holder, true);
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

    // 26.x：interactOn 增加 Vec3 命中点参数
    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    public void interactOn(Entity entityToInteractOn, InteractionHand hand,
                           net.minecraft.world.phys.Vec3 hitPoint, CallbackInfoReturnable<InteractionResult> cir) {
        if(entityToInteractOn instanceof INeko neko){
            Player player = (Player)(Object)this;
            ItemStack itemStack = player.getItemInHand(hand);
            if (itemStack.is(Items.BUCKET) && !neko.isNekoBaby()) {
                player.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
                ItemStack itemStack2 = ItemUtils.createFilledResult(itemStack, player, Items.MILK_BUCKET.getDefaultInstance());
                // 显示来源
                itemStack2.set(DataComponents.LORE, new ItemLore(Collections.singletonList(Component.translatable("item.minecraft.milk_bucket.source", neko.getEntity().getName()).withStyle(ChatFormatting.LIGHT_PURPLE))));
                player.setItemInHand(hand, itemStack2);
                cir.setReturnValue(player.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
                cir.cancel();
            }
            // 空手摸头：仅服务端触发型反应（客户端 player 恒为 LocalPlayer，instanceof ServerPlayer 必为 false，
            // 严格排除客户端预测路径，避免同一右键在双端各触发一次）
            if (itemStack.isEmpty() && player instanceof ServerPlayer sp
                    && entityToInteractOn instanceof NekoEntity nekoEntity) {
                NekoTriggerManager.onPlayerPet((ServerLevel) sp.level(), nekoEntity, sp);
            }
        }
    }

    // ---- 猫娘潜行 ----

    /**
     * 玩家姿势钉定重申（双端）。
     *
     * Player.tick 在 super.tick() 之后调用 updatePlayerPose() 重算姿势，
     * 因此必须在它的所有返回点把钉定姿势写回去，保证「钉定」是该 tick
     * 内最后一次姿势写入 —— DATA_POSE 同步出去的值才是钉定值。
     * 写入走普通 setPose，不取消/劫持任何原版逻辑，兼容其它动作模组。
     *
     * 趴/躺钉定时按 Shift 起身的双端同规则也在此处理：
     * 客户端本地解除预测钉定、服务端解钉并通知本人客户端，
     * 两端判定条件一致，起身瞬间不会出现姿势来回闪烁。
     */
    @Inject(method = "updatePlayerPose", at = @At("RETURN"))
    public void toneko$reapplyPinnedPose(CallbackInfo ci) {
        Player self = (Player)(Object)this;
        boolean clientSide = self.level().isClientSide();

        Pose pinned = clientSide
                ? org.cneko.toneko.common.mod.client.api.ClientPoseState.applyTo(self)
                : EntityPoseManager.getNullablePose(self);
        if (pinned == null || self.isPassenger()) {
            return;
        }

        // 与服务端同规则：趴(SWIMMING)/躺(SLEEPING)时按 Shift 起身
        if ((pinned == Pose.SWIMMING || pinned == Pose.SLEEPING) && self.isShiftKeyDown()) {
            if (clientSide) {
                org.cneko.toneko.common.mod.client.api.ClientPoseState.deactivate();
            } else {
                PoseStateSync.unpin((ServerPlayer) self);
            }
            return;
        }

        self.setPose(pinned);
    }

    @Override
    public void setStealthActive(boolean active) {
        toneko$stealthActive = active;
    }
    @Override
    public boolean isStealthActive() {
        return toneko$stealthActive;
    }
    /** 攻击后暂时解除潜行 5 秒 */
    @Override
    public void breakStealth() {
        if (toneko$stealthActive) {
            toneko$stealthCooldown = 100;
        }
    }

    /** 潜行状态下怪物不将猫娘视为敌人。潜行冷却期间恢复可见。 */
    @Inject(method = "canBeSeenAsEnemy", at = @At("HEAD"), cancellable = true)
    private void onCanBeSeenAsEnemy(CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player)(Object)this;
        if (toneko$stealthActive && toneko$stealthCooldown <= 0 && self.isCrouching() && this.isNeko()) {
            cir.setReturnValue(false);
        }
    }

}
