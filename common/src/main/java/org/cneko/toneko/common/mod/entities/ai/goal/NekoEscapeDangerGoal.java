package org.cneko.toneko.common.mod.entities.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.cneko.toneko.common.mod.entities.NekoEntity;

import java.util.EnumSet;
import java.util.List;

public class NekoEscapeDangerGoal extends Goal {
    private final NekoEntity neko;
    private int dangerTick = 0;

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
        return false;
    }

    @Override
    public void start() {
        dangerTick = 40;
        boolean isYowaki = neko.getMoeTags().contains("yowaki");
        double speed = isYowaki ? 1.5 : 1.2;

        Vec3 target;
        if (neko.isInWater()) {
            target = neko.position().add(0, 2, 0);
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

    @Override
    public boolean canContinueToUse() {
        return dangerTick-- > 0 && !neko.getNavigation().isDone();
    }
}
