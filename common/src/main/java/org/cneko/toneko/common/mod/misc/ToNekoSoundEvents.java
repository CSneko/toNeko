package org.cneko.toneko.common.mod.misc;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoSoundEvents {
    public static final SoundEvent BAZOOKA_BIU = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, "item.bazooka.biu"));
    public static final SoundEvent BAZOOKA_MEOW = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, "item.bazooka.meow"));

}
