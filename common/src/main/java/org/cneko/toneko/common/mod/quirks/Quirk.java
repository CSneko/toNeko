package org.cneko.toneko.common.mod.quirks;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.cneko.toneko.common.mod.entities.INeko;

@AllArgsConstructor
public abstract class Quirk implements ModQuirk {
    @Getter
    private final String id;

    abstract public int getInteractionValue();

    @Override
    public InteractionResult onNekoInteraction(Player owner, Level world, InteractionHand hand, INeko nekoPlayer, EntityHitResult hitResult) {
        return ModQuirk.super.onNekoInteraction(owner, world, hand, nekoPlayer, hitResult);
    }
}
