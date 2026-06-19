package org.cneko.toneko.common.mod.entities.ai.goal;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.entities.ai.BehaviorPriority;

import java.util.EnumSet;

public class NekoChunibyoGoal extends Goal {
    private final NekoEntity neko;
    private int duration;
    private int cooldown;

    public NekoChunibyoGoal(NekoEntity neko) {
        this.neko = neko;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!neko.getMoeTags().contains("chunibyo")) return false;
        if (cooldown > 0) { cooldown--; return false; }
        if (neko.isSitting()) return false;
        // only when not navigating anywhere
        return neko.getNavigation().isDone() && neko.getRandom().nextFloat() < 0.01f;
    }

    @Override
    public void start() {
        duration = 20 + neko.getRandom().nextInt(20);
        neko.getNekoBrain().stopMoving(this);
    }

    @Override
    public void tick() {
        if (!(neko.level() instanceof ServerLevel serverLevel)) return;

        // spawn particles in a spiral pattern
        double t = (double)(duration % 40) / 40.0 * Math.PI * 4;
        double radius = 0.5 + t * 0.05;
        for (int i = 0; i < 3; i++) {
            double angle = t + i * Math.PI * 2 / 3;
            double px = neko.getX() + Math.cos(angle) * radius;
            double pz = neko.getZ() + Math.sin(angle) * radius;
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    px, neko.getY() + 1.5, pz,
                    1, 0, 0.1, 0, 0.02);
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    px, neko.getY() + 2.0, pz,
                    1, 0, 0.1, 0, 0.5);
        }
        // look dramatic
        neko.getLookControl().setLookAt(
                neko.getX() + Math.cos(t) * 3,
                neko.getY() + 1,
                neko.getZ() + Math.sin(t) * 3,
                30, 30);
    }

    @Override
    public boolean canContinueToUse() {
        return duration-- > 0;
    }

    @Override
    public void stop() {
        cooldown = 200 + neko.getRandom().nextInt(200);
    }
}
