package org.cneko.toneko.common.mod.entities.boss.mouflet;

import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.effects.ToNekoEffects;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.entities.boss.NekoBoss;
import org.cneko.toneko.common.mod.items.ToNekoItems;
import org.cneko.toneko.common.mod.misc.ToNekoAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 graph TD
    %% 战斗系统
    A[战斗开始] --> B{被动检测}
    B -->|半径32格有箱子| C[锁定所有箱子]
    B -->|检测玩家物品| D[偷窃机制]

    A --> E[攻击决策]
    E --> F{满足武器条件?}
    F -->|是| G[使用武器攻击]
    F -->|否| H[徒手攻击10点]
    G --> I{远程武器?}
    I -->|是| J[检查弹药并射击]
    I -->|否| K[近战攻击]

    L[主动技能CD] --> M{随机选择}
    M --> N[撒娇]
    N --> O["▪播放语音+文字+动作<br>▪减伤60%+清除debuff"]
    M --> P[魅惑]
    P --> Q["▪血条消失+停止攻击<br>▪潜行移动速度+30%"]

    %% 偷窃机制
    D --> R{偷窃条件}
    R -->|血量<80% CD5s| S[偷食物]
    R -->|血量<60% 武器伤害>10 CD20s| T[偷武器/工具]

    %% 防御系统
    U[受到伤害] --> V{单次>6伤害?}
    V -->|是| W[触发防御架势]
    W --> X["▪抗性IV(15s)<br>▪伤害反弹80%(10s)"]
    X --> Y[攻击者失去8饱食和50能量]
    V -->|否| Z[正常受伤]

    %% 收服系统
    AA[血量检测] --> AB{<30hp?}
    AB -->|是且猫薄荷≥10| AC[▪可收服状态<br>▪撒娇技能]
    AC --> AD[手持契约物品右击]
    AD --> AE["▪血量100→40<br>▪攻击10→3<br>▪体型3x→2x<br>▪速度3x→1.5x"]
    AE --> AF[收服]
    AB -->|是但未达标| AG[逃跑]

    %% 绝望状态
    AH[致命伤害] --> AI[触发绝望]
    AI --> AJ["▪不死图腾+回50hp<br>▪攻击+20 防御-10"]
    AJ --> AK["▪TNT×3.75爆炸"]
    AK --> AL[清空玩家饥饿]
    AL --> AM["▪持续清除buff+虚弱III"]
    AM --> AN[20秒后自爆]
    AN --> AO["▪TNT×9爆炸"]

    %% 宠物模式
    AF --> AP[▪行为与武备猫娘一致<br>▪额外的语音和文案]
```
 */
public class MoufletNekoBoss extends NekoEntity implements NekoBoss {
    public static final List<String> NEKO_SKINS = List.of("mouflet");
    public int eatenCatnip = 0; // 吃猫薄荷的次数
    private int defenseStanceTicks = 0; // 防御架势剩余tick数
    private int thornsTicks = 0;        // 反弹剩余tick数
    private boolean despairTriggered = false; // 是否触发了绝望状态
    private int despairTicks = 0; // 绝望状态剩余tick数
    private int skillCooldown = 0; // 技能冷却
    private int charmTicks = 0;    // 魅惑状态持续tick
    private int unhurtTime = 0; // 无伤时间计数器
    private int grabFlyTicks = 0; // 抱飞行剩余tick数
    private Player grabbedPlayer = null; // 抱起的玩家

    @Getter
    private boolean isCharmed = false; // 是否处于魅惑状态
    private MoufletAttackGoal attackGoal; // 攻击目标

    public static final EntityDataAccessor<Boolean> PET_MODE = SynchedEntityData.defineId(MoufletNekoBoss.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossEvent =
            new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);

    public MoufletNekoBoss(EntityType<? extends NekoEntity> entityType, Level level) {
        super(entityType, level);
        if (!level.isClientSide && !this.isPersistenceRequired() && !this.isPetMode()) {
            // 头盔、胸甲、护腿、靴子
            this.addItem(new ItemStack(net.minecraft.world.item.Items.DIAMOND_HELMET));
            this.addItem(new ItemStack(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE));
            this.addItem(new ItemStack(net.minecraft.world.item.Items.DIAMOND_LEGGINGS));
            this.addItem(new ItemStack(net.minecraft.world.item.Items.DIAMOND_BOOTS));
            if (random.nextBoolean()) {
                // 主手武器
                this.addItem(new ItemStack(Items.DIAMOND_SWORD));
            }else {
                // 使用远程武器
                this.addItem(new ItemStack(ToNekoItems.BAZOOKA));
                // 随机给10~64发闪电弹
                int ammoCount = random.nextInt(55) + 10; // 10到64发
                ItemStack ammo = new ItemStack(ToNekoItems.LIGHTNING_BOMB);
                ammo.setCount(ammoCount);
                // 随机给10~64发爆炸弹
                ItemStack explosiveAmmo = new ItemStack(ToNekoItems.EXPLOSIVE_BOMB);
                explosiveAmmo.setCount(random.nextInt(55) + 10); // 10到64发
                this.addItem(ammo);
                this.addItem(explosiveAmmo);
            }
            this.setNekoLevel(1000);
        }
    }

    @Override
    public @Nullable NekoEntity getBreedOffspring(ServerLevel level, INeko otherParent) {
        return null; // 不允许繁殖
    }

    @Override
    public void registerGoals() {
        super.registerGoals();
        this.attackGoal = new MoufletAttackGoal(this); // 创建攻击目标
        this.goalSelector.addGoal(4, new MoufletStealItemGoal(this)); // 添加偷窃物品的目标
        this.goalSelector.addGoal(2, attackGoal); // 添加攻击目标
        this.goalSelector.addGoal(1, new MoufletFlyOutOfWaterGoal(this)); // 添加飞行到陆地的目标
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PET_MODE, false); // 默认不是宠物模式
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("PetMode")) {
            this.setPetMode(compound.getBoolean("PetMode"));
        }
    }
    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("PetMode", this.isPetMode());
    }

    public boolean isFighting() {
        if (this.attackGoal == null) {
            return false;
        }
        return this.attackGoal.getTarget()!=null;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        // 只在服务端处理
        if (!level().isClientSide) {
            this.unhurtTime = 0; // 重置无伤时间计数器
            // 单次伤害大于5，触发防御架势
            if (amount > 5.0f && defenseStanceTicks <= 0) {
                this.defenseStanceTicks = 15 * 20; // 15秒
                this.thornsTicks = 10 * 20;        // 10秒
                // 抗性IV
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, defenseStanceTicks, 3));
                // TODO 发送触发特效/消息
            }
            // 反弹伤害
            if (thornsTicks > 0 && source.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
                // 反弹80%伤害
                float reflect = amount * 0.8f;
                player.hurt(player.damageSources().thorns(this), reflect);
                // 攻击者失去饱食8能量10
                player.getFoodData().eat(-8, 0.0f); // 饱食度减少
                if (player.getNekoEnergy() > 50) {
                    player.setNekoEnergy(player.getNekoEnergy() - 50); // 能量减少
                } else {
                    player.setNekoEnergy(0); // 能量不能为负
                }
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        if (bossEvent!=null) {
            bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    public @Nullable Component getCustomName() {
        if (this.isPetMode()) {
            return super.getCustomName();
        }else return this.getType().getDescription();
    }


    @Override
    public void tick() {
        super.tick();

        bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        // 控制血条显示
        if (!this.isPetMode()) {
            boolean shouldShowBar = isFighting() && !isCharmed;
            if (shouldShowBar && bossEvent.getPlayers().isEmpty()) {
                // 添加所有附近玩家
                this.level().getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(32))
                        .forEach(bossEvent::addPlayer);
            } else if (!shouldShowBar && !bossEvent.getPlayers().isEmpty()) {
                bossEvent.removeAllPlayers();
            }
        }

        // 技能冷却递减
        if (skillCooldown > 0) skillCooldown--;

        // 魅惑状态处理
        if (isCharmed) {
            charmTicks--;
            if (charmTicks <= 0) {
                isCharmed = false;
            } else {
                // 潜行加速
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.143); // +30%
            }
        } else {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.11); // 恢复正常速度
        }

        if (grabbedPlayer != null && grabbedPlayer.isPassenger() && grabFlyTicks < 100) {
            // 5秒快速升高
            this.setDeltaMovement(this.getDeltaMovement().x, 0.75, this.getDeltaMovement().z);
            grabFlyTicks++;
            if (grabFlyTicks >= 100) {
                // 到达高度，放下玩家
                grabbedPlayer.stopRiding();
                grabbedPlayer = null;
                grabFlyTicks = 0;
                // 缓降效果
                this.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, true, false));
            }
        }

        // 随机主动技能触发（仅战斗中且冷却结束）
        if (isFighting() && skillCooldown <= 0 && this.attackGoal.getTarget() != null && this.attackGoal.getTarget().isAlive()) {
            int skill = this.getRandom().nextInt(3); // 0:撒娇 1:魅惑 2:抱飞
            if (skill == 0) {
                useSpoilSkill();
            } else if (skill == 1) {
                useCharmSkill();
            } else {
                useGrabAndFlySkill();
            }
            skillCooldown = 40 * 20; // 40秒冷却
        }

        if (despairTriggered) {
            // 持续清除buff+虚弱III
            if (!this.hasEffect(MobEffects.WEAKNESS) || this.getEffect(MobEffects.WEAKNESS).getAmplifier() < 2) {
                this.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 2, true, false));
            }
            // 清除所有正面buff
            List<MobEffect> toRemove = this.getActiveEffects().stream()
                    .filter(e -> e.getEffect().value().isBeneficial() && e.getEffect() != MobEffects.WEAKNESS)
                    .map(e -> e.getEffect().value())
                    .toList();
            for (MobEffect mobEffect : toRemove) {
                removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(mobEffect));
            }

            despairTicks++;
            if (despairTicks == 20 * 20) { // 20秒后自爆
                this.level().explode(this, this.getX(), this.getY(), this.getZ(), 9.0f, Level.ExplosionInteraction.MOB);
                this.hurt(this.damageSources().generic(), Float.MAX_VALUE); // 立即死亡
            }
        }

        if (!this.level().isClientSide()) {
            unhurtTime++;
            if (unhurtTime > 1280) {
                // 给予生命回复效果
                this.addEffect(new MobEffectInstance(
                        MobEffects.HEAL,
                        1, // 持续时间为1 tick
                        0 // 强度为0
                ));
            }
        }

        if (!this.isPetMode() && isFighting() && !this.getPassengers().isEmpty()) {
            this.getPassengers().forEach(passenger -> {
                // 只对活着的实体造成伤害
                if (passenger instanceof LivingEntity living) {
                    living.hurt(this.damageSources().magic(), 5.0f); // 每tick造成5点伤害
                }
            });
        }

    }


    // 撒娇技能：减伤60%+清除debuff
    private void useSpoilSkill() {
        // TODO 播放语音/动作

        //TODO 自定义动画
        this.sendSkillMessage("spoil", this.getName().getString());

        // 添加减伤buff
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10 * 20, 2)); // 10秒，60%减伤
        // 清除debuff
        List<MobEffect> toRemove = this.getActiveEffects().stream()
                .filter(e -> !e.getEffect().value().isBeneficial() && e.getEffect() != MobEffects.WEAKNESS)
                .map(e -> e.getEffect().value())
                .toList();
        for (MobEffect mobEffect : toRemove) {
            removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(mobEffect));
        }
    }

    // 魅惑技能：血条消失+停止攻击+加速
    private void useCharmSkill() {
        this.isCharmed = true;
        this.charmTicks = 10 * 20; // 10秒
        // 血条消失
        bossEvent.removeAllPlayers();
        // 给予周围玩家魅惑效果
        this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(16))
                .forEach(p -> p.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ToNekoEffects.BEWITCHED_EFFECT), 20 * 20, 0, true, false)));
        this.sendSkillMessage("charm", this.getName().getString());
    }

    // 抱起并飞行技能：抱起玩家并飞行
    private void useGrabAndFlySkill() {
        if (this.attackGoal == null || this.attackGoal.getTarget() == null) return;
        LivingEntity target = this.attackGoal.getTarget();
        if (target.isPassenger()) return; // 已被骑乘
        if (!(target instanceof Player player)) {
            // 只允许玩家被抱起
            return;
        }
        this.grabbedPlayer = player;
        target.startRiding(this, true);
        this.grabFlyTicks = 0;
        this.sendSkillMessage("grabfly", target.getName().getString());
    }

    public boolean allowDismount(Player player) {
        return grabbedPlayer == null;
    }

    // 技能消息工具
    private void sendSkillMessage(String key,Object... args) {
        Component msg = Component.translatable("boss.toneko.mouflet.skill." + key,args);
        this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(32))
                .forEach(p -> p.sendSystemMessage(msg));
    }

    @Override
    public void die(@org.jetbrains.annotations.NotNull DamageSource damageSource) {
        if (!despairTriggered && this.getHealth() <= 0.0F) {
            triggerDespairState(damageSource);
            return; // 阻止正常死亡
        }
        super.die(damageSource);
    }

    private void triggerDespairState(DamageSource source) {
        this.despairTriggered = true;
        this.despairTicks = 0;
        this.setHealth(50.0f); // 回50血
        // 攻击+20，防御-10
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(
                this.getAttributeValue(Attributes.ATTACK_DAMAGE) + 20
        );
        this.getAttribute(Attributes.ARMOR).setBaseValue(
                Math.max(0, this.getAttributeValue(Attributes.ARMOR) - 10)
        );
        // 立即爆炸（3.75倍TNT）
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), 3.75f, Level.ExplosionInteraction.MOB);

        // 清空附近玩家饥饿
        this.level().getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, this.getBoundingBox().inflate(16))
                .forEach(p -> p.getFoodData().setFoodLevel(0));

        // TODO 播放特效/消息
    }

    @Override
    public boolean canBeTamed(Player player, ItemStack contractItem) {
        // 只有血量低于30且玩家持有猫薄荷>=10才可收服
        boolean lowHealth = this.getHealth() < 30.0f;
        int catnipCount = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(org.cneko.toneko.common.mod.items.ToNekoItems.CATNIP_TAG)) {
                catnipCount += stack.getCount();
            }
        }
        return lowHealth && catnipCount >= 10;
    }

    @Override
    public boolean tame(Player player, ItemStack contractItem) {
        if (!canBeTamed(player, contractItem)) return false;

        // 消耗10个猫薄荷
        int toConsume = 10;
        for (ItemStack stack : player.getInventory().items) {
            if (toConsume <= 0) break;
            if (stack.is(org.cneko.toneko.common.mod.items.ToNekoItems.CATNIP_TAG)) {
                int used = Math.min(stack.getCount(), toConsume);
                stack.shrink(used);
                toConsume -= used;
            }
        }

        // 属性变化
        this.setHealth(40.0f);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(3.0);
        this.getAttribute(Attributes.SCALE).setBaseValue(1.5);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.12);
        this.setNekoLevel(0);

        // 添加玩家为主人
        this.addOwner(player.getUUID(), new INeko.Owner(java.util.List.of(), 0));
        player.giveExperienceLevels(-30); // 按照契约消耗经验

        // 发送收服成功消息
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("item.toneko.contract.success", this.getName()));

        // 进入宠物模式
        this.setPetMode(true);

        // 删除契约物品
        if (!player.isCreative()) {
            contractItem.shrink(1);
        }

        //　持久化
        this.setPersistenceRequired();

        //　清除boss事件
        if (this.bossEvent != null) {
            this.bossEvent.removeAllPlayers();
        }
        return true;
    }

    public void setPetMode(boolean petMode) {
        this.entityData.set(PET_MODE, petMode);
        if (petMode){
            if (attackGoal != null) {
                // 如果进入宠物模式，清除攻击目标
                attackGoal.setTarget(null);
            }
        }
    }
    public boolean isPetMode() {
        return this.entityData.get(PET_MODE);
    }

    @Override
    public void openInteractiveMenu(ServerPlayer player) {
        if (this.isPetMode()) {
            super.openInteractiveMenu(player);
        }
    }

    @Override
    public void sendHurtMessageToPlayer(Player player) {
    }

    public static AttributeSupplier.Builder createMoufletNekoAttributes() {
        return NekoEntity.createNekoAttributes()
                .add(Attributes.MAX_HEALTH, 100) // 生命值
                .add(Attributes.ARMOR, 10) // 护甲值
                .add(Attributes.ATTACK_DAMAGE, 10) // 攻击伤害
                .add(Attributes.MOVEMENT_SPEED, 0.11) // 移动速度
                .add(Attributes.SCALE, 2.0) // 体型大小
                .add(ToNekoAttributes.MAX_NEKO_ENERGY,5000) // 最大能量
                .add(Attributes.SAFE_FALL_DISTANCE,50); // 安全落地距离

    }
}
