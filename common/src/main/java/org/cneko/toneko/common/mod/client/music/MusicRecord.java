package org.cneko.toneko.common.mod.client.music;

import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

public record MusicRecord(int[] music, NoteBlockInstrument instrument, int noteDuration) {
}
