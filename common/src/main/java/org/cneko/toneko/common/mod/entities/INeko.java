package org.cneko.toneko.common.mod.entities;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.api.NekoQuery;

public interface INeko {

    default NekoQuery.Neko getNeko(){
        Entity entity = this.getEntity();
        if (entity.isAlive()){
            return NekoQuery.getNeko(this.getEntity().getUUID());
        }else {
            return NekoQuery.NekoData.getNeko(NekoQuery.NekoData.EMPTY_UUID);
        }
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

    default int getNekoAbility(){
        return (int)((this.getNeko().getLevel()+1));
    }
}
