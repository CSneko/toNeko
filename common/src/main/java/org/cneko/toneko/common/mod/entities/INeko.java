package org.cneko.toneko.common.mod.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.mod.misc.ToNekoAttributes;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface INeko {

    default NekoQuery.Neko getNeko(){
        Entity entity = this.getEntity();
        if (!entity.level().isClientSide && entity.isAlive()){
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
        return (int)(this.getNeko().getLevel() + this.getEntity().getAttributeValue(ToNekoAttributes.NEKO_DEGREE));
    }

    default float getMaxNekoEnergy(){
        return (float) getEntity().getAttributeValue(ToNekoAttributes.MAX_NEKO_ENERGY);
    }
    default float getNekoEnergy(){
        return 0;
    }
    default void setNekoEnergy(float energy){
    }

    default void saveNekoNBTData(@NotNull CompoundTag nbt){
        nbt.putDouble("NekoEnergy", this.getNekoEnergy());
    }
    default void loadNekoNBTData(@NotNull CompoundTag nbt){
        if (nbt.contains("NekoEnergy")) {
            this.setNekoEnergy(nbt.getFloat("NekoEnergy"));
        }
    }

    default void serverNekoSlowTick(){
        // 如果是猫娘
        if (this.isNeko()){
            increaseEnergy();
        }
    }

    default void increaseEnergy(){
        // 如果满了，则忽略
        float max = this.getMaxNekoEnergy();
        float energy = this.getNekoEnergy();
        if (energy >= max){
            this.setNekoEnergy(max);
            return;
        }
        // 根据自身猫猫等级来增加
        float increase = (float) (this.getNekoAbility() * 0.005);
        // 根据周围猫猫数量来增加
        List<INeko> nekos = EntityUtil.getNekoInRange(this.getEntity(), this.getEntity().level(), 3);
        for (INeko neko : nekos){
            if (neko.isNeko()){
                increase += (float) (neko.getNekoAbility() * 0.5);
            }
        }
        this.setNekoEnergy(energy + increase);
        if (this.getNekoEnergy() >= max){
            this.setNekoEnergy(max);
        }
    }
}
