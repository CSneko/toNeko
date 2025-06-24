package org.cneko.toneko.common.mod.misc;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoAttributes {
    public static final ResourceLocation NEKO_DEGREE_ID = ResourceLocation.fromNamespaceAndPath(MODID, "neko.degree");
    public static final @NotNull Holder<Attribute> NEKO_DEGREE = register(NEKO_DEGREE_ID,
        new RangedAttribute("attribute.name.neko.degree",
        1.0, 1.0, 100.0
        ).setSyncable(true)
    );
    public static final ResourceLocation MAX_NEKO_ENERGY_ID = ResourceLocation.fromNamespaceAndPath(MODID, "neko.max_energy");
    public static final @NotNull Holder<Attribute> MAX_NEKO_ENERGY = register(MAX_NEKO_ENERGY_ID,
        new RangedAttribute("attribute.name.neko.max_energy",
        1000.0, 0.0, 100000.0
        ).setSyncable(true)
    );

    @ExpectPlatform
    public static @NotNull Holder<Attribute> register(ResourceLocation id, Attribute attribute) {
        throw new AssertionError();
    }

    public static void init() {
    }
}
