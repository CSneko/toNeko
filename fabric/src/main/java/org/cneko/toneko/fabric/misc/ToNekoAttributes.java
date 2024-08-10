package org.cneko.toneko.fabric.misc;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.player.Player;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoAttributes {
    public static final ResourceLocation NEKO_DEGREE_ID = ResourceLocation.fromNamespaceAndPath(MODID, "neko.degree");
    public static final Holder<Attribute> NEKO_DEGREE = register(NEKO_DEGREE_ID,
        new RangedAttribute("attribute.name.neko.degree",
        1.0, 0.0, 100.0
        ).setSyncable(true)
    );

    public static void init(){
        FabricDefaultAttributeRegistry.register(EntityType.PLAYER,Player.createAttributes().add(NEKO_DEGREE));
    }

    public static Holder<Attribute> register(ResourceLocation id, Attribute attribute) {
        return Registry.registerForHolder(BuiltInRegistries.ATTRIBUTE,id, attribute);
    }

}
