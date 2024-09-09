package org.cneko.toneko.common.mod.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.entities.INeko;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Player.class)
public abstract class PlayerEntityMixin implements INeko {
    @Override
    public LivingEntity getEntity() {
        return (Player)(Object) this;
    }

    @Override
    public boolean isPlayer() {
        return true;
    }
}
