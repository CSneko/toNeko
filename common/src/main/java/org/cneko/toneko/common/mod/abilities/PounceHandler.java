package org.cneko.toneko.common.mod.abilities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.cneko.toneko.common.mod.entities.INeko;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.cneko.toneko.common.Bootstrap.LOGGER;
import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

/**
 * 扑跃（Pounce）：猫娘按住潜行 + 疾跑蓄力，松开潜行后沿视线方向弹出。
 *
 * <p>流程：
 * <ol>
 *   <li>客户端 {@code PounceClientHandler} 负责按键、蓄力条与本地预测；</li>
 *   <li>松开潜行时发 {@link org.cneko.toneko.common.mod.packets.PouncePayload}；</li>
 *   <li>服务端在本类中重新计算冲量并覆盖客户端预测，同时挂 3 秒伤害倍率、免疫摔落；</li>
 *   <li>起跳至落地期间 {@code PlayerEntityMixin} 把姿势钉成 {@code Pose.SWIMMING}（趴下/游泳）。</li>
 * </ol>
 *
 * <p>所有状态都是「服务端权威、客户端只做预测」，与 {@code ClimbWallHandler} 同一套写法。
 */
public final class PounceHandler {
    /** 蓄力上限：2 秒 */
    public static final int MAX_CHARGE_TICKS = 40;
    /** 最小有效蓄力：0.25 秒，低于此值视为误触 */
    public static final int MIN_CHARGE_TICKS = 5;
    /** 扑跃后的冷却 */
    public static final int COOLDOWN_TICKS = 10;

    /** 伤害倍率 = 2 + 0.1 × 猫娘等级（通过 ADD_MULTIPLIED_TOTAL 实现，3 秒） */
    public static final double DAMAGE_MULTIPLIER_BASE = 2.0;
    public static final double DAMAGE_MULTIPLIER_PER_LEVEL = 0.1;
    /** 伤害倍率持续时间：3 秒 */
    public static final int DAMAGE_BONUS_TICKS = 60;
    /** 扑跃期间完全免疫摔落伤害（FALL_DAMAGE_MULTIPLIER 归零） */
    public static final double FALL_DAMAGE_NEGATION = -1.0;

    /** 基础冲量倍率：原 0.5 的三倍（1.5），0 级即为改版前满蓄力的 1.5 倍 */
    private static final double BASE_DISTANCE_SCALE = 1.5;
    /** 每 1 级猫娘等级增加的距离倍率：基础 × (1 + 0.02 × 等级) */
    private static final double DISTANCE_SCALE_PER_LEVEL = 0.02;

    /** 满蓄力冲量大小（沿视线方向，1.35 格/tick） */
    private static final double MAX_POWER = 1.35;

    private static final Identifier DAMAGE_BONUS_ID = toNekoLoc("pounce_damage_bonus");
    private static final Identifier FALL_REDUCTION_ID = toNekoLoc("pounce_fall_reduction");

    /** 攻击力加成的到期 tick（服务端 tick） */
    private static final Map<UUID, Integer> BUFF_EXPIRY = new ConcurrentHashMap<>();
    /** 仍在扑跃空中的玩家：UUID → 已飞行 tick */
    private static final Map<UUID, Integer> AIRBORNE = new ConcurrentHashMap<>();
    /** 上次扑跃的 tick，用于冷却 */
    private static final Map<UUID, Integer> LAST_LAUNCH = new ConcurrentHashMap<>();
    /** 最近一次确认站在地面的服务端 tick（容忍 1~2 tick 的起跳/网络时序偏差） */
    private static final Map<UUID, Integer> LAST_GROUND_TICK = new ConcurrentHashMap<>();

    private PounceHandler() {
    }

    // ------------------------------------------------------------------
    // 服务端入口
    // ------------------------------------------------------------------

    /**
     * 处理一次扑跃释放。客户端上报的蓄力值不可信，这里统一校验与 clamp。
     */
    public static void launch(ServerPlayer player, int chargeTicks) {
        if (!(player instanceof INeko neko) || !neko.isNeko()) {
            reject(player, "not_neko");
            return;
        }
        if (!player.isAlive() || player.isSpectator() || player.isPassenger()) {
            reject(player, "invalid_state");
            return;
        }
        if (player.isFallFlying() || player.isInWater()) {
            reject(player, "in_water_or_flying");
            return;
        }
        UUID uuid = player.getUUID();
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        int now = server == null ? 0 : server.getTickCount();
        // 必须在地面起跳；允许 3 tick 的时序偏差（客户端起跳预测可能先于本包到达）
        Integer lastGround = LAST_GROUND_TICK.get(uuid);
        boolean grounded = player.onGround() || (lastGround != null && now - lastGround <= 3);
        if (!grounded) {
            reject(player, "not_grounded onGround=" + player.onGround() + " lastGround=" + lastGround);
            return;
        }
        if (chargeTicks < MIN_CHARGE_TICKS) {
            reject(player, "charge_too_short " + chargeTicks);
            return;
        }
        if (AIRBORNE.containsKey(uuid)) {
            reject(player, "already_pouncing");
            return;
        }
        Integer last = LAST_LAUNCH.get(uuid);
        if (last != null && now - last < COOLDOWN_TICKS) {
            reject(player, "cooldown");
            return;
        }

        int ticks = Mth.clamp(chargeTicks, MIN_CHARGE_TICKS, MAX_CHARGE_TICKS);
        float t = ticks / (float) MAX_CHARGE_TICKS;

        // 先摘掉 onGround，避免起跳这一 tick 吃到地面摩擦（否则水平速度会被砍掉近一半）
        player.setOnGround(false);
        player.setDeltaMovement(computeImpulse(player, t));
        player.hurtMarked = true;
        player.resetFallDistance();

        LAST_LAUNCH.put(uuid, now);
        AIRBORNE.put(uuid, 0);
        applyFallReduction(player);
        applyDamageBonus(player, now);
        LOGGER.info("[pounce] launch {} ticks={}", player.getName().getString(), ticks);

        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS,
                1.0f, 0.85f + 0.3f * t);
    }

    /**
     * 计算扑跃冲量（客户端预测与服务端权威共用，保证两端一致）。
     *
     * <p>方向完全取视线方向（含俯仰），大小只由蓄力进度与猫娘等级决定：
     * 同样的蓄力 = 同样的冲量大小，抬头就飞、平视就贴地平推、看地就往下扑。
     *
     * @param player 玩家
     * @param t      蓄力进度 0~1
     */
    public static Vec3 computeImpulse(Player player, double t) {
        double clamped = Mth.clamp(t, 0.0, 1.0);
        Vec3 look = player.getLookAngle();
        // 开方：距离/高度都与 (1 + 0.02 × 等级) 近似线性增长，避免高等级时按平方爆炸
        double speed = MAX_POWER * clamped * Math.sqrt(distanceScale(nekoLevelOf(player)));
        return new Vec3(look.x * speed, look.y * speed, look.z * speed);
    }

    /** 扑跃冲量倍率：0.5 × (1 + 0.02 × 等级)，冲量大小与水平距离都按此线性成长 */
    public static double distanceScale(float nekoLevel) {
        return BASE_DISTANCE_SCALE * (1.0 + DISTANCE_SCALE_PER_LEVEL * Math.max(0.0f, nekoLevel));
    }

    /** 扑跃伤害倍率：2 + 0.1 × 猫娘等级 */
    public static double damageMultiplier(float nekoLevel) {
        return DAMAGE_MULTIPLIER_BASE + DAMAGE_MULTIPLIER_PER_LEVEL * Math.max(0.0f, nekoLevel);
    }

    /** 取猫娘等级（非猫娘按 0 处理） */
    public static float nekoLevelOf(Player player) {
        return player instanceof INeko neko ? neko.getNekoLevel() : 0.0f;
    }

    /** 该玩家此刻是否处于「扑跃后到落地前」的空中阶段（双端可调用）。 */
    public static boolean isPouncing(Player player) {
        // 只在实际离地时强制姿势：起跳那一 tick 仍在地面、落地那一 tick 已经着地，
        // 两端都用同一判定，保证「落地当 tick 立刻恢复直立」。
        if (player.onGround()) {
            return false;
        }
        return player.level().isClientSide()
                ? PounceClientState.isPouncing(player.getUUID())
                : AIRBORNE.containsKey(player.getUUID());
    }

    /** 是否持有扑跃的攻击力加成（服务端）。 */
    public static boolean hasDamageBonus(ServerPlayer player) {
        return BUFF_EXPIRY.containsKey(player.getUUID());
    }

    /**
     * 首次攻击命中后提前结束攻击力加成。
     * 未命中（挥空）不会触发原版攻击回调，因此不会消耗加成。
     */
    public static void consumeDamageBonus(ServerPlayer player) {
        if (BUFF_EXPIRY.remove(player.getUUID()) != null) {
            removeDamageBonus(player);
        }
    }

    public static void onServerTick(MinecraftServer server) {
        int now = server.getTickCount();
        recordGroundStates(server, now);
        tickDamageBonus(server, now);
        tickAirborne(server);
    }

    /** 每 tick 记录谁站在地面上，供扑跃做 3 tick 内的时序容错 */
    private static void recordGroundStates(MinecraftServer server, int now) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.onGround()) {
                LAST_GROUND_TICK.put(player.getUUID(), now);
            }
        }
    }

    private static void reject(ServerPlayer player, String reason) {
        LOGGER.info("[pounce] rejected {}: {}", player.getName().getString(), reason);
    }

    // ------------------------------------------------------------------
    // 内部
    // ------------------------------------------------------------------

    private static void tickDamageBonus(MinecraftServer server, int now) {
        if (BUFF_EXPIRY.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Integer>> it = BUFF_EXPIRY.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !player.isAlive() || now >= entry.getValue()) {
                if (player != null) {
                    removeDamageBonus(player);
                }
                it.remove();
            }
        }
    }

    private static void tickAirborne(MinecraftServer server) {
        if (AIRBORNE.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Integer>> it = AIRBORNE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.isRemoved() || !player.isAlive() || player.isPassenger()) {
                if (player != null) {
                    clearFallReduction(player);
                }
                it.remove();
                continue;
            }
            int ticks = entry.getValue() + 1;
            entry.setValue(ticks);
            // 落地：恢复姿势（isPouncing 变 false 后由原版 updatePlayerPose 重算）、清摔落免疫
            boolean landed = ticks > 2 && player.onGround();
            if (landed) {
                spawnLandingDust(player);
            }
            // 入水后游泳姿势本来就是原版行为，直接结束扑跃状态
            if (landed || player.isInWater()) {
                clearFallReduction(player);
                it.remove();
            }
        }
    }

    private static void applyDamageBonus(ServerPlayer player, int now) {
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr == null) {
            return;
        }
        // 倍率 M 通过 ADD_MULTIPLIED_TOTAL 实现：最终攻击力 = 原值 × (1 + amount) = 原值 × M
        double amount = damageMultiplier(nekoLevelOf(player)) - 1.0;
        attr.addOrUpdateTransientModifier(new AttributeModifier(
                DAMAGE_BONUS_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));
        // 重复扑跃只刷新持续时间，不叠加
        BUFF_EXPIRY.put(player.getUUID(), now + DAMAGE_BONUS_TICKS);
    }

    private static void removeDamageBonus(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr != null) {
            attr.removeModifier(DAMAGE_BONUS_ID);
        }
    }

    private static void applyFallReduction(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER);
        if (attr == null) {
            return;
        }
        // -100% 基础倍率 → 摔落伤害归零：扑跃期间及落地瞬间完全不摔伤
        attr.addOrUpdateTransientModifier(new AttributeModifier(
                FALL_REDUCTION_ID, FALL_DAMAGE_NEGATION, AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        ));
    }

    private static void clearFallReduction(@Nullable ServerPlayer player) {
        if (player == null) {
            return;
        }
        AttributeInstance attr = player.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER);
        if (attr != null) {
            attr.removeModifier(FALL_REDUCTION_ID);
        }
    }

    /** 落地扬尘：纯原版粒子，不加自定义资源 */
    private static void spawnLandingDust(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(ParticleTypes.CLOUD,
                player.getX(), player.getY() + 0.05, player.getZ(),
                12, 0.35, 0.05, 0.35, 0.02);
    }
}
