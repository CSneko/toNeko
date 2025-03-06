package org.cneko.toneko.common.mod.client;

import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class ClientMusicPlayer {
    public static final List<Music> MUSICS = List.of(
            // 《小星星》
            new Music(
                    new int[]{
                            1,1,5,5,6,6,5, 4,4,3,3,2,2,1,
                            5,5,4,4,3,3,2, 5,5,4,4,3,3,2,
                            1,1,5,5,6,6,5, 4,4,3,3,2,2,1
                    },
                    NoteBlockInstrument.HARP,
                    500
            ),
            // 《虫儿飞》
            new Music(
                    new int[]{
                    },
                    NoteBlockInstrument.HARP,
                    500
            )
    );

    private static final Map<Integer, Integer> NOTE_MAP = Map.ofEntries(
            Map.entry(0, 0),  // 休止符
            Map.entry(1, 60), // C
            Map.entry(2, 62), // D
            Map.entry(3, 64), // E
            Map.entry(4, 65), // F
            Map.entry(5, 67), // G
            Map.entry(6, 69),  // A
            Map.entry(7, 71) // B
    );

    private static final Map<NoteBlockInstrument, Integer> INSTRUMENT_BASE_NOTE = Map.of(
            NoteBlockInstrument.HARP, 60
    );

    private Music currentMusic;
    private int currentIndex;
    private long lastNoteTime;

    public ClientMusicPlayer() {
        randomSwitchMusic();
    }

    public void randomSwitchMusic() {
        Random random = new Random();
        this.currentMusic = MUSICS.get(random.nextInt(MUSICS.size()));
        this.currentIndex = 0;
        this.lastNoteTime = 0;
    }

    public void tryPlayNextNote(NotePlayer player) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastNoteTime < currentMusic.noteDuration() / 2) {
            return;
        }

        if (this.currentIndex >= this.currentMusic.music().length) {
            this.currentIndex = 0; // 循环播放
        }

        int noteValue = this.currentMusic.music()[this.currentIndex];
        if (noteValue != 0) {
            int midiNote = NOTE_MAP.getOrDefault(noteValue, 0);
            int baseMidiNote = INSTRUMENT_BASE_NOTE.getOrDefault(this.currentMusic.instrument(), 60);
            // 计算倍频因子：当 midiNote == baseMidiNote 时，pitch 为 1.0
            float pitch = (float) Math.pow(2.0, (midiNote - baseMidiNote) / 12.0);
            NoteBlockInstrument instrument = currentMusic.instrument();
            player.playNote(instrument, pitch, 1.0f);
        }

        this.currentIndex = (this.currentIndex + 1) % this.currentMusic.music().length;
        this.lastNoteTime = currentTime;
    }

    public interface NotePlayer {
        void playNote(NoteBlockInstrument instrument, float pitch, float volume);
    }

    public record Music(int[] music, NoteBlockInstrument instrument, int noteDuration) {}
}
