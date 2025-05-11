package org.cneko.gal.common.client;

import org.lwjgl.openal.*;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
// import java.nio.charset.StandardCharsets; // Not used in WAV loader for critical parts
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.cneko.gal.common.Gal.LOGGER;

public class GalSoundInstance {

    private static GalSoundInstance instance;

    private long device;
    private long context;
    private boolean ownsOpenALContext = false; // Flag to track if this instance created the context

    private int musicSource = -1;
    private int voiceSource = -1;

    private int currentMusicBuffer = -1;
    private int currentVoiceBuffer = -1;

    private String currentMusicPath = null;
    private String currentVoicePath = null;

    private GalSoundInstance() {
    }

    public static synchronized GalSoundInstance getInstance() {
        if (instance == null) {
            instance = new GalSoundInstance();
            if (!instance.init()) {
                LOGGER.error("GalSoundInstance初始化失败。");
                // instance remains null if init fails, or we can explicitly null it:
                instance = null;
                return null;
            }
        }
        return instance;
    }

    public boolean init() {
        long currentContext = alcGetCurrentContext();
        if (currentContext != NULL) {
            LOGGER.warn("检测到已存在的OpenAL上下文，将尝试共享使用");
            context = currentContext;
            device = alcGetContextsDevice(context);
            if (device == NULL) {
                LOGGER.error("无法从现有上下文中获取OpenAL设备。");
                return false;
            }
            this.ownsOpenALContext = false; // We are sharing, so we don't own it
            AL.createCapabilities(ALC.createCapabilities(device)); // Create AL caps for current context
        } else {
            String defaultDeviceName = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
            if (defaultDeviceName == null) {
                LOGGER.error("无法获取默认的OpenAL设备名称。");
                return false;
            }
            device = alcOpenDevice(defaultDeviceName);
            if (device == NULL) {
                LOGGER.error("无法打开默认的OpenAL设备。");
                return false;
            }

            ALCCapabilities deviceCaps = ALC.createCapabilities(device);
            context = alcCreateContext(device, (IntBuffer) null);
            if (context == NULL) {
                LOGGER.error("无法创建OpenAL上下文。");
                alcCloseDevice(device);
                return false;
            }

            if (!alcMakeContextCurrent(context)) {
                LOGGER.error("无法将OpenAL上下文设为当前。");
                alcDestroyContext(context);
                alcCloseDevice(device);
                return false;
            }
            AL.createCapabilities(deviceCaps);
            this.ownsOpenALContext = true; // We created it, we own it
        }

        musicSource = alGenSources();
        voiceSource = alGenSources();

        if (checkALError("init - alGenSources")) {
            destroy(); // Clean up partially initialized state
            return false;
        }

        String actualDeviceName = alcGetString(device, ALC_DEVICE_SPECIFIER);
        LOGGER.info("GalSoundInstance初始化成功。设备: {} ({}), 上下文: {}, OwnsContext: {}",
                device, actualDeviceName != null ? actualDeviceName : "N/A", context, ownsOpenALContext);
        return true;
    }

    private SoundData loadSound(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            LOGGER.error("文件路径为空或无效。");
            return null;
        }

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            LOGGER.error("未找到声音文件: {}", filePath);
            return null;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer channelsBuffer = stack.mallocInt(1);
            IntBuffer sampleRateBuffer = stack.mallocInt(1);

            ShortBuffer rawAudioBuffer = STBVorbis.stb_vorbis_decode_filename(filePath,
                    channelsBuffer, sampleRateBuffer);

            if (rawAudioBuffer == null) {
                int stbError = STBVorbis.stb_vorbis_get_error(NULL); // Use NULL for global error after decode_filename
                LOGGER.warn("加载OGG/Vorbis失败: {} (STB Error Code: {}, Message: {}). 尝试作为WAV加载.",
                        filePath, stbError, getVorbisErrorString(stbError));
                return loadWavInternal(filePath);
            }

            int channels = channelsBuffer.get(0);
            int sampleRate = sampleRateBuffer.get(0);
            int format;
            if (channels == 1) {
                format = AL_FORMAT_MONO16;
            } else if (channels == 2) {
                format = AL_FORMAT_STEREO16;
            } else {
                LOGGER.error("不支持的声道数: {} 在 OGG/Vorbis 文件 {}", channels, filePath);
                MemoryUtil.memFree(rawAudioBuffer);
                return null;
            }
            // STBVorbis allocates rawAudioBuffer, so it's the one to be managed by SoundData
            return new SoundData(rawAudioBuffer, format, sampleRate);

        } catch (Exception e) {
            LOGGER.error("加载声音 {} (OGG尝试) 时出现意外错误: {}. 尝试作为WAV加载.", filePath, e.getMessage(), e);
            return loadWavInternal(filePath);
        }
    }

    private String getVorbisErrorString(int errorCode) {
        return switch (errorCode) {
            case STBVorbis.VORBIS__no_error -> "无错误 (VORBIS__no_error)";
            case STBVorbis.VORBIS_need_more_data -> "需要更多数据 (VORBIS_need_more_data)";
            case STBVorbis.VORBIS_invalid_api_mixing -> "无效的API混合 (VORBIS_invalid_api_mixing) - often if stb_vorbis_get_error(NULL) called inappropriately";
            case STBVorbis.VORBIS_outofmem -> "内存不足 (VORBIS_outofmem)";
            case STBVorbis.VORBIS_feature_not_supported -> "不支持的特性 (VORBIS_feature_not_supported)";
            case STBVorbis.VORBIS_too_many_channels -> "声道数过多 (VORBIS_too_many_channels)";
            case STBVorbis.VORBIS_file_open_failure -> "文件打开失败 (VORBIS_file_open_failure)";
            case STBVorbis.VORBIS_seek_without_length -> "无长度信息时尝试seek (VORBIS_seek_without_length)";
            case STBVorbis.VORBIS_unexpected_eof -> "意外EOF (VORBIS_unexpected_eof)";
            case STBVorbis.VORBIS_seek_invalid -> "无效seek (VORBIS_seek_invalid)";
            case STBVorbis.VORBIS_invalid_setup -> "无效的设置 (VORBIS_invalid_setup)";
            case STBVorbis.VORBIS_invalid_stream -> "无效的流 (VORBIS_invalid_stream)";
            case STBVorbis.VORBIS_missing_capture_pattern -> "解码内存时缺少捕获模式 (VORBIS_missing_capture_pattern - unlikely for decode_filename)";
            case STBVorbis.VORBIS_bad_packet_type -> "错误的包类型 (VORBIS_bad_packet_type)";
            case STBVorbis.VORBIS_continued_packet_flag_invalid -> "连续包标志无效 (VORBIS_continued_packet_flag_invalid)";
            case STBVorbis.VORBIS_ogg_skeleton_not_supported -> "不支持Ogg骨架 (VORBIS_ogg_skeleton_not_supported)";
            default -> "未知STB Vorbis错误 (" + errorCode + ")";
        };
    }

    private SoundData loadWavInternal(String filePath) {
        try (InputStream is = Files.newInputStream(Paths.get(filePath));
             ReadableByteChannel rbc = Channels.newChannel(is);
             MemoryStack stack = MemoryStack.stackPush()) {

            ByteBuffer header = stack.malloc(44);
            if (rbc.read(header) < 44) {
                LOGGER.error("WAV error: Premature EOF while reading header from {}", filePath);
                return null;
            }
            header.flip();

            if (header.getInt() != 0x52494646) { LOGGER.error("WAV error: Missing RIFF in {}", filePath); return null; }
            header.getInt(); // ChunkSize
            if (header.getInt() != 0x57415645) { LOGGER.error("WAV error: Missing WAVE in {}", filePath); return null; }

            int fmtChunkSize = 0;
            short audioFormat = 0;
            short numChannels = 0;
            int sampleRate = 0;
            short bitsPerSample = 0;
            boolean fmtFound = false;

            while(true) {
                ByteBuffer chunkHeader = stack.malloc(8);
                int bytesReadChunkHeader = rbc.read(chunkHeader);
                if (bytesReadChunkHeader == -1) { // Clean EOF
                    LOGGER.error("WAV error: Premature EOF before finding 'data' chunk in {}", filePath);
                    break;
                }
                if (bytesReadChunkHeader < 8) {
                    LOGGER.error("WAV error: Premature EOF while reading chunk header (read {} bytes) from {}", bytesReadChunkHeader, filePath);
                    return null;
                }
                chunkHeader.flip();
                int chunkId = chunkHeader.getInt();
                int chunkSize = chunkHeader.getInt();

                if (chunkId == 0x666d7420) { // "fmt "
                    fmtFound = true;
                    ByteBuffer fmtChunk = stack.malloc(chunkSize); // Max typical fmt size is small
                    if (rbc.read(fmtChunk) < chunkSize) {
                        LOGGER.error("WAV error: Premature EOF in fmt chunk from {}", filePath); return null;
                    }
                    fmtChunk.flip();
                    audioFormat = fmtChunk.getShort();
                    numChannels = fmtChunk.getShort();
                    sampleRate = fmtChunk.getInt();
                    fmtChunk.getInt(); // byteRate
                    fmtChunk.getShort(); // blockAlign
                    bitsPerSample = fmtChunk.getShort();
                    if (audioFormat != 1) {
                        LOGGER.error("WAV error: Not PCM format (format={}) in {}", audioFormat, filePath); return null;
                    }
                } else if (chunkId == 0x64617461) { // "data"
                    if (!fmtFound) {
                        LOGGER.error("WAV error: 'data' chunk found before 'fmt ' in {}", filePath); return null;
                    }

                    int alFormat = -1;
                    if (bitsPerSample == 16) {
                        if (numChannels == 1) alFormat = AL_FORMAT_MONO16;
                        else if (numChannels == 2) alFormat = AL_FORMAT_STEREO16;
                    } else if (bitsPerSample == 8) {
                        if (numChannels == 1) alFormat = AL_FORMAT_MONO8;
                        else if (numChannels == 2) alFormat = AL_FORMAT_STEREO8;
                    }

                    if (alFormat == -1) {
                        LOGGER.error("Unsupported WAV format: {} channels, {} bps in {}", numChannels, bitsPerSample, filePath);
                        return null;
                    }

                    ByteBuffer dataBuffer = MemoryUtil.memAlloc(chunkSize);
                    try {
                        int totalRead = 0;
                        ByteBuffer tempReadBuffer = stack.malloc(Math.min(chunkSize, 8192));
                        while(totalRead < chunkSize) {
                            tempReadBuffer.clear();
                            tempReadBuffer.limit(Math.min(tempReadBuffer.capacity(), chunkSize - totalRead));
                            int bytesRead = rbc.read(tempReadBuffer);
                            if (bytesRead == -1) break;
                            tempReadBuffer.flip();
                            dataBuffer.put(tempReadBuffer);
                            totalRead += bytesRead;
                        }

                        if (totalRead != chunkSize) {
                            LOGGER.error("WAV error: Read {} bytes for data chunk, expected {} in {}", totalRead, chunkSize, filePath);
                            MemoryUtil.memFree(dataBuffer); // Free if not returning it in SoundData
                            return null;
                        }
                        dataBuffer.flip();

                        // dataBuffer is allocated by MemoryUtil.memAlloc and needs to be managed by SoundData
                        if (bitsPerSample == 16) {
                            return new SoundData(dataBuffer, alFormat, sampleRate, true); // WAV 16-bit constructor
                        } else { // 8-bit
                            return new SoundData(dataBuffer, alFormat, sampleRate);    // WAV 8-bit constructor
                        }
                    } catch (Throwable t) {
                        MemoryUtil.memFree(dataBuffer); // Ensure buffer is freed on any error during filling/processing
                        throw t;
                    }
                } else { // Skip unknown chunk
                    if (chunkSize < 0) { // Should not happen with valid WAV
                        LOGGER.error("WAV error: Invalid chunk size {} for chunk 0x{} in {}", chunkSize, Integer.toHexString(chunkId), filePath);
                        return null;
                    }
                    long skipped = is.skip(chunkSize);
                    if (skipped != chunkSize) {
                        LOGGER.error("WAV error: Failed to skip chunk 0x{} of size {} (skipped {}). File: {}", Integer.toHexString(chunkId), chunkSize, skipped, filePath);
                        return null;
                    }
                }
            }
            LOGGER.error("WAV error: 'data' chunk not found or premature EOF in {}", filePath);
            return null;

        } catch (IOException e) {
            LOGGER.error("加载WAV {} 时发生IO异常: {}", filePath, e.getMessage(), e);
            return null;
        }
    }


    private int createBuffer(SoundData soundData) {
        if (soundData == null) return -1;

        int buffer = alGenBuffers();
        if (checkALError("alGenBuffers")) {
            soundData.dispose();
            return -1;
        }

        try {
            if (soundData.pcmDataForALShort != null) {
                alBufferData(buffer, soundData.format, soundData.pcmDataForALShort, soundData.sampleRate);
            } else if (soundData.pcmDataForALByte != null) {
                alBufferData(buffer, soundData.format, soundData.pcmDataForALByte, soundData.sampleRate);
            } else {
                LOGGER.error("SoundData没有有效的音频数据供OpenAL使用。");
                alDeleteBuffers(buffer);
                return -1;
            }

            if (checkALError("alBufferData")) {
                alDeleteBuffers(buffer);
                return -1;
            }
            return buffer;
        } finally {
            soundData.dispose(); // Data is now in OpenAL buffer (or failed), release original
        }
    }


    public synchronized void playMusic(String filePath, boolean loop) {
        if (musicSource == -1) {
            LOGGER.error("音乐源未初始化。");
            return;
        }
        stopMusicInternal(); // Stops and cleans up previous music/buffer

        SoundData soundData = loadSound(filePath);
        if (soundData == null) {
            // currentMusicPath is already null from stopMusicInternal or stays null
            LOGGER.error("无法加载音乐文件: {}", filePath);
            return;
        }

        currentMusicBuffer = createBuffer(soundData); // soundData is disposed inside createBuffer
        if (currentMusicBuffer == -1) {
            // currentMusicPath is already null
            LOGGER.error("无法为音乐创建OpenAL缓冲区: {}", filePath);
            return;
        }

        currentMusicPath = filePath; // Set path only on success

        alSourcei(musicSource, AL_BUFFER, currentMusicBuffer);
        alSourcei(musicSource, AL_LOOPING, loop ? AL_TRUE : AL_FALSE);
        alSourcePlay(musicSource);
        if (!checkALError("playMusic - " + filePath)) {
            LOGGER.info("播放音乐: {}{}", filePath, loop ? " (循环)" : "");
        }
    }

    public synchronized void playVoice(String filePath) {
        if (voiceSource == -1) {
            LOGGER.error("语音源未初始化。");
            return;
        }
        stopVoiceInternal();

        SoundData soundData = loadSound(filePath);
        if (soundData == null) {
            LOGGER.error("无法加载语音文件: {}", filePath);
            return;
        }

        currentVoiceBuffer = createBuffer(soundData);
        if (currentVoiceBuffer == -1) {
            LOGGER.error("无法为语音创建OpenAL缓冲区: {}", filePath);
            return;
        }
        currentVoicePath = filePath;

        alSourcei(voiceSource, AL_BUFFER, currentVoiceBuffer);
        alSourcei(voiceSource, AL_LOOPING, AL_FALSE); // Voices typically don't loop by default
        alSourcePlay(voiceSource);
        if (!checkALError("playVoice - " + filePath)) {
            LOGGER.info("播放语音: {}", filePath);
        }
    }


    private void stopMusicInternal() {
        if (musicSource != -1 && alGetSourcei(musicSource, AL_SOURCE_STATE) == AL_PLAYING) {
            alSourceStop(musicSource);
            checkALError("stopMusicInternal - stop");
        }
        if (musicSource != -1 && alGetSourcei(musicSource, AL_BUFFER) != 0) { // Check if buffer is attached
            alSourcei(musicSource, AL_BUFFER, 0); // Detach buffer
            checkALError("stopMusicInternal - detach buffer from source " + musicSource);
        }
        if (currentMusicBuffer != -1) {
            alDeleteBuffers(currentMusicBuffer);
            checkALError("stopMusicInternal - delete buffer " + currentMusicBuffer);
            currentMusicBuffer = -1;
        }
        currentMusicPath = null;
    }

    public synchronized void stopMusic() {
        if (currentMusicPath != null || isMusicPlaying()) {
            LOGGER.info("显式停止音乐。");
        }
        stopMusicInternal();
    }

    private void stopVoiceInternal() {
        if (voiceSource != -1 && alGetSourcei(voiceSource, AL_SOURCE_STATE) == AL_PLAYING) {
            alSourceStop(voiceSource);
            checkALError("stopVoiceInternal - stop");
        }
        if (voiceSource != -1 && alGetSourcei(voiceSource, AL_BUFFER) != 0) {
            alSourcei(voiceSource, AL_BUFFER, 0);
            checkALError("stopVoiceInternal - detach buffer from source " + voiceSource);
        }
        if (currentVoiceBuffer != -1) {
            alDeleteBuffers(currentVoiceBuffer);
            checkALError("stopVoiceInternal - delete buffer " + currentVoiceBuffer);
            currentVoiceBuffer = -1;
        }
        currentVoicePath = null;
    }

    public synchronized void stopVoice() {
        if (currentVoicePath != null || isVoicePlaying()) {
            LOGGER.info("显式停止语音。");
        }
        stopVoiceInternal();
    }

    public synchronized boolean isMusicPlaying() {
        if (musicSource == -1) return false;
        boolean playing = alGetSourcei(musicSource, AL_SOURCE_STATE) == AL_PLAYING;
        // Don't spam errors if source is invalid, just return false
        // checkALError("isMusicPlaying - alGetSourcei");
        return playing && alGetError() == AL_NO_ERROR; // Check error state after get
    }

    public synchronized boolean isVoicePlaying() {
        if (voiceSource == -1) return false;
        boolean playing = alGetSourcei(voiceSource, AL_SOURCE_STATE) == AL_PLAYING;
        // checkALError("isVoicePlaying - alGetSourcei");
        return playing && alGetError() == AL_NO_ERROR;
    }


    public synchronized void setMusicVolume(float volume) {
        if (musicSource != -1) {
            alSourcef(musicSource, AL_GAIN, Math.max(0.0f, Math.min(1.0f, volume)));
            checkALError("setMusicVolume");
        }
    }

    public synchronized void setVoiceVolume(float volume) {
        if (voiceSource != -1) {
            alSourcef(voiceSource, AL_GAIN, Math.max(0.0f, Math.min(1.0f, volume)));
            checkALError("setVoiceVolume");
        }
    }

    public synchronized void destroy() {
        LOGGER.info("销毁GalSoundInstance...");
        stopMusicInternal();
        stopVoiceInternal();

        if (musicSource != -1) {
            alDeleteSources(musicSource);
            musicSource = -1;
        }
        if (voiceSource != -1) {
            alDeleteSources(voiceSource);
            voiceSource = -1;
        }
        checkALError("destroy - delete sources");

        if (ownsOpenALContext) {
            if (context != NULL) {
                // Detach context from current thread before destroying if it is current
                if (alcGetCurrentContext() == context) {
                    alcMakeContextCurrent(NULL);
                }
                alcDestroyContext(context);
                context = NULL;
            }
            if (device != NULL) {
                alcCloseDevice(device);
                device = NULL;
            }
            LOGGER.info("OpenAL context and device destroyed by GalSoundInstance.");
        } else {
            LOGGER.info("GalSoundInstance did not destroy OpenAL context/device as it was shared.");
        }
        instance = null; // Allow re-creation via getInstance()
        LOGGER.info("GalSoundInstance已销毁。");
    }

    private static boolean checkALError(String operation) {
        int error = alGetError();
        if (error != AL_NO_ERROR) {
            String errorName = switch (error) {
                case AL_INVALID_NAME -> "AL_INVALID_NAME";
                case AL_INVALID_ENUM -> "AL_INVALID_ENUM";
                case AL_INVALID_VALUE -> "AL_INVALID_VALUE";
                case AL_INVALID_OPERATION -> "AL_INVALID_OPERATION";
                case AL_OUT_OF_MEMORY -> "AL_OUT_OF_MEMORY";
                default -> "未知AL错误";
            };
            LOGGER.error("OpenAL错误 ({}) 在操作 '{}': {} ({})", Thread.currentThread().getName(), operation, errorName, error);
            return true;
        }
        return false;
    }

    private static class SoundData {
        // Buffers for OpenAL's alBufferData. One of these will be non-null.
        final ShortBuffer pcmDataForALShort;
        final ByteBuffer pcmDataForALByte;

        // The actual native buffer allocated by STB or MemoryUtil.memAlloc. This is what needs freeing.
        // It can be a ShortBuffer (from STB) or a ByteBuffer (from WAV loader).
        private final Buffer allocatedNativeBuffer;

        final int format;
        final int sampleRate;

        // Constructor for STB-decoded data (16-bit, STB directly allocates a ShortBuffer)
        public SoundData(ShortBuffer stbAllocatedShortBuffer, int alFormat, int sampleRate) {
            if (alFormat != AL_FORMAT_MONO16 && alFormat != AL_FORMAT_STEREO16) {
                MemoryUtil.memFree(stbAllocatedShortBuffer); // Clean up if format is wrong
                throw new IllegalArgumentException("Invalid AL format for STB ShortBuffer data. Expected 16-bit mono/stereo.");
            }
            this.pcmDataForALShort = stbAllocatedShortBuffer;
            this.pcmDataForALByte = null;
            this.allocatedNativeBuffer = stbAllocatedShortBuffer; // This ShortBuffer is the native allocation
            this.format = alFormat;
            this.sampleRate = sampleRate;
        }

        // Constructor for WAV 16-bit data (we allocate ByteBuffer, OpenAL needs a ShortBuffer view)
        public SoundData(ByteBuffer memAllocatedByteBuffer, int alFormat, int sampleRate, boolean is16bitWav) {
            if (!is16bitWav) { // Should not be called with false, use other constructor for 8-bit
                MemoryUtil.memFree(memAllocatedByteBuffer);
                throw new IllegalArgumentException("This constructor is specifically for 16-bit WAV data.");
            }
            if (alFormat != AL_FORMAT_MONO16 && alFormat != AL_FORMAT_STEREO16) {
                MemoryUtil.memFree(memAllocatedByteBuffer);
                throw new IllegalArgumentException("Invalid AL format for 16-bit WAV ByteBuffer data. Expected 16-bit mono/stereo.");
            }
            this.pcmDataForALShort = memAllocatedByteBuffer.asShortBuffer(); // View for OpenAL
            this.pcmDataForALByte = null;
            this.allocatedNativeBuffer = memAllocatedByteBuffer; // This ByteBuffer is the native allocation
            this.format = alFormat;
            this.sampleRate = sampleRate;
        }

        // Constructor for WAV 8-bit data (we allocate ByteBuffer, OpenAL uses it directly)
        public SoundData(ByteBuffer memAllocatedByteBuffer, int alFormat, int sampleRate) {
            if (alFormat != AL_FORMAT_MONO8 && alFormat != AL_FORMAT_STEREO8) {
                MemoryUtil.memFree(memAllocatedByteBuffer);
                throw new IllegalArgumentException("Invalid AL format for 8-bit WAV ByteBuffer data. Expected 8-bit mono/stereo.");
            }
            this.pcmDataForALShort = null;
            this.pcmDataForALByte = memAllocatedByteBuffer; // Direct use for OpenAL
            this.allocatedNativeBuffer = memAllocatedByteBuffer; // This ByteBuffer is the native allocation
            this.format = alFormat;
            this.sampleRate = sampleRate;
        }

        public void dispose() {
            if (allocatedNativeBuffer != null) {
                // allocatedNativeBuffer is either ShortBuffer or ByteBuffer, memFree is overloaded
                MemoryUtil.memFree(allocatedNativeBuffer);
            }
            // pcmDataForALShort/Byte are either the same as allocatedNativeBuffer or views,
            // so no separate free needed for them.
        }
    }
}