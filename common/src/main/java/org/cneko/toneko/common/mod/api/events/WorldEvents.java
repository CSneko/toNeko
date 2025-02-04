package org.cneko.toneko.common.mod.api.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;

public class WorldEvents {
    public static Event<OnWeatherChange> ON_WEATHER_CHANGE = EventFactory.createArrayBacked(OnWeatherChange.class,
            (listeners) -> (world,clearTime, weatherTime, isRaining, isThundering) -> {
        for (OnWeatherChange listener : listeners) {
            listener.onWeatherChange(world,clearTime, weatherTime, isRaining, isRaining);
        }
    });

    public interface OnWeatherChange {
        void onWeatherChange(ServerLevel world, int clearTime, int weatherTime, boolean isRaining, boolean isThundering);
    }
}
