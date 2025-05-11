package org.cneko.gal.common.client.parser;

import org.cneko.gal.common.client.GalSoundInstance;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class GalSoundPlayer {
    private final Path soundPath;
    private final GalSoundInstance instance = GalSoundInstance.getInstance();
    public GalSoundPlayer(Path soundPath) {
        this.soundPath = soundPath;
    }

    public void stopAll() {
        stopMusic();
        stopVoice();
    }

    public void stopMusic(){
        instance.stopMusic();
    }
    public void stopVoice(){
        instance.stopVoice();
    }
    public void playMusic(String name, boolean loop) {
        instance.playMusic(name, loop);
    }
    public void playVoice(String name) {
        instance.playVoice(name);
    }

    private @NotNull String getMusicPath(String name) {
        return soundPath.resolve("music").resolve(name + ".ogg").toString();
    }

    private @NotNull String getVoicePath(String name) {
        return soundPath.resolve("voice").resolve(name + ".ogg").toString();
    }
}