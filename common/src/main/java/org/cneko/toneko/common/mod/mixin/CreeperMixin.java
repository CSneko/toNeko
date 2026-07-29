package org.cneko.toneko.common.mod.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import org.cneko.toneko.common.mod.entities.INeko;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 让苦力怕遇到猫娘时不会爆炸。
 * 在 tick 末尾（SwellGoal 之后）检测周围是否有猫娘，有则取消膨胀。
 * 苦力怕不会逃开猫娘（不像原版遇到豹猫/猫那样逃跑），只是不会爆炸。
 */
@Mixin(Creeper.class)
public abstract class CreeperMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickTail(CallbackInfo ci) {
        Creeper self = (Creeper) (Object) this;
        if (self.level().isClientSide) return;
        if (self.getSwellDir() <= 0) return;

        // 在苦力怕爆炸检测范围内（7格）查找猫娘
        var nekoes = self.level().getEntitiesOfClass(
                LivingEntity.class,
                self.getBoundingBox().inflate(7.0),
                e -> e instanceof INeko neko && neko.isNeko() && e.isAlive()
        );
        if (!nekoes.isEmpty()) {
            self.setSwellDir(-1);
        }
    }
}
