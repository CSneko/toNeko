package org.cneko.toneko.common.mod.entities;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.api.NekoQuery;

public interface INeko {

    default NekoQuery.Neko getNeko(){
        return NekoQuery.getNeko(this.getEntity().getUUID());
    }
    default LivingEntity getEntity(){
        throw new RuntimeException("You should implements in your entity");
    }
    default boolean isPlayer(){
        return this.getEntity() instanceof Player;
    }

    default boolean allowMateIfNotNeko(){
        return false;
    }

    default boolean isNeko(){
        return this.getNeko().isNeko();
    }
}
