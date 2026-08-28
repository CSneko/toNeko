package org.cneko.toneko.common.mod.misc.fabric;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;

public class ToNekoAttributesImpl {
    public static Holder<Attribute> register(Identifier id, Attribute attribute) {
        return Registry.registerForHolder(BuiltInRegistries.ATTRIBUTE,id, attribute);
    }
}
