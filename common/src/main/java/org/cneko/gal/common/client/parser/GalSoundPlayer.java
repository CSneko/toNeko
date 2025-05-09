package org.cneko.gal.common.client.parser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import org.cneko.gal.common.util.FileUtil;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GalSoundPlayer {
    private final Path soundPath;
    private final ExecutorService musicExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService voiceExecutor = Executors.newSingleThreadExecutor();

    private net.minecraft.client.resources.sounds.SoundInstance currentMusic;
    private net.minecraft.client.resources.sounds.SoundInstance currentVoice;

    public GalSoundPlayer(Path soundPath) {
        this.soundPath = soundPath;

    }


    public void playMusic(String name) {
        musicExecutor.submit(() -> {
            // Stop current music if playing
            stopMusic();

            InputStream musicStream = readMusic(name);
            if (musicStream != null) {
//                currentMusic = SimpleSoundInstance.forMusic(
//                        soundPath.resolve("music").resolve(name + ".ogg").toUri()
//                );

                float volume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MUSIC);
                Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(
                        currentMusic.getLocation(),
                        SoundSource.MUSIC,
                        volume,
                        1.0f,
                        SoundInstance.createUnseededRandom(),
                        false,
                        0,
                        SoundInstance.Attenuation.NONE,
                        0.0, 0.0, 0.0,
                        true
                ));
            }
        });
    }



    public void playVoice(String name) {
        voiceExecutor.submit(() -> {
            stopVoice();

            InputStream voiceStream = readVoice(name);
            if (voiceStream == null) {
                stopMusic();
                return;
            }

//            currentVoice = SimpleSoundInstance.forMusic(
//                    soundPath.resolve("voice").resolve(name + ".ogg").toUri()
//            );

            float volume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.RECORDS);
            Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(
                    currentVoice.getLocation(),
                    SoundSource.RECORDS,
                    volume,
                    1.0f,
                    SoundInstance.createUnseededRandom(),
                    false,
                    0,
                    SoundInstance.Attenuation.NONE,
                    0.0, 0.0, 0.0,
                    true
            ));
        });
    }

    public void stopMusic() {
        if (currentMusic != null && Minecraft.getInstance().getSoundManager().isActive(currentMusic)) {
            Minecraft.getInstance().getSoundManager().stop(currentMusic);
            currentMusic = null;
        }
    }

    public void stopVoice() {
        if (currentVoice != null && Minecraft.getInstance().getSoundManager().isActive(currentVoice)) {
            Minecraft.getInstance().getSoundManager().stop(currentVoice);
            currentVoice = null;
        }
    }

    public void stopAll() {
        stopMusic();
        stopVoice();
    }

    @Nullable
    private InputStream readMusic(String name) {
        return FileUtil.readFile(soundPath.resolve("music").resolve(name + ".ogg").toFile());
    }

    @Nullable
    private InputStream readVoice(String name) {
        return FileUtil.readFile(soundPath.resolve("voice").resolve(name + ".ogg").toFile());
    }
}