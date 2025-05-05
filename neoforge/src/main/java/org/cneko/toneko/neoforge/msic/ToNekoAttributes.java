package org.cneko.toneko.neoforge.msic;

import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import org.cneko.toneko.common.mod.entities.AdventurerNeko;
import org.cneko.toneko.common.mod.entities.CrystalNekoEntity;
import org.cneko.toneko.common.mod.entities.GhostNekoEntity;
import org.cneko.toneko.neoforge.entities.ToNekoEntities;

import static org.cneko.toneko.common.mod.misc.ToNekoAttributes.MAX_NEKO_ENERGY;
import static org.cneko.toneko.common.mod.misc.ToNekoAttributes.NEKO_DEGREE;


public class ToNekoAttributes {

    public static void init(){
        org.cneko.toneko.common.mod.misc.ToNekoAttributes.init();
    }


    @SubscribeEvent
    public static void onRegisterAttributes(EntityAttributeModificationEvent event){
        event.add(EntityType.PLAYER,NEKO_DEGREE);
        event.add(EntityType.PLAYER,MAX_NEKO_ENERGY);
    }
    public static void registerAttributes(EntityAttributeCreationEvent event){
        event.put(ToNekoEntities.ADVENTURER_NEKO_HOLDER.get(), AdventurerNeko.createAdventurerNekoAttributes().build());
        event.put(ToNekoEntities.CRYSTAL_NEKO_HOLDER.get(), CrystalNekoEntity.createNekoAttributes().build());
        event.put(ToNekoEntities.GHOST_NEKO_HOLDER.get(), GhostNekoEntity.createGhostNekoAttributes().build());
    }


}
