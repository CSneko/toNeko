package org.cneko.toneko.common.mod.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.entities.ai.goal.ZombieMushroomTargetGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Zombie.class)
public abstract class ZombieMushroomTargetMixin extends Monster {
    protected ZombieMushroomTargetMixin(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void toneko$preferMushroomGirls(CallbackInfo ci) {
        Zombie zombie = (Zombie) (Object) this;
        if (zombie instanceof ZombifiedPiglin) return;
        // Above the player's priority 2, alongside retaliation at priority 1.
        targetSelector.addGoal(1, new ZombieMushroomTargetGoal(zombie));
    }
}
