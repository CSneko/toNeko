package org.cneko.toneko.common.mod.client.music;

import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import org.cneko.toneko.common.util.ConfigUtil;

import java.util.Map;
import java.util.Random;

import static org.cneko.toneko.common.mod.client.music.ToNekoMusic.MUSICS;
import static org.cneko.toneko.common.mod.client.music.ToNekoMusic.NOTE_MAP;

public class ClientMusicPlayer {


    private static final Map<NoteBlockInstrument, Integer> INSTRUMENT_BASE_NOTE = Map.of(
            NoteBlockInstrument.HARP, 60
    );

    private MusicRecord currentMusic;
    private int currentIndex;
    private long lastNoteTime;

    public ClientMusicPlayer() {
        randomSwitchMusic();
    }

    public void randomSwitchMusic() {
        if (ConfigUtil.IS_FOOL_DAY){
            // 愚人节播放Never Gonna Give You Up
            this.currentMusic = ToNekoMusic.NEVER_GONNA_GIVE_YOU_UP;
        }
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
    public void restart() {
        this.currentIndex = 0;
        this.lastNoteTime = 0;
    }

    public interface NotePlayer {
        void playNote(NoteBlockInstrument instrument, float pitch, float volume);
    }

}
