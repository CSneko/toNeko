package org.cneko.toneko.common.mod.client.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class Music {
    public static final List<int[]> MUSICS = List.of(
            // 《小星星》
            new int[]{
                    1,1,5,5,6,6,5, 4,4,3,3,2,2,1,
                    5,5,4,4,3,3,2, 5,5,4,4,3,3,2,
                    1,1,5,5,6,6,5, 4,4,3,3,2,2,1
            }
    );

    private static final Map<Integer, Integer> NOTE_MAP = Map.ofEntries(
            Map.entry(0, 0),  // 休止符
            Map.entry(1, 12), // C3
            Map.entry(2, 14), // D3
            Map.entry(3, 16), // E3
            Map.entry(4, 17), // F3
            Map.entry(5, 19), // G3
            Map.entry(6, 21), // A3
            Map.entry(7, 23), // B3
            Map.entry(8, 24), // C4
            Map.entry(9, 12), // C3（低八度重复）
            Map.entry(10,14)  // D3（低八度重复）
    );
    private static final NoteBlockInstrument[] INSTRUMENTS = {NoteBlockInstrument.HARP};
    private static final long NOTE_DURATION = 100L;

    private final int[] currentMusic;
    private int currentIndex;
    private long lastNoteTime;

    public Music() {
        Random random = new Random();
        this.currentMusic = MUSICS.get(random.nextInt(MUSICS.size()));
        this.currentIndex = 0;
        this.lastNoteTime = 0;
    }

    public void tryPlayNextNote(NotePlayer player) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastNoteTime < NOTE_DURATION) {
            return;
        }

        if (this.currentIndex >= this.currentMusic.length) {
            this.currentIndex = 0; // 循环播放
        }

        int noteValue = this.currentMusic[this.currentIndex];
        if (noteValue != 0) {
            int midiNote = NOTE_MAP.getOrDefault(noteValue, 0);
            float pitch = (float) Math.pow(2.0, (midiNote - 12) / 12.0);
            NoteBlockInstrument instrument = INSTRUMENTS[this.currentIndex % INSTRUMENTS.length];
            player.playNote(instrument, pitch, 1.0f);
        }

        this.currentIndex = (this.currentIndex + 1) % this.currentMusic.length;
        this.lastNoteTime = currentTime;
    }

    public interface NotePlayer {
        void playNote(NoteBlockInstrument instrument, float pitch, float volume);
    }
}