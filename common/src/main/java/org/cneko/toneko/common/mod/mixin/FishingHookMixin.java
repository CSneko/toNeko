package org.cneko.toneko.common.mod.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.api.NekoLevelRegistry;
import org.cneko.toneko.common.mod.entities.INeko;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingHook.class)
public class FishingHookMixin {
    @Inject(method = "retrieve", at = @At("RETURN"))
    private void toneko$onRetrieve(ItemStack rod, CallbackInfoReturnable<Integer> cir) {
        FishingHook self = (FishingHook) (Object) this;
        Player player = self.getPlayerOwner();
        if (player instanceof INeko neko && neko.isNeko()) {
            int vanillaXp = cir.getReturnValue();
            double fishingXp = vanillaXp * 20.0;
            NekoLevelRegistry.fishing().addRaw(neko, fishingXp);
        }
    }
}
