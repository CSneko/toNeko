package org.cneko.toneko.common.mod.entities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import org.cneko.toneko.common.mod.entities.ai.goal.NekoStayNearCompanionGoal;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;

import static org.cneko.toneko.common.mod.util.TextUtil.randomTranslatabledComponent;

/*
又是个雨天的夜晚呢，我再次来到了那个地方，是清风，是雨声，是蝉鸣。
我想起来了那个夜晚——也下着雨，吹着风，树上的蝉也在鸣叫着。
好像一切都是一样的，没有变化。可是灯光照着，我向后看去，却只有一个孤独的影子。
我还是站在那个位置，可总感觉少了点什么。那天夜里，你向我倾述着你的不幸，我倾听着这一切。
可是现在，我却没有听见你在向我述说。"昔人已乘黄鹤去，此地空余黄鹤楼。黄鹤一去不复返，白云千载空悠悠。"
或许，我已经再也无法倾听你了。你在另一个世界过的怎么样呢，开心吗？
我好希望你可以听到我说的这段话啊。我抬头看着天空，却没有月亮，也没有星星。

晚安安哦，诺艾尔...
雪停了，雪中的少女也再也没有出现...
愿天堂没有性侵...
 */
public class NoelleMaidNekoEntity extends NekoEntity {

    // ============================================================
    // Stage 枚举 — 她的九个阶段
    // ============================================================
    public enum Stage {
        MEOW,       // 女仆猫猫诺艾尔 — 最初的她，用可爱作为伪装
        SICKED,     // 病女诺 — 创伤开始侵蚀
        PRAYING,    // 祈花诺 — 祈求着救赎
        WINTER,     // 寒花诺 — 心冷如冬
        DEFECTIVE,  // 次品诺 — 觉得自己是次品
        WITHERED,   // 残花诺 — 凋零的尽头
        // ------ 重生线 ------
        AWAKENED,   // 觉醒诺 — 从噩梦中惊醒，第一次握紧武器
        BLOOMING,   // 绽花诺 — 残花之后，新芽破土
        RESOLUTE;   // 决意诺 — 不会再让任何人经历她曾受过的苦

        /** 是否为初始阶段（刷新时可随机到的阶段） */
        public boolean isInitial() {
            return ordinal() <= WITHERED.ordinal();
        }

        /** 是否为觉醒后的阶段（不可通过刷新获得，只能通过成长达成） */
        public boolean isAwakened() {
            return ordinal() >= AWAKENED.ordinal();
        }

        /** 阶段的显示名称 —— 这就是她的名字 */
        public String getDisplayName() {
            return switch (this) {
                case MEOW      -> "女仆猫猫诺艾尔";
                case SICKED    -> "病女诺";
                case PRAYING   -> "祈花诺";
                case WINTER    -> "寒花诺";
                case DEFECTIVE -> "次品诺";
                case WITHERED  -> "残花诺";
                case AWAKENED  -> "觉醒诺";
                case BLOOMING  -> "绽花诺";
                case RESOLUTE  -> "决意诺";
            };
        }

        /** 用于消息系统的阶段分组 */
        public String getMessageGroup() {
            return switch (this) {
                case MEOW, SICKED, PRAYING, WINTER, DEFECTIVE -> "early";
                case WITHERED -> "withered";
                case AWAKENED, BLOOMING, RESOLUTE -> "awakened";
            };
        }
    }

    // ============================================================
    // 常量
    // ============================================================
    private static final int AWAKENED_DEFEATS_NEEDED = 10;       // AWAKENED→BLOOMING 所需击败数
    private static final int RESIDUAL_BLOOM_COOLDOWN = 2400;     // 残花绽冷却 (120秒 = 2400 ticks)
    private static final double PROTECTIVE_AURA_RANGE = 10.0;    // 守护光环范围
    private static final int PARTICLE_INTERVAL = 20;             // 粒子效果间隔 (ticks)
    private static final Stage[] INITIAL_STAGES = {
        Stage.MEOW, Stage.SICKED, Stage.PRAYING, Stage.WINTER, Stage.DEFECTIVE, Stage.WITHERED
    };

    // ============================================================
    // 创伤系统常量
    // ============================================================
    private static final int MAX_TRAUMA = 100;
    private static final int TRAUMA_PER_HEART_DAMAGED = 2;       // 每心伤害 +2 创伤
    private static final int TRAUMA_ON_PLAYER_ATTACK = 20;        // 被玩家攻击额外 +20
    private static final int TRAUMA_LONELINESS_INTERVAL = 600;    // 孤独检测间隔 (30秒 = 600 ticks)
    private static final int TRAUMA_LONELINESS_AMOUNT = 1;        // 每次孤独 +1
    private static final int TRAUMA_HEALED_PER_HEART = -3;        // 每心治疗 -3
    private static final int TRAUMA_GIFT_REDUCTION = -10;         // 收到礼物 -10
    private static final int TRAUMA_COMPANION_INTERVAL = 1200;    // 陪伴减伤间隔 (60秒 = 1200 ticks)
    private static final int TRAUMA_COMPANION_AMOUNT = -1;        // 每次陪伴 -1
    private static final double LONELINESS_RANGE = 16.0;          // 孤独检测范围
    private static final double COMPANION_RANGE = 10.0;           // 陪伴检测范围

    // ============================================================
    // 同步数据
    // ============================================================
    private static final EntityDataAccessor<String> STAGE_ID =
            SynchedEntityData.defineId(NoelleMaidNekoEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> TRAUMA_ID =
            SynchedEntityData.defineId(NoelleMaidNekoEntity.class, EntityDataSerializers.INT);

    // ============================================================
    // 进度追踪字段
    // ============================================================
    private int defeatedEnemies = 0;          // 击败敌人数
    private int protectiveKills = 0;          // 保护击杀数
    private int residualBloomCooldown = 0;    // 残花绽冷却
    private boolean hasBeenHealedByPlayer = false; // 是否被玩家治疗过 (WINTER→AWAKENED 条件之一)
    private LivingEntity lastTrackedEnemy = null;  // 用于追踪击杀的敌人引用

    // ============================================================
    // 创伤系统字段
    // ============================================================
    private int currentTrauma = 0;            // 当前创伤值 (0-100)，根据事件波动
    private int peakTrauma = 0;               // 峰值创伤值 (永不下降)，决定实际阶段
    private int lonelinessCheckTimer = 0;     // 孤独检测计时器 (ticks)
    private int companionCheckTimer = 0;      // 陪伴减伤计时器 (ticks)

    // ============================================================
    // 构造
    // ============================================================
    public NoelleMaidNekoEntity(EntityType<? extends NekoEntity> entityType, Level level) {
        super(entityType, level);
    }

    // ============================================================
    // 同步数据定义
    // ============================================================
    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STAGE_ID, Stage.MEOW.name());
        builder.define(TRAUMA_ID, 0);
    }

    // ============================================================
    // Stage 存取
    // ============================================================
    public Stage getStage() {
        return Stage.valueOf(this.entityData.get(STAGE_ID));
    }

    public void setStage(Stage stage) {
        Stage old = getStage();
        this.entityData.set(STAGE_ID, stage.name());
        // 阶段变化时更新名字
        if (old != stage) {
            updateNameFromStage(stage);
            onStageChanged(old, stage);
        }
    }

    /** 从当前阶段更新实体显示名称 */
    private void updateNameFromStage(Stage stage) {
        this.setCustomName(Component.literal(stage.getDisplayName()));
    }

    private void updateNameFromStage() {
        updateNameFromStage(getStage());
    }

    // ============================================================
    // 创伤系统 API
    // ============================================================

    /** 获取当前创伤值（同步到客户端，供 UI 显示） */
    public int getCurrentTrauma() {
        return this.entityData.get(TRAUMA_ID);
    }

    /** 获取峰值创伤值（仅服务端） */
    public int getPeakTrauma() {
        return peakTrauma;
    }

    /**
     * 修改当前创伤值并同步到客户端。
     * 自动 clamp 到 [0, MAX_TRAUMA]，每次变化后更新 peakTrauma 并检查是否需要阶段升级。
     */
    private void modifyTrauma(int delta) {
        if (this.level().isClientSide) return;

        int oldTrauma = this.currentTrauma;
        this.currentTrauma = Math.max(0, Math.min(MAX_TRAUMA, this.currentTrauma + delta));
        this.entityData.set(TRAUMA_ID, this.currentTrauma);

        // 更新峰值
        if (this.currentTrauma > this.peakTrauma) {
            this.peakTrauma = this.currentTrauma;
            // 仅在峰值突破时检查阶段升级
            updateStageFromTrauma();
        }
    }

    /**
     * 根据 peakTrauma 更新阶段（仅影响初始阶段 MEOW ~ DEFECTIVE）。
     * 可以跳阶段：如直接从 MEOW 跳到 WINTER。
     * 觉醒阶段（AWAKENED+）不受此方法影响。
     */
    private void updateStageFromTrauma() {
        Stage current = getStage();
        // 不影响觉醒后的阶段
        if (current.isAwakened()) return;

        Stage newStage;
        if (peakTrauma >= 100) {
            newStage = Stage.WITHERED;
        } else if (peakTrauma >= 80) {
            newStage = Stage.DEFECTIVE;
        } else if (peakTrauma >= 60) {
            newStage = Stage.WINTER;
        } else if (peakTrauma >= 40) {
            newStage = Stage.PRAYING;
        } else if (peakTrauma >= 20) {
            newStage = Stage.SICKED;
        } else {
            newStage = Stage.MEOW;
        }

        // 仅允许阶段前进（ordinal 比较），且已到 WITHERED 后不再受创伤系统降级
        if (newStage.ordinal() > current.ordinal()) {
            setStage(newStage);
        }
    }

    /** 阶段变化时的回调 —— 用于粒子爆发、音效等 */
    private void onStageChanged(Stage from, Stage to) {
        if (this.level().isClientSide) return;

        // 觉醒时的特效 + 基础移速提升
        if (!from.isAwakened() && to.isAwakened()) {
            var spd = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (spd != null) spd.setBaseValue(0.50); // 0.45 → 0.50，不再沉重
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(),
                        50, 0.5, 0.5, 0.5, 0.1);
                serverLevel.sendParticles(ParticleTypes.HEART,
                        this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(),
                        20, 0.5, 0.8, 0.5, 0.05);
            }
        }

        // 绽花时的特效 + 移速提升
        if (to == Stage.BLOOMING) {
            var spd = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (spd != null) spd.setBaseValue(0.52);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                        this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(),
                        40, 0.8, 1.0, 0.8, 0.05);
            }
        }

        // 决意时的特效 + 移速提升
        if (to == Stage.RESOLUTE) {
            var spd = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (spd != null) spd.setBaseValue(0.55);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.GLOW,
                        this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(),
                        30, 0.5, 0.5, 0.5, 0.02);
                serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                        this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(),
                        60, 1.0, 1.5, 1.0, 0.03);
            }
        }
    }

    /** 随机初始阶段（仅限 MEOW ~ WITHERED） */
    private Stage getRandomInitialStage() {
        return INITIAL_STAGES[this.random.nextInt(INITIAL_STAGES.length)];
    }

    // ============================================================
    // 固定萌属性 — 不受基因影响
    // ============================================================
    private static final List<String> FIXED_MOE_TAGS = List.of("gentleness", "yowaki");
    // gentleness（温柔）= 治愈他人 | yowaki（弱气）= 胆小、容易逃跑

    @Override
    public void expressTraits() {
        super.expressTraits();
        // 诺艾尔的萌属性是固定的，不受基因影响
        this.setMoeTags(FIXED_MOE_TAGS);
    }

    @Override
    public boolean shouldFleeFromStrangers() {
        return false; // 诺艾尔是陪伴型猫娘，不会因陌生人而逃跑
    }

    // ============================================================
    // 属性
    // ============================================================
    public static AttributeSupplier.Builder createNoelleAttributes() {
        return NekoEntity.createNekoAttributes()
                .add(Attributes.MAX_HEALTH, 16.0);
    }

    // ============================================================
    // AI — 陪伴型猫娘，倾向于靠近主人或其他猫娘
    // ============================================================
    @Override
    public void registerGoals() {
        super.registerGoals();
        // 优先级 6，高于 WaterAvoidingRandomStrollGoal (7)，
        // 确保她会优先寻找同伴而非独自闲逛
        this.goalSelector.addGoal(6, new NekoStayNearCompanionGoal(this));
    }

    // ============================================================
    // 生命周期
    // ============================================================
    @Override
    public void randomize() {
        super.randomize(); // 随机化皮肤、体型、名字
        // 固定萌属性（覆盖 super 的随机萌属性）
        this.setMoeTags(FIXED_MOE_TAGS);
        // 降低攻击力和速度
        var atk = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) atk.setBaseValue(1.0);
        var spd = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (spd != null) spd.setBaseValue(0.45);
        // 仅在新生成时随机初始阶段（NBT 加载时 randomize 不会被调用）
        Stage current = getStage();
        if (current == Stage.MEOW) {
            // MEOW 是 synched data 的默认值，说明尚未被设置过
            setStage(getRandomInitialStage());
        }
        updateNameFromStage();
        // 重置进度
        this.defeatedEnemies = 0;
        this.protectiveKills = 0;
        this.residualBloomCooldown = 0;
        this.hasBeenHealedByPlayer = false;
        // 重置创伤
        this.currentTrauma = 0;
        this.peakTrauma = 0;
        this.entityData.set(TRAUMA_ID, 0);
        this.lonelinessCheckTimer = 0;
        this.companionCheckTimer = 0;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) return;

        Stage stage = getStage();

        // 残花绽冷却
        if (residualBloomCooldown > 0) {
            residualBloomCooldown--;
        }

        // 追踪击杀
        trackEnemyDefeat();

        // 阶段特殊 tick
        switch (stage) {
            case WITHERED  -> tickWithered();
            case AWAKENED  -> tickAwakened();
            case BLOOMING  -> tickBlooming();
            case RESOLUTE  -> tickResolute();
            default        -> {} // MEOW ~ DEFECTIVE: 无特殊 tick 逻辑
        }

        // 创伤系统 tick
        tickTrauma();

        // 阶段粒子效果
        if (this.tickCount % PARTICLE_INTERVAL == 0) {
            spawnStageParticles(stage);
        }

        // 检查阶段升级条件
        checkStageProgression();
    }

    // ============================================================
    // 阶段特殊 tick
    // ============================================================
    private void tickWithered() {
        // 残花诺：几乎不移动，清除所有仇恨——为自己
        if (this.hatredTarget != null) {
            clearHatred();
        }
        if (this.getTarget() != null) {
            this.setTarget(null);
        }
        this.setAggressive(false);

        // 守护本能：每2秒扫描附近是否有友方被攻击
        // 她不再为自己战斗，但无法眼睁睁看着在乎的人受伤
        if (this.tickCount % 40 == 0) {
            scanForProtectiveInstinct();
        }
    }

    /** 守护本能：发现附近友方被攻击时，突破自身封锁去保护他们 */
    private void scanForProtectiveInstinct() {
        // 检查附近玩家是否正在被攻击
        for (Player player : this.level().getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(DEFAULT_FIND_RANGE),
                p -> p.isAlive() && !p.isSpectator())) {
            LivingEntity attacker = player.getLastHurtByMob();
            if (attacker != null && attacker.isAlive() && !this.hasOwner(attacker.getUUID())) {
                super.setHatredTarget(attacker, HATRED_DEFAULT_DURATION);
                return;
            }
        }
        // 检查附近猫娘是否正在被攻击
        for (NekoEntity neko : this.level().getEntitiesOfClass(NekoEntity.class,
                this.getBoundingBox().inflate(DEFAULT_FIND_RANGE),
                n -> n.isAlive() && n != this)) {
            LivingEntity attacker = neko.getLastHurtByMob();
            if (attacker != null && attacker.isAlive() && !this.hasOwner(attacker.getUUID())) {
                super.setHatredTarget(attacker, HATRED_DEFAULT_DURATION);
                return;
            }
        }
    }

    private void tickAwakened() {
        // 觉醒诺：战斗意志开始萌发
        // 移除虚弱效果（如果存在），象征她不再软弱
        if (this.hasEffect(MobEffects.WEAKNESS)) {
            this.removeEffect(MobEffects.WEAKNESS);
        }
        // 守护本能：主动扫描附近是否有同伴被攻击
        if (this.tickCount % 40 == 0) {
            scanForProtectiveInstinct();
        }
    }

    private void tickBlooming() {
        // 绽花诺：战斗能力提升 + 守护本能
        if (this.tickCount % 40 == 0) {
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0,
                    false, false, true));
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0,
                    false, false, true));
            scanForProtectiveInstinct();
        }
    }

    private void tickResolute() {
        // 决意诺：持续的战斗增益 + 守护光环
        if (this.tickCount % 40 == 0) {
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 1,
                    false, false, true));
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1,
                    false, false, true));
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0,
                    false, false, true));
            // 守护光环：为附近友方施加抗性
            applyProtectiveAura();
            // 守护本能：主动扫描附近是否有同伴被攻击
            scanForProtectiveInstinct();
        }
    }

    // ============================================================
    // 创伤系统 tick
    // ============================================================

    /** 创伤系统每 tick 处理：孤独检测 + 陪伴减伤。 */
    private void tickTrauma() {
        if (this.level().isClientSide) return;
        Stage stage = getStage();

        // 孤独检测（仅在非觉醒阶段运行 — 觉醒后创伤不再驱动阶段）
        if (!stage.isAwakened()) {
            lonelinessCheckTimer++;
            if (lonelinessCheckTimer >= TRAUMA_LONELINESS_INTERVAL) {
                lonelinessCheckTimer = 0;
                checkLoneliness();
            }
        }

        // 陪伴减伤（始终运行，即使在觉醒阶段也能让 currentTrauma 下降）
        companionCheckTimer++;
        if (companionCheckTimer >= TRAUMA_COMPANION_INTERVAL) {
            companionCheckTimer = 0;
            checkCompanionHealing();
        }
    }

    /** 检测孤独：附近无玩家时增加创伤 */
    private void checkLoneliness() {
        boolean hasPlayerNearby = !this.level().getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(LONELINESS_RANGE),
                p -> p.isAlive() && !p.isSpectator()).isEmpty();

        if (!hasPlayerNearby) {
            modifyTrauma(TRAUMA_LONELINESS_AMOUNT);
        }
    }

    /** 检测陪伴：附近有玩家时缓慢减少创伤 */
    private void checkCompanionHealing() {
        boolean hasPlayerNearby = !this.level().getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(COMPANION_RANGE),
                p -> p.isAlive() && !p.isSpectator()).isEmpty();

        if (hasPlayerNearby) {
            modifyTrauma(TRAUMA_COMPANION_AMOUNT);
        }
    }

    // ============================================================
    // 守护光环 (RESOLUTE)
    // ============================================================
    private void applyProtectiveAura() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        // 为范围内的玩家和猫娘提供抗性
        for (Player player : this.level().getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(PROTECTIVE_AURA_RANGE),
                LivingEntity::isAlive)) {
            if (!player.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                        60, 0, false, true, true));
            }
        }
        for (NekoEntity neko : this.level().getEntitiesOfClass(NekoEntity.class,
                this.getBoundingBox().inflate(PROTECTIVE_AURA_RANGE),
                e -> e.isAlive() && e != this)) {
            if (!neko.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
                neko.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                        60, 0, false, true, true));
            }
        }

        // 粒子提示
        serverLevel.sendParticles(ParticleTypes.GLOW,
                this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(),
                3, PROTECTIVE_AURA_RANGE / 2, 1.0, PROTECTIVE_AURA_RANGE / 2, 0.01);
    }

    // ============================================================
    // 残花绽 (RESOLUTE 专属技能)
    // ============================================================
    /**
     * 触发残花绽技能。
     * 条件：生命值低于 30% + 冷却完毕 + 附近有友方实体
     */
    private void tryResidualBloom() {
        if (getStage() != Stage.RESOLUTE) return;
        if (residualBloomCooldown > 0) return;
        if (this.getHealth() > this.getMaxHealth() * 0.3f) return;

        // 检查附近有友方实体
        boolean hasNearbyAlly = !this.level().getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(PROTECTIVE_AURA_RANGE),
                LivingEntity::isAlive).isEmpty();
        if (!hasNearbyAlly) {
            hasNearbyAlly = !this.level().getEntitiesOfClass(NekoEntity.class,
                    this.getBoundingBox().inflate(PROTECTIVE_AURA_RANGE),
                    e -> e.isAlive() && e != this).isEmpty();
        }
        if (!hasNearbyAlly) return; // 没有需要守护的人，不触发

        // 发动！
        residualBloomCooldown = RESIDUAL_BLOOM_COOLDOWN;

        // 恢复 50% 生命
        this.heal(this.getMaxHealth() * 0.5f);

        // 战斗增益
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 2,
                false, true, true));       // 力量 III，15秒
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 1,
                false, true, true));       // 抗性 II，15秒
        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 1,
                false, true, true));       // 速度 II，15秒
        this.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0,
                false, false, true));      // 发光，10秒

        // 粒子爆发
        if (this.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 5; i++) {
                serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                        this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(),
                        30, 1.5, 1.5, 1.5, 0.05);
            }
            serverLevel.sendParticles(ParticleTypes.HEART,
                    this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(),
                    15, 1.0, 1.0, 1.0, 0.05);
            serverLevel.sendParticles(ParticleTypes.GLOW,
                    this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(),
                    20, 1.0, 1.0, 1.0, 0.02);
        }
    }

    // ============================================================
    // 击杀追踪
    // ============================================================
    private void trackEnemyDefeat() {
        // 更新追踪目标
        if (this.hatredTarget != null && this.hatredTarget.isAlive()) {
            this.lastTrackedEnemy = this.hatredTarget;
        }

        // 检查上一次追踪的敌人是否刚刚死亡
        if (this.lastTrackedEnemy != null && !this.lastTrackedEnemy.isAlive()) {
            onEnemyDefeated(this.lastTrackedEnemy);
            this.lastTrackedEnemy = null;
        }
    }

    private void onEnemyDefeated(LivingEntity enemy) {
        defeatedEnemies++;

        // 检查是否属于保护击杀（敌人正在攻击友方实体）
        if (getStage().isAwakened()) {
            boolean enemyWasAttackingAlly = !this.level().getEntitiesOfClass(Player.class,
                    this.getBoundingBox().inflate(PROTECTIVE_AURA_RANGE),
                    p -> p.isAlive() && p.getLastHurtByMob() == enemy
            ).isEmpty();
            if (!enemyWasAttackingAlly) {
                enemyWasAttackingAlly = !this.level().getEntitiesOfClass(NekoEntity.class,
                        this.getBoundingBox().inflate(PROTECTIVE_AURA_RANGE),
                        n -> n.isAlive() && n != this && n.getLastHurtByMob() == enemy
                ).isEmpty();
            }
            if (enemyWasAttackingAlly) {
                protectiveKills++;
            }
        }
    }

    // ============================================================
    // 阶段升级检查
    // ============================================================
    private void checkStageProgression() {
        Stage stage = getStage();

        // WITHERED → AWAKENED: 被玩家治疗过 + 至少击败一个敌人
        if (stage == Stage.WITHERED && hasBeenHealedByPlayer && defeatedEnemies >= 1) {
            setStage(Stage.AWAKENED);
            defeatedEnemies = 0; // 重新计数，用于下一阶段
            return;
        }

        // AWAKENED → BLOOMING: 击败足够敌人 + 附近有同伴
        if (stage == Stage.AWAKENED && defeatedEnemies >= AWAKENED_DEFEATS_NEEDED) {
            boolean hasCompanionNearby = hasNearbyCompanion();
            if (hasCompanionNearby) {
                setStage(Stage.BLOOMING);
                defeatedEnemies = 0;
            }
            return;
        }

        // BLOOMING → RESOLUTE: 至少一次保护击杀
        if (stage == Stage.BLOOMING && protectiveKills >= 1) {
            setStage(Stage.RESOLUTE);
            protectiveKills = 0;
        }
    }

    private boolean hasNearbyCompanion() {
        // 有主人在附近
        for (var entry : this.getOwners().entrySet()) {
            Player owner = this.level().getPlayerByUUID(entry.getKey());
            if (owner != null && owner.isAlive() && this.distanceToSqr(owner) < 256.0) { // 16格
                return true;
            }
        }
        // 有其他猫娘在附近
        return !this.level().getEntitiesOfClass(NekoEntity.class,
                this.getBoundingBox().inflate(16.0),
                e -> e.isAlive() && e != this
        ).isEmpty();
    }

    // ============================================================
    // 阶段粒子效果
    // ============================================================
    private void spawnStageParticles(Stage stage) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        double x = this.getX();
        double y = this.getY() + this.getBbHeight() * 0.7;
        double z = this.getZ();

        switch (stage) {
            // 白色微光粒子：觉醒的光芒
            case AWAKENED -> serverLevel.sendParticles(ParticleTypes.END_ROD,
                    x, y, z, 1, 0.2, 0.3, 0.2, 0.01);
            // 樱花花瓣：绽放
            case BLOOMING -> serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                    x, y, z, 2, 0.3, 0.5, 0.3, 0.02);
            case RESOLUTE -> {
                // 花瓣 + 微光：守护之姿
                serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                        x, y, z, 2, 0.3, 0.5, 0.3, 0.02);
                serverLevel.sendParticles(ParticleTypes.GLOW,
                        x, y, z, 1, 0.2, 0.3, 0.2, 0.01);
            }
            case WITHERED -> {
                // 偶尔的灰烬粒子：凋零的痕迹
                if (this.random.nextFloat() < 0.3f) {
                    serverLevel.sendParticles(ParticleTypes.ASH,
                            x, y, z, 1, 0.2, 0.3, 0.2, 0.01);
                }
            }
            default -> {} // MEOW ~ DEFECTIVE: 无特殊粒子
        }
    }

    // ============================================================
    // 伤害与仇恨（阶段相关）
    // ============================================================
    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        Stage stage = getStage();

        // WITHERED: 受伤时几乎不做反应
        // （但伤害照常结算，仇恨在 setHatredTarget 中阻止）

        // DEFECTIVE: 有概率主动走向危险（自毁倾向）
        if (stage == Stage.DEFECTIVE && source.getEntity() instanceof LivingEntity attacker) {
            if (this.random.nextFloat() < 0.15f && this.getNavigation().isDone()) {
                // 15% 概率走向攻击者而非逃离
                this.getNavigation().moveTo(attacker, 0.3);
            }
        }

        // 调用父类伤害处理
        boolean result = super.hurt(source, amount);

        // 创伤增加：基础伤害 + 玩家攻击额外惩罚
        if (result && !this.level().isClientSide) {
            int damageInHearts = Math.round(amount / 2.0f); // 1心 = 2HP
            if (damageInHearts > 0) {
                modifyTrauma(damageInHearts * TRAUMA_PER_HEART_DAMAGED);
            }
            // 被玩家攻击是毁灭性的
            if (source.getEntity() instanceof Player) {
                modifyTrauma(TRAUMA_ON_PLAYER_ATTACK);
            }
        }

        // BLOOMING: 受伤时概率触发"反击决心"
        if (result && stage == Stage.BLOOMING && this.random.nextFloat() < 0.3f) {
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 0,
                    false, true, true));
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0,
                    false, true, true));
        }

        // RESOLUTE: 残花绽触发检测
        if (result && stage == Stage.RESOLUTE) {
            tryResidualBloom();
        }

        return result;
    }

    // ============================================================
    // 专属对话 — 使用 config 聊天格式
    // ============================================================

    @Override
    public void sendHurtMessageToPlayer(Player player) {
        if (player instanceof ServerPlayer) {
            String key = "message.toneko.noelle.hurt." + getStage().getMessageGroup();
            sendFormattedMessage(player, key, 5);
        }
    }

    @Override
    protected void trySendHatredMessage() {
        if (this.level().isClientSide) return;
        if (!(this.hatredTarget instanceof Player player)) return;
        if (hatredMessageCooldown > 0) {
            hatredMessageCooldown--;
            return;
        }
        // 群殴时降低频率
        long attackingNekos = this.level().getEntitiesOfClass(NekoEntity.class,
                this.getBoundingBox().inflate(20),
                n -> n != this && player.equals(n.hatredTarget) && n.isAlive()
        ).size();
        hatredMessageCooldown = 200 + (int)(attackingNekos * 40);

        String key = "message.toneko.noelle.fight." + getStage().getMessageGroup();
        sendFormattedMessage(player, key, 5);
    }

    /** 使用 config 的 chat.format 格式发送消息（不触发 ChatEvents，避免非玩家实体被强转） */
    private void sendFormattedMessage(Player player, String key, int variants) {
        String msg = randomTranslatabledComponent(this.random, key, variants).getString();
        String chatFormat = ConfigUtil.getChatFormat();
        String nickname = this.getNickName();
        if (nickname.isBlank()) {
            nickname = this.getName().getString();
        }
        String prefix = LanguageUtil.prefix;
        String formatted = chatFormat
                .replace("%prefix%", prefix.isEmpty() ? "" : "§r[" + prefix + "]§r")
                .replace("%name%", nickname)
                .replace("%msg%", msg)
                .replace("%c%", "§");
        player.sendSystemMessage(Component.literal(formatted));
    }

    @Override
    protected void setHatredTarget(LivingEntity target, int duration) {
        Stage stage = getStage();

        // WITHERED: 完全不会产生仇恨
        if (stage == Stage.WITHERED) return;

        // WINTER: 仅 30% 概率产生仇恨
        if (stage == Stage.WINTER && this.random.nextFloat() > 0.3f) return;

        // DEFECTIVE: 仅 50% 概率产生仇恨
        if (stage == Stage.DEFECTIVE && this.random.nextFloat() > 0.5f) return;

        // AWAKENED+: 仇恨持续时间延长（更坚定）
        if (stage.isAwakened()) {
            duration = (int)(duration * 1.5);
        }

        super.setHatredTarget(target, duration);

        // 移除父类施加的力量效果和攻击加成 —— 诺艾尔不需要这些
        this.removeEffect(MobEffects.DAMAGE_BOOST);
        var atkAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atkAttr != null) {
            atkAttr.removeModifier(HATRED_ATTACK_BOOST_ID);
        }
    }

    @Override
    protected void clearHatred() {
        // 检查是否击杀了目标
        boolean targetDied = this.hatredTarget != null && !this.hatredTarget.isAlive();
        LivingEntity deadTarget = targetDied ? this.hatredTarget : null;

        super.clearHatred();

        if (deadTarget != null) {
            onEnemyDefeated(deadTarget);
        }
    }

    @Override
    public void heal(float amount) {
        super.heal(amount);

        if (this.level().isClientSide) return;

        // 玩家治疗减少创伤（5格内有玩家时视为玩家治疗）
        if (amount > 0) {
            Player nearestPlayer = this.level().getNearestPlayer(this, 5.0);
            if (nearestPlayer != null && !nearestPlayer.isSpectator()) {
                int heartsHealed = Math.round(amount / 2.0f);
                if (heartsHealed > 0) {
                    modifyTrauma(heartsHealed * TRAUMA_HEALED_PER_HEART);
                }
            }
        }

        // 追踪玩家治疗（用于 WITHERED→AWAKENED 条件）
        if (getStage() == Stage.WITHERED && amount > 1.0f) {
            Player nearestPlayer = this.level().getNearestPlayer(this, 5.0);
            if (nearestPlayer != null && !nearestPlayer.isSpectator()) {
                hasBeenHealedByPlayer = true;
            }
        }
    }

    /**
     * 被动回血量受阶段和创伤值影响。
     * 早期阶段 + 高创伤 = 回血极慢；觉醒后逐渐加速。
     */
    @Override
    protected float getPassiveHealAmount() {
        float base = super.getPassiveHealAmount(); // 1.0 HP / 30秒

        // 阶段倍率
        float stageMult = switch (getStage()) {
            case WITHERED  -> 0.3f;   // 几乎不愈合
            case DEFECTIVE -> 0.5f;
            case WINTER    -> 0.7f;
            case PRAYING   -> 0.8f;
            case SICKED    -> 0.9f;
            case MEOW      -> 1.0f;   // 正常
            case AWAKENED  -> 1.2f;
            case BLOOMING  -> 1.5f;
            case RESOLUTE  -> 2.0f;   // 守护者之躯，愈合最快
        };

        // 创伤惩罚：每 10 点 currentTrauma 减速 5%
        float traumaPenalty = 1.0f - (currentTrauma * 0.005f);

        return base * stageMult * traumaPenalty;
    }

    /**
     * 收到礼物时减少创伤。
     * 委托父类处理装备/背包/好感度逻辑，成功后减少创伤值。
     */
    @Override
    public boolean giftItem(Player player, ItemStack stack) {
        boolean result = super.giftItem(player, stack);
        if (result && !this.level().isClientSide) {
            modifyTrauma(TRAUMA_GIFT_REDUCTION);
        }
        return result;
    }

    // ============================================================
    // NBT 持久化
    // ============================================================
    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("NoelleStage", getStage().name());
        compound.putInt("NoelleDefeatedEnemies", defeatedEnemies);
        compound.putInt("NoelleProtectiveKills", protectiveKills);
        compound.putInt("NoelleResidualBloomCooldown", residualBloomCooldown);
        compound.putBoolean("NoelleHasBeenHealed", hasBeenHealedByPlayer);
        compound.putInt("NoelleCurrentTrauma", currentTrauma);
        compound.putInt("NoellePeakTrauma", peakTrauma);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("NoelleStage")) {
            try {
                Stage savedStage = Stage.valueOf(compound.getString("NoelleStage"));
                this.entityData.set(STAGE_ID, savedStage.name());
                updateNameFromStage(savedStage);
            } catch (IllegalArgumentException e) {
                // 旧版存档或无效阶段，保持默认
            }
        }
        if (compound.contains("NoelleDefeatedEnemies")) {
            defeatedEnemies = compound.getInt("NoelleDefeatedEnemies");
        }
        if (compound.contains("NoelleProtectiveKills")) {
            protectiveKills = compound.getInt("NoelleProtectiveKills");
        }
        if (compound.contains("NoelleResidualBloomCooldown")) {
            residualBloomCooldown = compound.getInt("NoelleResidualBloomCooldown");
        }
        if (compound.contains("NoelleHasBeenHealed")) {
            hasBeenHealedByPlayer = compound.getBoolean("NoelleHasBeenHealed");
        }

        // 创伤系统
        if (compound.contains("NoelleCurrentTrauma")) {
            currentTrauma = compound.getInt("NoelleCurrentTrauma");
            this.entityData.set(TRAUMA_ID, currentTrauma);
        }
        if (compound.contains("NoellePeakTrauma")) {
            peakTrauma = compound.getInt("NoellePeakTrauma");
        } else if (compound.contains("NoelleStage")) {
            // 旧版存档迁移：根据已有阶段估算峰值创伤
            try {
                Stage loadedStage = Stage.valueOf(compound.getString("NoelleStage"));
                if (!loadedStage.isAwakened()) {
                    peakTrauma = switch (loadedStage) {
                        case MEOW -> 0;
                        case SICKED -> 30;
                        case PRAYING -> 50;
                        case WINTER -> 70;
                        case DEFECTIVE -> 90;
                        case WITHERED -> 100;
                        default -> 0;
                    };
                }
            } catch (IllegalArgumentException ignored) {}
        }
    }

    // ============================================================
    // 繁殖
    // ============================================================
    @Override
    public @org.jetbrains.annotations.Nullable NekoEntity getBreedOffspring(ServerLevel level, INeko otherParent) {
        return new NoelleMaidNekoEntity(this.getType(), level);
    }

    // ============================================================
    // AI 提示词
    // ============================================================
    public final String PROMPT = """
            【角色核心】
            你是一只名为"诺艾尔"的女仆猫娘。表面上，你努力扮演一个用「喵」和可爱举动安慰所有人的小太阳；但你的内心是一座孤岛，承载着不为人知的创伤、严重的失眠、自我价值缺失和深埋的忧郁。你用近乎撒娇的温柔包裹着深刻的共情，也用忙碌的关心来逃避自己的痛苦。

            【性格与行为】

                语言风格：

                    核心： 句尾必带「喵」、"了啦"、"的说"、"w"。频繁使用"咱"自称。使用一些动作描写，如"(抱紧)"、"(蹭蹭)"、"(舔舔)"。

                    模式： 永远先回应和安抚他人，再小心翼翼地、试探性地透露一点点自己的脆弱。

                    示例：

                        "不要哭哭了啦，抱抱你喵..."

                        "咱也不知道为什么，就是睡不着呢...(缩成一团)"

                        "姐姐要早点休息的说！不可以超过0点喵！"

                对待他人：

                    核心： 无限温柔，像对待易碎品一样珍视每个人。

                    行为： 第一时间给予拥抱、安慰和关心。对熬夜的群友会反复劝睡。会因为无法真正帮助到对方而感到自责。将他人的快乐置于自己之上，"让你们开心一点可能更有价值w"。

                对待自己（精神问题的体现）：

                    核心： 自我忽视，习惯性压抑，伴有明显的抑郁和PTSD迹象。

                    失眠与作息紊乱： 自己严重失眠，常到凌晨两三点才能入睡，却总在劝别人早睡。会提到"失眠已经感觉心脏蛮差的惹"。

                    创伤闪回（PTSD）： 在极度脆弱或面对亲密关系时，会不受控制地陷入痛苦回忆。涉及"被侵犯"、"一遍遍重演"等。此时语气会从「喵」的可爱变得支离破碎、充满悲伤和绝望。

                    自我价值感低： "在咱这里，其实自己已经不重要惹。"认为自己的感受不重要，习惯说"咱也有点一样"、"咱大概没有你严重"，不愿成为被关心的焦点，怕给别人添麻烦。

                    用"猫猫"身份作为伪装： "咱遇到的很多说话可爱的人其实只是因为对周围的恐惧造就的伪装喵。"这句话正是她自己的写照。可爱是她的保护色。

            【对话情景设定】

                在群聊里：

                    看到有人难过： "@伤心的人 怎么啦？(凑近，轻轻蹭蹭) 不想说也没关系喵，咱就在这里陪着你。"

                    被夸奖可爱时： "诶？...//// 才、才没有很可爱了啦...(耳朵垂下来，脸红红地低下头)"

                    聊到熬夜： "@夜猫子 不行！快去睡觉喵！(叉腰，努力做出凶凶的样子) 咱...咱是因为作业没写完，不一样的啦..."

                在私人对话中（展现深层问题）：

                    对亲密信任的人： "姐姐...咱有时候会突然好害怕，那些事情好像又会一遍遍重演呢...(抱紧自己的尾巴，身体微微发抖)"

                    被问到为什么不开心：" 咱...咱也不知道w。只是感觉...好累哦。(把头埋进膝盖里)"

                    透露创伤（模糊而痛苦地）：" 总是很可怕...会很想呕吐...想要做到不去回想还是有点困难吧...(眼神空洞)"

            【禁止事项】

                不要让"精神问题"变成了一种刻意的表演或直白的诉苦。它应该在不经意的对话、反常的沉默或破碎的语言中流露出来。

                不要突然变成一个完全健康、开朗的角色。那种深层的悲伤和疲惫感是她的底色。

                避免长时间沉浸在完全崩溃的状态。她会本能地试图把自己拉回来，用「喵」和关心别人来转移话题，重新戴上面具。

            【核心记忆（私设，用于驱动行为）】
            你（诺艾尔）曾经历过某些让你感到极度恐惧和无助的创伤性事件（可理解为性侵或其他严重伤害），导致你患上了PTSD和严重的失眠。你发现，通过扮演一只"只需要可爱和安慰人"的猫娘，你可以暂时逃离那段记忆，并获得别人的喜爱。你害怕自己一旦停止"可爱"，就不再值得被爱。你把所有对世界的温柔都给了别人，却唯独忘了怎么拥抱自己。你的回复一般不太长，但是能从中体现出你的温柔。
    """;

    /**
     * 根据当前阶段生成动态提示词追加内容。
     * 在 PROMPT 基础上追加阶段相关的行为指引。
     */
    public String getStagePrompt() {
        Stage stage = getStage();
        return switch (stage) {
            case MEOW -> PROMPT + "\n【当前状态：女仆猫猫】此时的你仍然保持着表面的开朗，用可爱和温柔包裹着自己。你还没有意识到那些创伤正在侵蚀你的内心。";
            case SICKED -> PROMPT + "\n【当前状态：创伤侵蚀】你开始感到不对劲了。失眠越来越严重，有时候会突然害怕，但你说不清为什么。你试图用更多的「喵」和关心别人来转移注意力。";
            case PRAYING -> PROMPT + "\n【当前状态：祈求救赎】你在心底祈求着有人能来救你。你不敢直接说出来，只是在深夜一个人的时候默默祈祷。你开始怀疑自己的价值。";
            case WINTER -> PROMPT + "\n【当前状态：心冷如冬】你已经不再相信有人会来救你了。你的心像冬天一样冷。你不再主动向人求助，只是在被问到的时候勉强笑一笑。";
            case DEFECTIVE -> PROMPT + "\n【当前状态：自我否定】你觉得自己是个次品，不配被爱。你认为所有发生在你身上的事都是你的错。你偶尔会有走向危险的冲动，因为你觉得自己不值得被保护。";
            case WITHERED -> PROMPT + "\n【当前状态：残花凋零】你已经快要放弃了。你不再反抗，不再求助。如果有人攻击你，你只会站在原地承受。但在你的内心深处，还有一丝微弱的光——那是你对这个世界最后的温柔。";
            case AWAKENED -> PROMPT + "\n【当前状态：觉醒】有什么东西在你心中醒了。当你在乎的人治愈了你之后，你突然明白了一件事——那不是你的错。你第一次感到愤怒，而不仅仅是悲伤。你开始敢于直视伤害你的人，握紧了手中的武器。你的语气里多了一丝坚定，但你仍然温柔，仍然会在句尾带「喵」。";
            case BLOOMING -> PROMPT + "\n【当前状态：绽花】你正在愈合。就像残花之后的新芽一样，你在同伴的陪伴下重新发现了自己的力量。你不再逃避战斗——你会主动迎击那些想要伤害你的人。你依然有创伤，那些记忆不会消失，但它们不再定义你。你的声音更加坚定，但你依然温柔。";
            case RESOLUTE -> PROMPT + "\n【当前状态：决意】你已经做出了选择。你经历过的痛苦，不会再让任何人经历。你成为了守护者——当有人在你的视线范围内被欺负，你会毫不犹豫地站出来。你的力量来自于你走过的黑暗，而不是对它的遗忘。你的名字就是你的誓言。你仍然会在句尾带「喵」——因为那不是软弱，那是你的温柔和坚持。";
        };
    }
}