package org.cneko.toneko.common.mod.entities.ai.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.entities.ai.BehaviorPriority;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * 陪伴型目标：诺艾尔倾向于靠近主人或其他猫娘（包括玩家猫娘），而非独自闲逛。
 * 优先级高于 WaterAvoidingRandomStrollGoal（priority 6 > 7），
 * 确保她会优先寻找同伴而非随机走动。
 *
 * <p>搜索优先级：主人 > 其他猫娘（NekoEntity 或玩家猫娘）</p>
 */
public class NekoStayNearCompanionGoal extends Goal {
    private final NekoEntity neko;
    private LivingEntity companion;
    private int timeToRecalcPath;
    private static final double DETECTION_RANGE = 20.0;      // 检测同伴的范围
    private static final double COMFORT_RANGE = 5.0;         // 舒适距离，在此范围内无需移动
    private static final double MOVE_SPEED = 0.35;           // 走向同伴的速度

    public NekoStayNearCompanionGoal(NekoEntity neko) {
        this.neko = neko;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (neko.isSitting()) return false;

        // 寻找最近的同伴：主人优先，其次其他猫娘
        LivingEntity nearest = findNearestCompanion();
        if (nearest == null) return false;

        // 如果已经在舒适距离内，不需要移动
        double distSq = neko.distanceToSqr(nearest);
        if (distSq < COMFORT_RANGE * COMFORT_RANGE) return false;

        this.companion = nearest;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (neko.isSitting()) return false;
        if (companion == null || !companion.isAlive()) return false;

        double distSq = neko.distanceToSqr(companion);
        return distSq > COMFORT_RANGE * COMFORT_RANGE
                && distSq < DETECTION_RANGE * DETECTION_RANGE * 2;
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
    }

    @Override
    public void tick() {
        if (companion == null) return;
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 15;
            neko.getNekoBrain().submitMove(companion, MOVE_SPEED, BehaviorPriority.NORMAL, this);
        }
    }

    @Override
    public void stop() {
        this.companion = null;
        neko.getNekoBrain().stopMoving(this);
    }

    /**
     * 寻找最近的同伴。
     * 优先级：主人 > 其他 INeko（NekoEntity 或玩家猫娘）。
     * 主人即使是普通玩家也算；其他实体必须是 isNeko() == true。
     */
    private LivingEntity findNearestCompanion() {
        double rangeSq = DETECTION_RANGE * DETECTION_RANGE;

        // 1. 优先寻找最近的主人（在线且在范围内）
        LivingEntity nearestOwner = neko.getOwners().keySet().stream()
                .map(uuid -> neko.level().getPlayerByUUID(uuid))
                .filter(p -> p != null && p.isAlive() && !p.isSpectator()
                        && neko.distanceToSqr(p) < rangeSq)
                .min(Comparator.comparingDouble(neko::distanceToSqr))
                .orElse(null);
        if (nearestOwner != null) return nearestOwner;

        // 2. 其次寻找最近的猫娘同伴
        List<LivingEntity> companions = new ArrayList<>();

        // NekoEntity 实体
        companions.addAll(neko.level().getEntitiesOfClass(NekoEntity.class,
                neko.getBoundingBox().inflate(DETECTION_RANGE),
                e -> e.isAlive() && e != neko && e.isNeko()));

        // 玩家猫娘（isNeko() 为 true 的玩家）
        companions.addAll(neko.level().getEntitiesOfClass(Player.class,
                neko.getBoundingBox().inflate(DETECTION_RANGE),
                p -> p.isAlive() && !p.isSpectator() && p.isNeko()));

        return companions.stream()
                .min(Comparator.comparingDouble(neko::distanceToSqr))
                .orElse(null);
    }
}
