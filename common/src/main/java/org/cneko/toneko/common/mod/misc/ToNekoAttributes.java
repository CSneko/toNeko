package org.cneko.toneko.common.mod.misc;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoAttributes {
    public static final Identifier NEKO_DEGREE_ID = Identifier.fromNamespaceAndPath(MODID, "neko.degree");
    public static final @NotNull Holder<Attribute> NEKO_DEGREE = register(NEKO_DEGREE_ID,
        new RangedAttribute("attribute.name.neko.degree",
        1.0, 0, 100.0
        ).setSyncable(true)
    );
    public static final Identifier MAX_NEKO_ENERGY_ID = Identifier.fromNamespaceAndPath(MODID, "neko.max_energy");
    public static final @NotNull Holder<Attribute> MAX_NEKO_ENERGY = register(MAX_NEKO_ENERGY_ID,
        new RangedAttribute("attribute.name.neko.max_energy",
        1000.0, 0.0, 100000.0
        ).setSyncable(true)
    );

    @ExpectPlatform
    public static @NotNull Holder<Attribute> register(Identifier id, Attribute attribute) {
        throw new AssertionError();
    }

    public static void init() {
    }
}
