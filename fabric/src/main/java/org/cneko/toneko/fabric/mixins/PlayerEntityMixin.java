package org.cneko.toneko.fabric.mixins;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.fabric.api.events.AttributeEvents;
import org.cneko.toneko.common.mod.misc.ToNekoAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerEntityMixin {
    @Inject(at = @At("RETURN"), method = "createAttributes", cancellable = true)
    private static void createAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        AttributeSupplier.Builder builder = cir.getReturnValue();
        builder = builder.add(ToNekoAttributes.NEKO_DEGREE);
        builder = AttributeEvents.ON_REGISTER_PLAYER_ATTRIBUTES.invoker().onRegister(builder);
        cir.setReturnValue(builder);
    }
}
