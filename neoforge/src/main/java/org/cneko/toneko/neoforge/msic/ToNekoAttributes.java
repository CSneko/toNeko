package org.cneko.toneko.neoforge.msic;

import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

import static org.cneko.toneko.common.mod.misc.ToNekoAttributes.NEKO_DEGREE;


public class ToNekoAttributes {

    public static void init(){
    }


    @SubscribeEvent
    public static void onRegisterAttributes(EntityAttributeModificationEvent event){
        event.add(EntityType.PLAYER,NEKO_DEGREE);
    }


}
