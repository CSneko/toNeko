package org.cneko.toneko.common.mod.entities.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.cneko.toneko.common.mod.entities.NekoEntity;

import java.util.EnumSet;
import java.util.List;

public class NekoEscapeDangerGoal extends Goal {
    private final NekoEntity neko;
    private int dangerTick;
    private int recalcTimer;
    private double speed;
    private boolean inWater;

    public NekoEscapeDangerGoal(NekoEntity neko) {
        this.neko = neko;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (neko.isOnFire() || neko.isInLava() || neko.isInWater() || neko.fallDistance > 4.0F) {
            return true;
        }
        if (neko.getLastDamageSource() != null && neko.getLastDamageSource().getEntity() == null) {
            return true;
        }
        // yowaki / paranoia 逃离陌生人（可通过 shouldFleeFromStrangers() 禁用）
        if (neko.shouldFleeFromStrangers()) {
            List<String> tags = neko.getMoeTags();
            if (tags.contains("yowaki")) {
                if (neko.getHealth() < neko.getMaxHealth() * 0.5) return true;
                for (Player p : neko.level().getEntitiesOfClass(Player.class, neko.getBoundingBox().inflate(8))) {
                    if (!neko.hasOwner(p.getUUID())) return true;
                }
            }
            if (tags.contains("paranoia")) {
                for (Player p : neko.level().getEntitiesOfClass(Player.class, neko.getBoundingBox().inflate(10))) {
                    if (!neko.hasOwner(p.getUUID())) return true;
                }
            }
        }
        return false;
    }

    @Override
    public void start() {
        dangerTick = 200;
        recalcTimer = 0;
        inWater = neko.isInWater();
        double baseSpeed = neko.getAttributeValue(Attributes.MOVEMENT_SPEED);
        speed = neko.getMoeTags().contains("yowaki") ? baseSpeed * 2.0 : baseSpeed * 1.6;

        Vec3 target;
        if (inWater) {
            target = findShore();
            if (target == null) target = randomFlee();
        } else if (neko.isOnFire()) {
            target = findWater();
            if (target == null) target = randomFlee();
        } else {
            target = fleeFromPlayers();
            if (target == null) target = randomFlee();
        }
        if (target != null) {
            neko.getNavigation().moveTo(target.x, target.y, target.z, speed);
        }
    }

    @Override
    public void tick() {
        // 在水中时定期重新搜索岸边（寻路失效后换个方向重试）
        if (neko.isInWater()) {
            if (--recalcTimer <= 0) {
                recalcTimer = 40;
                Vec3 shore = findShore();
                if (shore != null) {
                    neko.getNavigation().moveTo(shore.x, shore.y, shore.z, speed);
                } else {
                    Vec3 flee = randomFlee();
                    neko.getNavigation().moveTo(flee.x, flee.y, flee.z, speed);
                }
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        // 仍未脱离危险则持续运行，不检查 isDone()（水中寻路常瞬间失败会导致频繁启停）
        if (--dangerTick > 0) return true;
        if (neko.isInWater() || neko.isOnFire() || neko.isInLava()) {
            dangerTick = 200; // 重置计时器，继续逃跑
            return true;
        }
        return false;
    }

    @Override
    public void stop() {
        neko.getNavigation().stop();
    }

    private Vec3 findShore() {
        BlockPos nekoPos = neko.blockPosition();
        for (int r = 1; r <= 16; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    BlockPos pos = nekoPos.offset(dx, 0, dz);
                    BlockPos above = pos.above();
                    if (!neko.level().getFluidState(pos).is(FluidTags.WATER)
                            && !neko.level().getFluidState(above).is(FluidTags.WATER)
                            && neko.level().isEmptyBlock(above)) {
                        return Vec3.atBottomCenterOf(pos).add(0, 0.5, 0);
                    }
                }
            }
        }
        return null;
    }

    private Vec3 findWater() {
        for (int i = 0; i < 10; i++) {
            BlockPos pos = neko.blockPosition().offset(
                    neko.getRandom().nextInt(12) - 6, 0, neko.getRandom().nextInt(12) - 6);
            if (neko.level().getFluidState(pos).is(FluidTags.WATER)) {
                return Vec3.atBottomCenterOf(pos);
            }
        }
        return null;
    }

    private Vec3 fleeFromPlayers() {
        Player threat = null;
        double nearest = Double.MAX_VALUE;
        for (Player p : neko.level().getEntitiesOfClass(Player.class, neko.getBoundingBox().inflate(10))) {
            if (neko.hasOwner(p.getUUID())) continue;
            double d = neko.distanceToSqr(p);
            if (d < nearest) { nearest = d; threat = p; }
        }
        if (threat != null) {
            return neko.position().subtract(threat.position()).normalize().scale(8).add(neko.position());
        }
        return null;
    }

    private Vec3 randomFlee() {
        return neko.position().add(neko.getRandom().nextDouble() * 8 - 4, 0, neko.getRandom().nextDouble() * 8 - 4);
    }
}
