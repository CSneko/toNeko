package org.cneko.toneko.common.mod.misc;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoSoundEvents {
    public static final SoundEvent BAZOOKA_BIU = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MODID, "item.bazooka.biu"));
    public static final SoundEvent BAZOOKA_MEOW = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MODID, "item.bazooka.meow"));
    public static final SoundEvent NEKO_ALARM = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MODID, "entity.neko.alarm"));
    public static final SoundEvent LEGWEAR_RUSTLE = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MODID, "item.legwear.rustle"));
}
