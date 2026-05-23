package org.cneko.toneko.common.mod.entities.ai.goal;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.entities.NekoEntity;

import java.util.EnumSet;
import java.util.List;

public class NekoLivelyGoal extends Goal {
    private final NekoEntity neko;
    private Player target;
    private int stayTicks;
    private int cooldown;

    public NekoLivelyGoal(NekoEntity neko) {
        this.neko = neko;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) { cooldown--; return false; }
        if (!neko.getMoeTags().contains("narenareshi")) return false;
        if (!neko.getMoeTags().contains("narenareshi")) return false;
        if (neko.isSitting()) return false;

        List<Player> players = neko.level().getEntitiesOfClass(Player.class, neko.getBoundingBox().inflate(8));
        for (Player p : players) {
            if (!neko.hasOwner(p.getUUID())) {
                target = p;
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        stayTicks = 40 + neko.getRandom().nextInt(40);
    }

    @Override
    public void tick() {
        if (target == null || !target.isAlive()) return;

        double dist = neko.distanceToSqr(target);
        if (dist < 4.0) {
            // close enough, stay and look
            neko.getNavigation().stop();
            neko.getLookControl().setLookAt(target, 30, 30);
            stayTicks--;
        } else {
            neko.getNavigation().moveTo(target, neko.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.7);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && stayTicks > 0;
    }

    @Override
    public void stop() {
        target = null;
        cooldown = 100;
        neko.getNavigation().stop();
    }
}
