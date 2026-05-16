package org.cneko.toneko.common.mod.entities.ai.goal;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.cneko.toneko.common.mod.entities.NekoEntity;

import java.util.EnumSet;

public class NekoParanoiaGoal extends Goal {
    private final NekoEntity neko;
    private int scanTimer;

    public NekoParanoiaGoal(NekoEntity neko) {
        this.neko = neko;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!neko.getMoeTags().contains("paranoia")) return false;
        if (scanTimer-- > 0) return false;
        scanTimer = 40;

        for (Player p : neko.level().getEntitiesOfClass(Player.class, neko.getBoundingBox().inflate(8))) {
            if (!neko.hasOwner(p.getUUID())) return true;
        }
        return false;
    }

    @Override
    public void start() {
        Player threat = null;
        double nearest = Double.MAX_VALUE;
        for (Player p : neko.level().getEntitiesOfClass(Player.class, neko.getBoundingBox().inflate(8))) {
            if (neko.hasOwner(p.getUUID())) continue;
            double d = neko.distanceToSqr(p);
            if (d < nearest) { nearest = d; threat = p; }
        }
        if (threat != null) {
            Vec3 away = neko.position().subtract(threat.position()).normalize().scale(8);
            neko.getNavigation().moveTo(
                    neko.getX() + away.x, neko.getY(), neko.getZ() + away.z, 1.0);
        }
    }

    @Override
    public boolean canContinueToUse() {
        // keep moving away until no unknown players within 16 blocks
        if (neko.getNavigation().isDone()) return false;
        for (Player p : neko.level().getEntitiesOfClass(Player.class, neko.getBoundingBox().inflate(16))) {
            if (!neko.hasOwner(p.getUUID())) return true;
        }
        return false;
    }

    @Override
    public void stop() {
        neko.getNavigation().stop();
    }
}
