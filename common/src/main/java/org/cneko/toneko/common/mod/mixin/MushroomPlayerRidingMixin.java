package org.cneko.toneko.common.mod.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.entities.MushroomGirlEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Players are not world-saveable vehicles in 26.1, but companionship is a transient passenger link. */
@Mixin(Entity.class)
public abstract class MushroomPlayerRidingMixin {
    @WrapOperation(method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;canSerialize()Z"))
    private boolean toneko$allowCompanionVehicle(EntityType<?> type, Operation<Boolean> original, Entity vehicle, boolean force, boolean emitGameEvent) {
        return original.call(type) || ((Object)this instanceof MushroomGirlEntity mushroom
                && vehicle instanceof Player && mushroom.getCompanionPose() != MushroomGirlEntity.NONE);
    }
}
