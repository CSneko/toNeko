package org.cneko.toneko.fabric.api.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

public class AttributeEvents {
    /**
     * 当注册玩家属性时触发<br>
     * args:<br>
     * - AttributeSupplier.Builder 属性构建器<br>
     * return:<br>
     * AttributeSupplier.Builder 属性构建器
     */
    public static Event<OnRegisterPlayerAttributes> ON_REGISTER_PLAYER_ATTRIBUTES = EventFactory.createArrayBacked(OnRegisterPlayerAttributes.class,
            (listeners) -> (builder) -> {
            for (OnRegisterPlayerAttributes listener : listeners){
                builder = listener.onRegister(builder);
            }
            return builder;
    });

    public interface OnRegisterPlayerAttributes {
        AttributeSupplier.Builder onRegister(AttributeSupplier.Builder builder);
    }
}
