package org.cneko.toneko.common.mod.misc.neoforge;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;

import static org.cneko.toneko.neoforge.ToNekoNeoForge.ATTRIBUTES;

public class ToNekoAttributesImpl {
    public static Holder<Attribute> register(Identifier id, Attribute attribute) {
        return ATTRIBUTES.register(id.getPath(),()->attribute);
    }
}
