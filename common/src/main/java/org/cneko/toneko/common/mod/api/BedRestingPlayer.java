package org.cneko.toneko.common.mod.api;

import net.minecraft.core.BlockPos;
import java.util.Optional;

/** A synchronized, awake rest on a bed; separate from vanilla's sleeping position. */
public interface BedRestingPlayer {
    Optional<BlockPos> toneko$getRestingBed();
    void toneko$setRestingBed(Optional<BlockPos> bed);
}
