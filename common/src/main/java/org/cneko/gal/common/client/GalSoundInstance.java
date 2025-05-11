package org.cneko.gal.common.client;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

import java.io.*;

import static org.cneko.gal.common.Gal.LOGGER;
public class GalSoundInstance {

    private SoundPlayer musicPlayer;  // 负责放BGM的小可爱
    private SoundPlayer voicePlayer;  // 负责念语音的小可爱

    private final Object musicLock = new Object();  // 防止BGM打架的锁
    private final Object voiceLock = new Object();  // 防止语音打架的锁

    private static GalSoundInstance instance;
    public static GalSoundInstance getInstance() {
        if (instance == null) {
            instance = new GalSoundInstance();
        }
        return instance;
    }
    private static class SoundPlayer implements Runnable {
        private final String filePath;  // 音乐文件在哪里呢
        private final boolean loop;      // 要不要单曲循环呀
        private volatile boolean playing;  // 现在在播放吗（volatile保证线程安全）
        private Player activePlayer;     // 真正干活的播放器君
        private Thread playerThread;    // 播放线程小助手

        public SoundPlayer(String filePath, boolean loop) {
            this.filePath = filePath;
            this.loop = loop;
            this.playing = false;
        }

        public void start() {
            if (playerThread != null && playerThread.isAlive()) {
                // 啊咧咧？播放线程已经在工作啦！
                LOGGER.error("警告：试图启动一个已经在工作的播放线程 {}", filePath);
                return;
            }
            playing = true;
            playerThread = new Thread(this, "SoundPlayer-" + (loop ? "BGM-" : "语音-") + filePath.substring(filePath.lastIndexOf('/') + 1));
            playerThread.setDaemon(true);  // 设为守护线程，这样主程序退出时它也会乖乖退出
            playerThread.start();
        }

        @Override
        public void run() {
            do {
                if (!playing) break;  // 检查是否要继续播放

                try (InputStream is = new BufferedInputStream(new FileInputStream(filePath))) {
                    activePlayer = new Player(is);  // 创建一个新的播放器

                    activePlayer.play();  // 开始播放（会一直阻塞直到播完）

                } catch (FileNotFoundException e) {
                    LOGGER.error("错误：找不到声音文件啦 {} - {}", filePath, e.getMessage());
                    playing = false;  // 找不到文件就不播啦
                } catch (JavaLayerException | IOException e) {
                    // 如果是主动停止播放，这里会正常抛出异常
                    if (playing) {  // 如果不是主动停止，那就是出问题啦
                        LOGGER.error("播放 {} 时出错啦：{}", filePath, e.getMessage());
                    }
                    playing = false;
                } finally {
                    if (activePlayer != null) {
                        activePlayer.close();  // 记得关掉播放器哦
                        activePlayer = null;
                    }
                }
            } while (loop && playing);  // 如果需要循环且还在播放状态，就再来一遍

            playing = false;  // 确保状态正确
        }

        public synchronized void stop() {
            playing = false;  // 告诉循环该停下来啦
            if (activePlayer != null) {
                activePlayer.close();  // 这样会中断player.play()
            }
        }

        public boolean isPlaying() {
            return playing && playerThread != null && playerThread.isAlive();
        }
    }

    // --- 以下是公开方法 ---

    public void playMusic(String filePath, boolean loop) {
        synchronized (musicLock) {
            stopMusicInternal();  // 先停掉当前的BGM
            if (filePath == null || filePath.isEmpty()) {
                LOGGER.error("BGM路径是空的啦，是不是忘记设置啦？");
                return;
            }
            LOGGER.info("正在播放BGM：{}{}", filePath, loop ? " (单曲循环中~)" : "");
            musicPlayer = new SoundPlayer(filePath, loop);
            musicPlayer.start();
        }
    }

    public void stopMusic() {
        synchronized (musicLock) {
            stopMusicInternal();
        }
    }

    private void stopMusicInternal() {
        if (musicPlayer != null) {
            musicPlayer.stop();
            musicPlayer = null;
        }
    }

    public void playVoice(String filePath) {
        synchronized (voiceLock) {
            stopVoiceInternal();  // 先停掉当前的语音
            if (filePath == null || filePath.isEmpty()) {
                LOGGER.error("语音路径是空的啦，是不是忘记设置啦？");
                return;
            }
            LOGGER.info("正在念语音：{}", filePath);
            voicePlayer = new SoundPlayer(filePath, false);  // 语音一般不需要循环
            voicePlayer.start();
        }
    }

    public void stopVoice() {
        synchronized (voiceLock) {
            stopVoiceInternal();
        }
    }

    private void stopVoiceInternal() {
        if (voicePlayer != null) {
            voicePlayer.stop();
            voicePlayer = null;
        }
    }

    public void stopAllSounds() {
        LOGGER.info("正在关掉所有声音...安静下来啦~");
        stopMusic();
        stopVoice();
    }

    public boolean isMusicPlaying() {
        synchronized(musicLock) {
            return musicPlayer != null && musicPlayer.isPlaying();
        }
    }

    public boolean isVoicePlaying() {
        synchronized(voiceLock) {
            return voicePlayer != null && voicePlayer.isPlaying();
        }
    }
}