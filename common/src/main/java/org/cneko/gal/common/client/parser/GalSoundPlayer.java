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
        if (name == null || name.isEmpty()) return;
        instance.playMusic(getMusicPath(name), loop);
    }
    public void playVoice(String name) {
        if (name == null || name.isEmpty()) return;
        instance.playVoice(getVoicePath(name));
    }

    private @NotNull String getMusicPath(String name) {
        return soundPath.resolve("music").resolve(name + ".mp3").toString();
    }

    private @NotNull String getVoicePath(String name) {
        return soundPath.resolve("voices").resolve(name + ".mp3").toString();
    }
}