package org.cneko.toneko.neoforge.msic;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

import static org.cneko.toneko.neoforge.ToNekoNeoForge.ATTRIBUTES;

public class ToNekoAttributes {
    public static final Holder<Attribute> NEKO_DEGREE = ATTRIBUTES.register("neko.degree",
            ()->new RangedAttribute("attribute.name.neko.degree",
                    1.0, 0.0, 100.0
            ).setSyncable(true));;

    public static void init(){
    }

    @SubscribeEvent
    public static void onRegisterAttributes(EntityAttributeCreationEvent event){
        // TODO: 解决这个错误
        event.put(EntityType.PLAYER,Player.createAttributes().add(NEKO_DEGREE.getDelegate()).build());
    }


}
