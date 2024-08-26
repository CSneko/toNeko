package org.cneko.toneko.common.mod.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.entities.Neko;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import software.bernie.geckolib.animatable.GeoEntity;

@Mixin(Player.class)
public abstract class PlayerEntityMixin implements Neko {
    @Override
    public LivingEntity getEntity() {
        return (Player)(Object) this;
    }

    @Override
    public boolean isPlayer() {
        return true;
    }
}
