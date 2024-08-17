package org.cneko.toneko.neoforge.msic;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

import static org.cneko.toneko.neoforge.ToNekoNeoForge.ATTRIBUTES;

public class ToNekoAttributes {
    public static final Holder<Attribute> NEKO_DEGREE = ATTRIBUTES.register("neko.degree",
            ()->new RangedAttribute("attribute.name.neko.degree",
                    1.0, 0.0, 100.0
            ).setSyncable(true));

    public static void init(){
    }

    @SubscribeEvent
    public static void onRegisterAttributes(EntityAttributeModificationEvent event){
        event.add(EntityType.PLAYER,NEKO_DEGREE);
    }


}
