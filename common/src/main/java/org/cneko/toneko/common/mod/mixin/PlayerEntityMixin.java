package org.cneko.toneko.common.mod.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.entities.Neko;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Player.class)
public class PlayerEntityMixin implements Neko {
    @Override
    public LivingEntity getEntity() {
        return (Player)(Object) this;
    }
}
