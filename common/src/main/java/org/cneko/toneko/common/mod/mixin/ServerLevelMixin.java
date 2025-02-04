package org.cneko.toneko.common.mod.mixin;

import net.minecraft.server.level.ServerLevel;
import org.cneko.toneko.common.mod.api.events.WorldEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @Inject(at = @At("TAIL"), method = "setWeatherParameters")
    public void setWeatherParameters(int clearTime, int weatherTime, boolean isRaining, boolean isThundering, CallbackInfo ci) {
        WorldEvents.ON_WEATHER_CHANGE.invoker().onWeatherChange((ServerLevel)(Object)this,clearTime, weatherTime, isRaining, isThundering);
    }
}
