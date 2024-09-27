package org.cneko.toneko.common.mod.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import org.cneko.toneko.common.mod.entities.INeko;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Cat.class)
public class CatEntityMixin implements INeko {
    @Override
    public LivingEntity getEntity() {
        return (LivingEntity) (Object)this;
    }

    @Override
    public boolean allowMateIfNotNeko() {
        return true;
    }
}
