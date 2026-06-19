package org.cneko.toneko.common.mod.entities.boss.mouflet;

import lombok.Getter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideable;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
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
import java.util.Objects;

import static org.cneko.toneko.common.mod.util.TextUtil.randomTranslatabledComponent;

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
    AA[血量检测] --> AB{<40hp?}
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
public class MoufletNekoBoss extends NekoEntity implements NekoBoss, PlayerRideable {
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

    // ============================================================
    // 弱点窗口系统（技能后的乏力期 —— 玩家输出的时机）
    // ============================================================
    private int vulnerabilityTicks = 0;
    private float vulnerabilityMultiplier = 1.0f;
    private int spoilTicks = 0;                              // 撒娇技能剩余时间追踪
    private static final int VULN_CHARM_DURATION = 5 * 20;   // 魅惑后 5 秒脆弱
    private static final float VULN_CHARM_MULT = 1.5f;       // 受伤 +50%
    private static final int VULN_SPOIL_DURATION = 5 * 20;   // 撒娇后 5 秒脆弱
    private static final float VULN_SPOIL_MULT = 1.3f;       // 受伤 +30%
    private static final int VULN_GRABFLY_DURATION = 3 * 20; // 抱飞后 3 秒硬直
    private static final float VULN_GRABFLY_MULT = 1.3f;     // 受伤 +30%
    private static final float VULN_DESPAIR_MULT = 1.25f;    // 绝望状态全局增伤

    // ============================================================
    // 骑乘飞行常量
    // ============================================================
    private static final double RIDDEN_FLY_SPEED = 0.3;        // 水平飞行速度
    private static final double RIDDEN_FLY_VERTICAL = 0.2;     // 垂直上升速度
    private static final double RIDDEN_MAX_HEIGHT = 30.0;      // 最大离地高度
    private static final double RIDDEN_DESCENT_SPEED = 0.05;   // 无操作时缓慢下降

    // ============================================================
    // 亲密度系统
    // ============================================================
    private static final int MAX_AFFECTION = 100;
    private static final int AFFECTION_GIFT = 2;
    private static final int AFFECTION_INTERACT = 3;
    private static final int AFFECTION_RIDE_TIME = 1;     // 每 30 秒骑乘 +1
    private static final int AFFECTION_HEAL = 5;
    private static final int AFFECTION_COMBAT = 3;

    public static final EntityDataAccessor<Boolean> PET_MODE = SynchedEntityData.defineId(MoufletNekoBoss.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> AFFECTION_ID = SynchedEntityData.defineId(MoufletNekoBoss.class, EntityDataSerializers.INT);

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
            org.cneko.toneko.common.mod.api.NekoLevelRegistry.base().setRaw(this, 1000);
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
        builder.define(PET_MODE, false);
        builder.define(AFFECTION_ID, 0);
    }

    // ============================================================
    // 亲密度 API
    // ============================================================
    public int getAffection() {
        return this.entityData.get(AFFECTION_ID);
    }
    private void modifyAffection(int delta) {
        if (!isPetMode() || this.level().isClientSide) return;
        int val = Math.max(0, Math.min(MAX_AFFECTION, getAffection() + delta));
        this.entityData.set(AFFECTION_ID, val);
    }
    /** 0=冷淡 1=缓和 2=亲密 3=热恋 */
    public int getAffectionLevel() {
        int a = getAffection();
        if (a >= 81) return 3;
        if (a >= 51) return 2;
        if (a >= 21) return 1;
        return 0;
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("PetMode")) {
            this.setPetMode(compound.getBoolean("PetMode"));
        }
        if (compound.contains("SpoilTicks")) {
            this.spoilTicks = compound.getInt("SpoilTicks");
        }
        if (compound.contains("VulnTicks")) {
            this.vulnerabilityTicks = compound.getInt("VulnTicks");
            this.vulnerabilityMultiplier = compound.getFloat("VulnMult");
        }
        if (compound.contains("Affection")) {
            this.entityData.set(AFFECTION_ID, compound.getInt("Affection"));
        }
    }
    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("PetMode", this.isPetMode());
        compound.putInt("SpoilTicks", this.spoilTicks);
        compound.putInt("VulnTicks", this.vulnerabilityTicks);
        compound.putFloat("VulnMult", this.vulnerabilityMultiplier);
        compound.putInt("Affection", getAffection());
    }

    public boolean isFighting() {
        if (this.attackGoal == null) {
            return false;
        }
        return this.attackGoal.getTarget()!=null;
    }

    /**
     * 覆盖父类 tickHatred：移动完全交给 MoufletAttackGoal，不在 Brain 中提交重复的 COMBAT 移动。
     * 宠物模式下完全禁用仇恨系统。
     */
    @Override
    protected void tickHatred() {
        if (isPetMode()) {
            if (this.hatredTarget != null) clearHatred();
            return;
        }
        if (this.hatredTarget == null) return;
        if (!this.hatredTarget.isAlive() || this.hatredCooldown <= 0) {
            clearHatred();
            return;
        }
        this.hatredCooldown--;
        trySendHatredMessage();
    }

    @Override
    protected void setHatredTarget(LivingEntity target, int duration) {
        if (isPetMode()) return;
        super.setHatredTarget(target, duration);
    }

    // ============================================================
    // 骑乘操控 — 仅宠物模式下主人可操控飞行
    // ============================================================

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        if (!isPetMode()) return null;
        var fp = this.getFirstPassenger();
        if (fp == null) return null;
        if (fp instanceof Player player) {
            // 客户端：不检查 owner（数据未同步），直接返回乘客
            if (this.level().isClientSide) return player;
            // 服务端：必须是主人
            return this.hasOwner(player.getUUID()) ? player : null;
        }
        return null;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        // 宠物模式被骑乘：不传递坠落伤害给乘客
        if (isPetMode() && this.isVehicle()) {
            this.fallDistance = 0;
            return false;
        }
        return super.causeFallDamage(fallDistance, multiplier, source);
    }

    @Override
    public void travel(@NotNull Vec3 input) {
        if (this.isVehicle() && getControllingPassenger() instanceof Player player) {
            // 飞行操控模式：类似鞘翅/创造飞行 —— 视线方向=飞行方向
            this.getNavigation().stop();
            float yaw = player.getYRot();
            float pitch = player.getXRot();

            // 前进/后退输入
            float fwd = player.zza;       // W(+) / S(-)
            float str = player.xxa;       // A / D

            // 水平方向 = 视线 yaw + 前后/左右
            double hSpeed = RIDDEN_FLY_SPEED;
            double radYaw = Math.toRadians(-yaw);
            double dx = (Math.sin(radYaw) * fwd + Math.cos(radYaw) * str) * hSpeed;
            double dz = (Math.cos(radYaw) * fwd - Math.sin(radYaw) * str) * hSpeed;

            // 垂直方向 = 视线 pitch：抬头=上升，低头=下降，平视=水平
            // 只有在按 W 前进时才应用垂直分量（避免原地起飞）
            double radPitch = Math.toRadians(-pitch);
            double vy = 0;
            if (fwd > 0.01) {
                vy = Math.sin(radPitch) * hSpeed * fwd;
            }
            // Ctrl 额外上升，Shift 额外下降
            if (player.isSprinting()) vy += RIDDEN_FLY_VERTICAL;
            if (player.isCrouching()) vy -= RIDDEN_FLY_VERTICAL;

            // 高度限制
            int groundY = this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, this.blockPosition()).getY();
            if (this.getY() > groundY + RIDDEN_MAX_HEIGHT && vy > 0) {
                vy = 0;
            }

            // 旋转跟随玩家
            this.setYRot(yaw);
            this.yHeadRot = yaw;

            Vec3 motion = new Vec3(dx, vy, dz);
            this.setDeltaMovement(motion);
            this.move(MoverType.SELF, motion);
            // 标记为离地
            if (this.getY() > this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, this.blockPosition()).getY() + 0.5) {
                this.setOnGround(false);
            }
            // 骑乘时间累计亲密度（每 30 秒）
            if (this.tickCount % 600 == 0) modifyAffection(AFFECTION_RIDE_TIME);
            // 骑乘中随机互动（每 30-60 秒）
            if (this.tickCount % (600 + this.random.nextInt(600)) == 0) {
                player.sendSystemMessage(Component.translatable(
                    "boss.toneko.mouflet.affection.ride." + this.random.nextInt(4),
                    this.getName().getString()));
            }
            // 主人低血量关心
            if (player.getHealth() < player.getMaxHealth() * 0.5 && this.tickCount % 200 == 0) {
                player.sendSystemMessage(Component.translatable(
                    "boss.toneko.mouflet.affection.care." + this.random.nextInt(3),
                    this.getName().getString()));
            }
        } else {
            super.travel(input);
        }
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        // 只在服务端处理
        if (!level().isClientSide) {
            this.unhurtTime = 0; // 重置无伤时间计数器

            // 弱点窗口：伤害倍率
            if (vulnerabilityTicks > 0) {
                amount *= vulnerabilityMultiplier;
            }
            // 绝望状态：全局增伤
            if (despairTriggered) {
                amount *= VULN_DESPAIR_MULT;
            }

            // 单次伤害大于5，触发防御架势（脆弱窗口期内不触发）
            if (amount > 5.0f && defenseStanceTicks <= 0 && vulnerabilityTicks <= 0) {
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
            // 宠物模式战斗保护：主人被攻击时，临时提升攻击力
            if (isPetMode() && source.getEntity() instanceof LivingEntity attacker) {
                for (var entry : this.getOwners().entrySet()) {
                    Player owner = this.level().getPlayerByUUID(entry.getKey());
                    if (owner != null && (owner.getLastHurtByMob() == attacker || owner.getLastHurtMob() == attacker)) {
                        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0, false, true));
                        if (this.random.nextFloat() < 0.3f) {
                            owner.sendSystemMessage(Component.translatable(
                                "boss.toneko.mouflet.affection.protect." + this.random.nextInt(3),
                                this.getName().getString()));
                        }
                        break;
                    }
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
                // 魅惑结束 → 5 秒脆弱窗口（反噬）
                vulnerabilityTicks = VULN_CHARM_DURATION;
                vulnerabilityMultiplier = VULN_CHARM_MULT;
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
                // 落地硬直 → 3 秒脆弱窗口
                vulnerabilityTicks = VULN_GRABFLY_DURATION;
                vulnerabilityMultiplier = VULN_GRABFLY_MULT;
            }
        }

        // 随机主动技能触发（仅战斗中、非宠物、非魅惑、冷却结束）
        if (!isPetMode() && isFighting() && !isCharmed && skillCooldown <= 0 && this.attackGoal.getTarget() != null && this.attackGoal.getTarget().isAlive()) {
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

        // 撒娇技能剩余时间追踪 → 结束时触发弱点窗口
        if (spoilTicks > 0) {
            spoilTicks--;
            if (spoilTicks <= 0) {
                vulnerabilityTicks = VULN_SPOIL_DURATION;
                vulnerabilityMultiplier = VULN_SPOIL_MULT;
            }
        }

        // 弱点窗口计时器递减
        if (vulnerabilityTicks > 0) {
            vulnerabilityTicks--;
            if (vulnerabilityTicks <= 0) {
                vulnerabilityMultiplier = 1.0f;
            }
            // 视觉提示：每 5 tick 生成 DAMAGE_INDICATOR 粒子，告诉玩家"现在是输出的时机"
            if (vulnerabilityTicks % 5 == 0 && this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        this.getX(), this.getY() + this.getBbHeight() * 0.7, this.getZ(),
                        3, 0.4, 0.3, 0.4, 0.01);
            }
        }

        // 低血量收服提示：每 30 秒向附近玩家广播一次
        if (!this.isPetMode() && this.getHealth() < 40.0f && this.tickCount % 600 == 0) {
            Component hint = Component.translatable("boss.toneko.mouflet.tame_hint",
                    this.getName().getString());
            this.level().getEntitiesOfClass(Player.class,
                    this.getBoundingBox().inflate(32), LivingEntity::isAlive)
                    .forEach(p -> p.sendSystemMessage(hint));
        }

        // 宠物模式：主动靠近主人（2-5 格内跟随）
        if (isPetMode() && this.tickCount % 40 == 0 && this.getTarget() == null) {
            Player owner = this.level().getNearestPlayer(this, 16);
            if (owner != null && hasOwner(owner.getUUID()) && this.distanceToSqr(owner) > 25.0) {
                this.getNavigation().moveTo(owner, 0.4);
            }
        }

        // 宠物模式待机行为：叼东西给主人
        if (isPetMode() && this.tickCount % 200 == 0 && this.getTarget() == null) {
            Player owner = this.level().getNearestPlayer(this, 16);
            if (owner != null && hasOwner(owner.getUUID())) {
                // 搜索附近掉落物，叼到主人脚下
                var items = this.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        this.getBoundingBox().inflate(8), e -> e.isAlive());
                if (!items.isEmpty() && this.random.nextFloat() < 0.3f) {
                    var item = items.get(this.random.nextInt(items.size()));
                    this.getNavigation().moveTo(item, 0.5);
                }
            }
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

        if (!this.isPetMode() && isFighting() && !isCharmed && !this.getPassengers().isEmpty()) {
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
        this.spoilTicks = 10 * 20; // 追踪撒娇剩余时间，用于结束时的弱点窗口
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
        int i = random.nextInt(10);
        Component msg = Component.translatable("boss.toneko.mouflet.skill." + key+"."+i,args);
        this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(32))
                .forEach(p -> p.sendSystemMessage(msg));
    }

    @Override
    public void sendGiftMessageToPlayer(Player player) {
        player.sendSystemMessage(randomTranslatabledComponent("boss.toneko.mouflet.gift",10, Objects.requireNonNull(this.getCustomName()).getString()));
    }
    @Override
    public void sendHurtMessageToPlayer(Player player) {
        player.sendSystemMessage(randomTranslatabledComponent("boss.toneko.mouflet.hurt",10, Objects.requireNonNull(this.getCustomName()).getString()));
    }

    // ============================================================
    // 送礼：宠物模式增加亲密度
    // ============================================================
    @Override
    public boolean giftItem(Player player, ItemStack stack) {
        boolean ok = super.giftItem(player, stack);
        if (ok && isPetMode() && hasOwner(player.getUUID())) {
            modifyAffection(AFFECTION_GIFT);
        }
        return ok;
    }

    // ============================================================
    // 右键互动：宠物模式亲昵行为 ♡
    // ============================================================
    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        // 宠物模式 + 主人 + 不潜行 + 空手 → 亲昵互动
        if (isPetMode() && hasOwner(player.getUUID()) && !player.isShiftKeyDown()
                && player.getItemInHand(hand).isEmpty()) {
            if (this.level().isClientSide) return InteractionResult.SUCCESS;
            modifyAffection(AFFECTION_INTERACT);
            int r = this.random.nextInt(100);
            if (r < 40) {
                doNuzzleInteraction(player);
            } else if (r < 65) {
                doHugInteraction(player);
            } else if (r < 85) {
                doTsunRefuseInteraction(player);
            } else if (r < 95) {
                doKissInteraction(player);
            } else {
                doLittleDevilInteraction(player);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    // ---- 亲昵行为实现 ----
    private void sendAffectionMsg(Player player, String key) {
        player.sendSystemMessage(Component.translatable(key, this.getName().getString()));
    }

    private void doNuzzleInteraction(Player player) {
        sendAffectionMsg(player, "boss.toneko.mouflet.affection.nuzzle." + this.random.nextInt(4));
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + this.getBbHeight(), this.getZ(),
                    5, 0.3, 0.3, 0.3, 0.02);
        }
        this.getNavigation().moveTo(player, 0.4);
    }

    private void doHugInteraction(Player player) {
        sendAffectionMsg(player, "boss.toneko.mouflet.affection.hug." + this.random.nextInt(4));
        if (this.level() instanceof ServerLevel sl) {
            for (int i = 0; i < 3; i++)
                sl.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + this.getBbHeight(), this.getZ(),
                        10, 0.5, 0.5, 0.5, 0.03);
        }
        player.heal(2);
        this.heal(2);
    }

    private void doTsunRefuseInteraction(Player player) {
        sendAffectionMsg(player, "boss.toneko.mouflet.affection.tsun." + this.random.nextInt(4));
        // 退后一步但冒爱心（傲娇！）
        this.getNavigation().stop();
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + this.getBbHeight(), this.getZ(),
                    3, 0.2, 0.3, 0.2, 0.01);
        }
    }

    private void doKissInteraction(Player player) {
        sendAffectionMsg(player, "boss.toneko.mouflet.affection.kiss." + this.random.nextInt(4));
        if (this.level() instanceof ServerLevel sl) {
            for (int i = 0; i < 5; i++)
                sl.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + this.getBbHeight(), this.getZ(),
                        15, 0.8, 1.0, 0.8, 0.05);
            sl.sendParticles(ParticleTypes.GLOW, this.getX(), this.getY() + this.getBbHeight(), this.getZ(),
                    10, 0.3, 0.5, 0.3, 0.02);
        }
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, true));
    }

    private void doLittleDevilInteraction(Player player) {
        sendAffectionMsg(player, "boss.toneko.mouflet.affection.devil." + this.random.nextInt(4));
        // 偷食物
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.has(DataComponents.FOOD)) {
                this.eatOrStoreFood(stack);
                player.getInventory().removeItem(i, 1);
                break;
            }
        }
    }

    @Override
    public List<String> getMoeTags() {
        return List.of("shoakuma","tsundere"); // 小恶魔
    }

    @Override
    public void remove(@NotNull Entity.RemovalReason reason) {
        if (this.bossEvent != null) {
            this.bossEvent.removeAllPlayers();
            this.bossEvent.setVisible(false);
        }
        super.remove(reason);
    }

    @Override
    public void die(@org.jetbrains.annotations.NotNull DamageSource damageSource) {
        if (!despairTriggered && this.getHealth() <= 0.0F) {
            triggerDespairState(damageSource);
            return; // 阻止正常死亡
        }
        // 关掉boss血条
        if (this.bossEvent != null) {
            this.bossEvent.removeAllPlayers();
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
        this.sendSkillMessage("despair", this.getName().getString());
    }

    @Override
    public boolean canBeTamed(Player player, ItemStack contractItem) {
        // 只有血量低于40且玩家持有猫薄荷>=5才可收服
        boolean lowHealth = this.getHealth() < 40.0f;
        int catnipCount = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(org.cneko.toneko.common.mod.items.ToNekoItems.CATNIP_TAG)) {
                catnipCount += stack.getCount();
            }
        }
        // 血量够了但猫薄荷不够 → 提示玩家
        if (lowHealth && catnipCount < 5) {
            player.sendSystemMessage(Component.translatable(
                    "boss.toneko.mouflet.tame_hint_catnip", catnipCount, 5));
        }
        return lowHealth && catnipCount >= 5;
    }

    @Override
    public boolean tame(Player player, ItemStack contractItem) {
        if (!canBeTamed(player, contractItem)) return false;

        // 消耗5个猫薄荷
        int toConsume = 5;
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
        org.cneko.toneko.common.mod.api.NekoLevelRegistry.base().setRaw(this, 0);

        // 添加玩家为主人
        this.addOwner(player.getUUID(), new INeko.Owner(java.util.List.of(), 0));
        player.giveExperienceLevels(-30); // 按照契约消耗经验

        // 发送收服成功消息
        player.sendSystemMessage(randomTranslatabledComponent("boss.toneko.mouflet.tamed",10, Objects.requireNonNull(this.getCustomName()).getString()));

        // 进入宠物模式 + 清除战斗状态
        this.setPetMode(true);
        this.entityData.set(AFFECTION_ID, 20); // 初始亲密度 20
        if (this.attackGoal != null && this.attackGoal.getTarget() != null) {
            this.attackGoal.stop(); // 停止攻击，清除目标
        }
        this.clearHatred();
        this.skillCooldown = 0; // 重置技能冷却

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
