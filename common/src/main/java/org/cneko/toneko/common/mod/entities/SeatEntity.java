package org.cneko.toneko.common.mod.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.cneko.toneko.common.mod.api.EntityPoseManager;
import org.cneko.toneko.common.mod.blocks.ShengDengBlock;
import org.cneko.toneko.common.mod.packets.EntityPosePayload;
import org.jetbrains.annotations.NotNull;

/**
 * 隐形座椅实体。
 * 机制：
 * - 大排档 Buff（≥4 人围坐）
 * - 冬天冰凉（寒冷群系缓慢冻结）
 * - 夏天烫手（炎热群系提示）
 * - 腿麻（坐太久站起缓慢）
 * - 雨水洗凳（雨天粒子特效）
 * - 省凳印彩蛋
 */
public class SeatEntity extends Entity {

    private int sitTicks = 0;
    private int buffCheckCooldown = 0;
    private int biomeCheckCooldown = 0;
    private int rainParticleCooldown = 0;

    // ==================== 摇晃失衡（按移动键会摔倒） ====================
    /** 失衡阈值：达到即摔倒 */
    private static final float BALANCE_THRESHOLD = 22f;
    /** 按移动键时每 tick 的基础失衡积累 */
    private static final float BALANCE_GAIN_BASE = 0.55f;
    /** 凳子每 1 格高度带来的额外积累（越高越容易倒） */
    private static final float BALANCE_GAIN_PER_HEIGHT = 0.7f;
    /** 松手后每 tick 的恢复量 */
    private static final float BALANCE_STABILIZE = 0.55f;

    private float imbalance = 0f;
    /** 座位绑定的省凳格（服务端；座位悬在柱顶上方时不能直接拿 blockPosition） */
    private BlockPos seatCell = null;

    /**
     * 骑乘偏移：1.21.1 默认骑乘位置 = 实体位置 + 实体自身高度（会把玩家抬到
     * 凳面 + 1.8 格，看起来凭空站高了）。这里让玩家臀部沉入凳面下方，
     * 坐姿头顶 ≈ 凳面 + 0.85 格，即真实的坐高。
     */
    private static final double RIDER_OFFSET = -0.6;

    /**
     * 柱顶格座位额外上抬：仅 -0.6 的坐深会让玩家臀部陷进凳面（"坐在凳子里面"），
     * 这里把柱顶格的整个座位再抬高一点。固定值、只作用于最顶层一次，
     * 不参与格子高度计算，因此绝不会随层数叠加。
     */
    private static final double TOP_SEAT_LIFT = 0.6;

    public SeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setInvisible(true);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            if (this.getPassengers().isEmpty()) {
                this.discard();
                return;
            }
            sitTicks++;

            // 大排档 Buff：每 5 秒
            if (--buffCheckCooldown <= 0) {
                buffCheckCooldown = 100;
                checkDapaidangBuff();
            }

            // 环境温度检查：每 2 秒
            if (--biomeCheckCooldown <= 0) {
                biomeCheckCooldown = 40;
                checkBiomeEffects();
            }

            // 摇晃失衡：按 WASD 会摔倒，凳子越高越容易倒
            Entity passenger = this.getPassengers().get(0);
            if (passenger instanceof Player player) {
                tickBalance(player);
            }
        } else {
            // 客户端：雨水粒子
            if (--rainParticleCooldown <= 0) {
                rainParticleCooldown = 30;
                if (this.level().isRainingAt(this.blockPosition())) {
                    spawnRainCleanParticles();
                }
            }
        }
    }

    // ==================== 大排档 Buff ====================
    private void checkDapaidangBuff() {
        if (this.getPassengers().isEmpty()) return;
        Entity passenger = this.getPassengers().get(0);
        if (!(passenger instanceof Player player)) return;

        BlockPos center = this.blockPosition();
        AABB searchArea = new AABB(
                center.getX() - 3, center.getY() - 3, center.getZ() - 3,
                center.getX() + 3, center.getY() + 3, center.getZ() + 3
        );

        int seatedCount = 0;
        for (SeatEntity seat : this.level().getEntitiesOfClass(SeatEntity.class, searchArea)) {
            if (!seat.getPassengers().isEmpty()) seatedCount++;
        }

        if (seatedCount >= 4) {
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 200, 0,
                    false, true));
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 300, 0,
                    false, true));
            player.displayClientMessage(
                    Component.translatable("block.toneko.sheng_deng.buff"), true);
        }
    }

    // ==================== 环境温度 ====================
    private void checkBiomeEffects() {
        if (this.getPassengers().isEmpty()) return;
        Entity passenger = this.getPassengers().get(0);
        if (!(passenger instanceof Player player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        BlockPos pos = this.blockPosition();
        Biome biome = this.level().getBiome(pos).value();
        float temperature = biome.getBaseTemperature();

        // 🥶 冬天冰凉：温度 <= 0.15（积雪/冰原群系）
        if (temperature <= 0.15f) {
            // 缓慢冻结：5 秒后开始掉血（参考细雪的冰冻机制）
            if (sitTicks > 100) {
                player.setTicksFrozen(Math.min(player.getTicksFrozen() + 3,
                        player.getTicksRequiredToFreeze() + 20));
                if (sitTicks % 60 == 0) {
                    player.displayClientMessage(
                            Component.translatable("block.toneko.sheng_deng.cold"), true);
                }
            }
        }

        // 🔥 夏天烫手：温度 >= 1.5（沙漠/恶地/下界）
        if (temperature >= 1.5f) {
            if (sitTicks > 60 && sitTicks % 80 == 0) {
                player.displayClientMessage(
                        Component.translatable("block.toneko.sheng_deng.hot"), true);
                player.hurt(player.damageSources().onFire(), 0.5f); // 象征性烫伤
                level().playSound(null, this.blockPosition(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3f, 2.0f);
            }
        }
    }

    // ==================== 摇晃失衡（按移动键会摔倒） ====================
    private void tickBalance(Player player) {
        // 旁观者免疫；创造模式也会失衡（方便测试/观赏），但摔倒时无伤
        if (player.isSpectator()) return;

        // 按 WASD（前后 zza / 左右 xxa）→ 失衡；松手恢复
        boolean moving = Math.abs(player.zza) > 0.5f || Math.abs(player.xxa) > 0.5f;

        if (!moving) {
            imbalance = Math.max(0f, imbalance - BALANCE_STABILIZE);
            return;
        }

        BlockPos cell = findSeatCell();
        if (cell == null) return;
        double height = ShengDengBlock.getColumnHeight(this.level(), cell);

        imbalance += BALANCE_GAIN_BASE + BALANCE_GAIN_PER_HEIGHT * height;

        // 摇晃预警：失衡过半时凳子吱呀作响
        if (imbalance > BALANCE_THRESHOLD * 0.6f && this.tickCount % 10 == 0) {
            this.level().playSound(null, cell, SoundEvents.BAMBOO_WOOD_HIT,
                    SoundSource.BLOCKS, 0.4f, 0.5f);
        }

        if (imbalance >= BALANCE_THRESHOLD) {
            topple(player, height);
        }
    }

    /** 找到座位绑定的省凳格（实体重载后绑定丢失时向下兜底搜索） */
    private BlockPos findSeatCell() {
        if (seatCell != null && this.level().getBlockState(seatCell).getBlock() instanceof ShengDengBlock) {
            return seatCell;
        }
        BlockPos pos = this.blockPosition();
        for (int i = 0; i < 4; i++) {
            BlockPos p = pos.below(i);
            if (this.level().getBlockState(p).getBlock() instanceof ShengDengBlock) {
                seatCell = p.immutable();
                return seatCell;
            }
        }
        return null;
    }

    /** 摔倒：从凳子上摔下来，凳子越高摔得越狠 */
    private void topple(Player player, double height) {
        // 摔下凳子
        player.stopRiding();
        imbalance = 0f;

        // 摔趴下：参考 NekoCommand 的 getDown（Pose.SWIMMING 爬行姿态）
        if (player instanceof ServerPlayer sp) {
            EntityPoseManager.setPose(player, Pose.SWIMMING);
            ServerPlayNetworking.send(sp, new EntityPosePayload(Pose.SWIMMING,
                    player.getUUID().toString(), true));
        }

        // 创造模式：只摔不伤（无伤害、无击退），便于测试/观赏
        boolean safe = player.isCreative();
        if (!safe) {
            // 摔伤
            float damage = 0.5f + (float) height * 0.5f;
            player.hurt(player.damageSources().fall(), damage);

            // 被凳子掀翻击退，越高飞得越远
            double strength = 0.4 + height * 0.15;
            double dx = player.getRandom().nextDouble() - 0.5;
            double dz = player.getRandom().nextDouble() - 0.5;
            if (Math.abs(dx) + Math.abs(dz) < 0.2) dx = 0.5;
            player.setDeltaMovement(player.getDeltaMovement().add(dx * strength, 0.3, dz * strength));
            player.hurtMarked = true;
        }

        // 特效
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.POOF,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    8, 0.3, 0.2, 0.3, 0.05);
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.BAMBOO_WOOD_BREAK,
                SoundSource.BLOCKS, 1.0f, 0.8f);
        player.displayClientMessage(
                Component.translatable("block.toneko.sheng_deng.tumble"), true);
    }

    // ==================== 雨水洗凳粒子 ====================
    private void spawnRainCleanParticles() {
        BlockPos pos = this.blockPosition();
        this.level().addParticle(ParticleTypes.SPLASH,
                pos.getX() + 0.5, pos.getY() + 0.55, pos.getZ() + 0.5,
                0.0, 0.05, 0.0);
        this.level().addParticle(ParticleTypes.BUBBLE_POP,
                pos.getX() + this.random.nextDouble(),
                pos.getY() + 0.55,
                pos.getZ() + this.random.nextDouble(),
                0.0, 0.02, 0.0);
    }

    // ==================== 下马处理 ====================
    @Override
    public void remove(@NotNull RemovalReason reason) {
        if (!this.level().isClientSide) {
            handleDismountEffects();
        }
        super.remove(reason);
    }

    private void handleDismountEffects() {
        if (this.getPassengers().isEmpty()) return;
        Entity passenger = this.getPassengers().get(0);
        if (!(passenger instanceof Player player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        // 🦵 腿麻了：坐 >30 秒站起 → 缓慢 II 3 秒
        if (sitTicks > 600) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    60, 1, false, false, true));
            player.displayClientMessage(
                    Component.translatable("block.toneko.sheng_deng.numb"), true);
        }

        // 省凳印彩蛋
        if (sitTicks > 600) {
            player.displayClientMessage(
                    Component.translatable("block.toneko.sheng_deng.imprint"), false);
        }

        // 离开音效
        level().playSound(null, this.blockPosition(),
                SoundEvents.BAMBOO_WOOD_BREAK, SoundSource.BLOCKS, 0.4f, 0.8f);
    }

    // ==================== 基础方法 ====================
    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {}
    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {}
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override
    protected boolean canAddPassenger(@NotNull Entity passenger) {
        return this.getPassengers().isEmpty();
    }

    /**
     * 骑乘位置 = 座位位置 + RIDER_OFFSET。
     * 不调用 super：默认会额外加上座位实体自身高度（1.8 格），导致坐姿凭空高出。
     */
    @Override
    public net.minecraft.world.phys.Vec3 getPassengerRidingPosition(Entity passenger) {
        return this.position().add(0, RIDER_OFFSET, 0);
    }
    @Override
    public boolean isPickable() { return true; }

    /** 在方块上方生成座椅并让玩家坐上去 */
    public static void sitOnBlock(Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return;
        EntityType<?> seatType = ToNekoEntities.SEAT_ENTITY;
        if (seatType == null) return;

        // 先识别整根柱子：点击任意一格，都坐在柱子最顶格（避免穿进上方格子）
        BlockPos top = ShengDengBlock.findColumnTop(level, pos);
        // 只对柱顶格加一次固定上抬（避免坐姿陷进凳面），绝不累加
        double topY = ShengDengBlock.getCellTopY(level, top) + TOP_SEAT_LIFT;

        SeatEntity seat = new SeatEntity(seatType, level);
        // 坐在柱顶格顶面（getCellTopY 已累加整柱高度）
        seat.setPos(top.getX() + 0.5, topY, top.getZ() + 0.5);
        seat.seatCell = top.immutable();
        level.addFreshEntity(seat);
        player.startRiding(seat, true);
        // 显式设置骑乘位置：1.21.1 的 positionRider 只在 rideTick 里被调用，
        // 而 rideTick 对外部 tick 流程不可靠，不显式 setPos 的话玩家会停在默认高度。
        player.setPos(top.getX() + 0.5, topY + RIDER_OFFSET, top.getZ() + 0.5);
        level.playSound(null, top, SoundEvents.BAMBOO_WOOD_PLACE,
                SoundSource.BLOCKS, 0.6f, 1.2f);
    }
}
