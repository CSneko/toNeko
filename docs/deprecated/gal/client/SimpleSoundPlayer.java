package org.cneko.gal.common.client;

import org.lwjgl.openal.AL10;
import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;

import static org.lwjgl.openal.AL10.*;

public class SimpleSoundPlayer {
    private int buffer;
    private int source;

    public SimpleSoundPlayer(File wavFile) throws Exception {
// 加载音频文件
        AudioInputStream ais = AudioSystem.getAudioInputStream(wavFile);
        AudioFormat format = ais.getFormat();

        if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
            throw new IllegalArgumentException("需要16位PCM编码");
        }

        byte[] rawData = ais.readAllBytes();
        ByteBuffer data = BufferUtils.createByteBuffer(rawData.length).put(rawData);
        data.flip();

        int alFormat = getOpenALFormat(format.getChannels(),format.getSampleSizeInBits());

// 生成缓冲区和音源
        buffer = alGenBuffers();
        source = alGenSources();

        alBufferData(buffer, alFormat, data, (int) format.getSampleRate());
        alSourcei(source, AL_BUFFER, buffer);
    }

    public void play() {
        alSourcePlay(source);
    }

    public void stop() {
        alSourceStop(source);
    }

    public void cleanup() {
        alDeleteSources(source);
        alDeleteBuffers(buffer);
    }

    private int getOpenALFormat(int channels, int bits) {
        if (channels == 1) {
            return bits == 8 ? AL_FORMAT_MONO8 : AL_FORMAT_MONO16;
        } else {
            return bits == 8 ? AL_FORMAT_STEREO8 : AL_FORMAT_STEREO16;
        }
    }
}