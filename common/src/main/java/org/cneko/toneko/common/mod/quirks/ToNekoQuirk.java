package org.cneko.toneko.common.mod.quirks;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.quirks.Quirk;

public abstract class ToNekoQuirk extends Quirk implements ModQuirk {
    public ToNekoQuirk(String id) {
        super(id);
    }

    abstract public int getInteractionValue();

    @Override
    public InteractionResult onNekoInteraction(Player owner, Level world, InteractionHand hand, INeko nekoPlayer, EntityHitResult hitResult) {
        return ModQuirk.super.onNekoInteraction(owner, world, hand, nekoPlayer, hitResult);
    }
}
