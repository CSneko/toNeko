package org.cneko.toneko.common.mod.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.cneko.toneko.common.mod.api.events.WorldEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.x：天气参数从 ServerLevel 移到了 MinecraftServer（全局 WeatherData），
 * 因此改挂 MinecraftServer#setWeatherParameters，并对每个世界分发一次事件。
 */
@Mixin(MinecraftServer.class)
public class ServerLevelMixin {
    @Inject(at = @At("TAIL"), method = "setWeatherParameters")
    public void setWeatherParameters(int clearTime, int weatherTime, boolean isRaining, boolean isThundering, CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        for (ServerLevel level : server.getAllLevels()) {
            WorldEvents.ON_WEATHER_CHANGE.invoker().onWeatherChange(level, clearTime, weatherTime, isRaining, isThundering);
        }
    }
}
