package org.cneko.gal.common.util.pack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.cneko.gal.common.Gal;

import java.util.List;

import static net.minecraft.client.Minecraft.useFancyGraphics;
import static org.cneko.gal.common.util.pack.ExternalPack.NAMESPACED_SOUNDS_JSON;
import static org.cneko.gal.common.util.pack.ExternalPack.soundsJsonLock;
import static net.minecraft.client.Minecraft.getInstance;

public class DynamicSoundManager {
    private static final Gson GSON = new GsonBuilder().create();

    public static void init() {
    }

    // 注册或更新SoundEvent
    public static void registerSoundEvent(String namespace, String eventName, SoundEventConfig config) {
        // 更新或创建sounds.json
        JsonObject soundsJson = getOrCreateSoundsJson(namespace);
        JsonObject eventEntry = new JsonObject();

        // 设置声音分类（category）
        if (config.category != null) {
            eventEntry.addProperty("category", config.category);
        }

        // 创建声音数组
        JsonArray soundsArray = new JsonArray();

        // 添加每个声音变体
        for (SoundVariant variant : config.variants) {
            JsonObject soundEntry = new JsonObject();
            soundEntry.addProperty("name", namespace + ":" + variant.path);

            // 设置可选属性
            if (variant.stream != null) {
                soundEntry.addProperty("stream", variant.stream);
            }
            if (variant.volume != null) {
                soundEntry.addProperty("volume", variant.volume);
            }
            if (variant.pitch != null) {
                soundEntry.addProperty("pitch", variant.pitch);
            }
            if (variant.weight != null) {
                soundEntry.addProperty("weight", variant.weight);
            }

            soundsArray.add(soundEntry);
        }

        eventEntry.add("sounds", soundsArray);
        soundsJson.add(eventName, eventEntry);

        var loc = ResourceLocation.fromNamespaceAndPath(namespace, eventName);
        var events = new WeighedSoundEvents(ResourceLocation.fromNamespaceAndPath(namespace, eventName), null);
        events.addSound(new Sound(
                loc,
                random -> 1.0f,
                random -> 1.0f,
                1,
                Sound.Type.SOUND_EVENT,
                true,
                true,
                10
        ));
        getInstance().getSoundManager().registry.put(loc, events);
        // 更新资源包
        ExternalPack.updateSoundsJson(namespace, soundsJson);

        // 刷新声音
        //ExternalPack.applyPendingSoundRefreshes(getInstance().getSoundManager());
        //Registry.register(BuiltInRegistries.SOUND_EVENT, loc, SoundEvent.createVariableRangeEvent(loc));
        Gal.LOGGER.info("Registered sound event: {}:{} with {} variants",
                namespace, eventName, config.variants.size());
    }

    // 配置类
    public static class SoundEventConfig {
        public String category; // 如 "record", "master" 等
        public List<SoundVariant> variants;

        public SoundEventConfig(String category, List<SoundVariant> variants) {
            this.category = category;
            this.variants = variants;
        }
    }

    // 声音变体类
    public static class SoundVariant {
        public String path;      // 如 "music/kawaii" 或 "item/bazooka/biu/biu0"
        public Boolean stream;   // 是否流式播放（用于长音频）
        public Float volume;     // 音量
        public Float pitch;      // 音高
        public Integer weight;   // 权重（用于随机选择）

        public SoundVariant(String path) {
            this.path = path;
        }

        public SoundVariant setStream(boolean stream) {
            this.stream = stream;
            return this;
        }

        public SoundVariant setVolume(float volume) {
            this.volume = volume;
            return this;
        }

        public SoundVariant setPitch(float pitch) {
            this.pitch = pitch;
            return this;
        }

        public SoundVariant setWeight(int weight) {
            this.weight = weight;
            return this;
        }
    }

    private static JsonObject getOrCreateSoundsJson(String namespace) {
        JsonObject soundsJson;
        soundsJsonLock.readLock().lock();
        try {
            soundsJson = NAMESPACED_SOUNDS_JSON.get(namespace);
            if (soundsJson == null) {
                soundsJson = new JsonObject();
            } else {
                // 创建深拷贝以避免并发修改
                soundsJson = GSON.fromJson(soundsJson.toString(), JsonObject.class);
            }
        } finally {
            soundsJsonLock.readLock().unlock();
        }
        return soundsJson;
    }

    // 播放SoundEvent
    public static void playSoundEvent(ResourceLocation eventLocation,float pitch) {
        SimpleSoundInstance sound = SimpleSoundInstance.forUI(
                SoundEvent.createFixedRangeEvent(eventLocation, pitch),
                pitch
        );
        getInstance().getSoundManager().play(sound);
    }
}
