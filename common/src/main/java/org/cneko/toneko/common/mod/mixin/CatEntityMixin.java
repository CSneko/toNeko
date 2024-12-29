package org.cneko.toneko.common.mod.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.mod.entities.INeko;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

@Mixin(Cat.class)
public class CatEntityMixin implements INeko {
    @Unique
    private static final UUID CAT_UUID = UUID.fromString("4cb581d0-c5f4-11ef-a9b1-6fa16c714ada");
    @Override
    public LivingEntity getEntity() {
        return (LivingEntity) (Object)this;
    }

    @Override
    public boolean allowMateIfNotNeko() {
        return true;
    }

    @Override
    public boolean isNeko() {
        return true;
    }

    @Override
    public int getNekoAbility() {
        return 1;
    }

    @Override
    public NekoQuery.Neko getNeko() {
        return NekoQuery.getNeko(CAT_UUID);
    }
}
