package org.cneko.toneko.fabric.misc;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoAttributes {
    public static final Identifier NEKO_DEGREE_ID = Identifier.of(MODID, "neko.degree");
    public static final RegistryEntry<EntityAttribute> NEKO_DEGREE = register(NEKO_DEGREE_ID,
        new ClampedEntityAttribute("attribute.name.neko.degree",
        1.0, 0.0, 100.0
        ).setTracked(true)
    );

    public static void init(){
        FabricDefaultAttributeRegistry.register(EntityType.PLAYER,PlayerEntity.createPlayerAttributes().add(NEKO_DEGREE));
    }

    public static RegistryEntry<EntityAttribute> register(Identifier id, EntityAttribute attribute) {
        return Registry.registerReference(Registries.ATTRIBUTE,id, attribute);
    }

}
