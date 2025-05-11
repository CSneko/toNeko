package org.cneko.gal.common.util.pack;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.flag.FeatureFlags;
import org.cneko.gal.common.Gal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ExternalPack implements PackResources {
    // 静态资源映射表，存储ResourceLocation到实际文件路径的映射
    private static final Map<ResourceLocation, Path> RESOURCE_MAP = new HashMap<>();
    private static final ReadWriteLock mapLock = new ReentrantReadWriteLock();
    private final PackLocationInfo locationInfo;

    private static final Set<ResourceLocation> pendingTextureRefreshes = new HashSet<>();
    private static final ReadWriteLock textureRefreshLock = new ReentrantReadWriteLock();

    private static final Set<ResourceLocation> pendingSoundRefreshes = new HashSet<>();
    private static final ReadWriteLock soundRefreshLock = new ReentrantReadWriteLock();

    public static final Map<String, JsonObject> NAMESPACED_SOUNDS_JSON = new HashMap<>();
    public static final ReadWriteLock soundsJsonLock = new ReentrantReadWriteLock();

    public ExternalPack(PackLocationInfo locationInfo) {
        this.locationInfo = locationInfo;
    }

    /**
     * 添加资源映射
     * @param location 资源定位符
     * @param filePath 实际文件路径
     */
    public static void addResource(ResourceLocation location, Path filePath) {
        mapLock.writeLock().lock();
        try {
            RESOURCE_MAP.put(location, filePath);
            if (location.getPath().startsWith("textures/")) {
                textureRefreshLock.writeLock().lock();
                try {
                    pendingTextureRefreshes.add(location);
                } finally {
                    textureRefreshLock.writeLock().unlock();
                }
            } else if (location.getPath().endsWith(".ogg")) {
                soundRefreshLock.writeLock().lock();
                try {
                    pendingSoundRefreshes.add(location);
                } finally {
                    soundRefreshLock.writeLock().unlock();
                }
            }
        } finally {
            mapLock.writeLock().unlock();
        }
    }
    /**
     * 移除资源映射
     * @param location 资源定位符
     */
    public static void removeResource(ResourceLocation location) {
        mapLock.writeLock().lock();
        try {
            RESOURCE_MAP.remove(location);
            if (location.getPath().startsWith("textures/")) {
                textureRefreshLock.writeLock().lock();
                try {
                    pendingTextureRefreshes.add(location);
                } finally {
                    textureRefreshLock.writeLock().unlock();
                }
            }else if (location.getPath().startsWith("sounds/")) {
                soundRefreshLock.writeLock().lock();
                try {
                    pendingSoundRefreshes.add(location);
                } finally {
                    soundRefreshLock.writeLock().unlock();
                }
            }
        } finally {
            mapLock.writeLock().unlock();
        }
    }

    /**
     * 清空所有资源映射
     */
    public static void clearResources() {
        mapLock.writeLock().lock();
        try {
            if (!RESOURCE_MAP.isEmpty()) {
                // 如果清除所有纹理，则所有现有纹理都需要“刷新”
                // 这是一种简化,更稳健的清除方法是在清除之前迭代 RESOURCE_MAP
                // 将所有纹理位置添加到 pendingTextureRefreshes 中
                RESOURCE_MAP.forEach((loc, path) -> {
                    if (loc.getPath().startsWith("textures/")) {
                        textureRefreshLock.writeLock().lock();
                        try {
                            pendingTextureRefreshes.add(loc);
                        } finally {
                            textureRefreshLock.writeLock().unlock();
                        }
                    }
                });
                RESOURCE_MAP.clear();
            }
        } finally {
            mapLock.writeLock().unlock();
        }
    }

    /**
     * 尝试刷新已通过 addResource/removeResource 标记的纹理
     * 这会告知 TextureManager 释放指定的纹理。它们将在下次需要时从 ResourceManager（以及此资源包）重新加载
     * <p>
     * 这主要用于纹理。其他资源类型（模型、声音、语言）
     * 无法通过这种方式可靠地更新，可能仍需要完整的
     * Minecraft.reloadResourcePacks()
     * <p>
     * 必须在主客户端线程上调用。
     */
    public static void applyPendingTextureRefreshes() {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isSameThread()) {
            mc.execute(ExternalPack::applyPendingTextureRefreshes);
            return;
        }

        Set<ResourceLocation> toRefresh;
        textureRefreshLock.writeLock().lock();
        try {
            if (pendingTextureRefreshes.isEmpty()) {
                return;
            }
            toRefresh = new HashSet<>(pendingTextureRefreshes);
            pendingTextureRefreshes.clear();
        } finally {
            textureRefreshLock.writeLock().unlock();
        }

        if (!toRefresh.isEmpty()) {
            Gal.LOGGER.info("Refreshing {} textures in TextureManager.", toRefresh.size());
            TextureManager textureManager = mc.getTextureManager();
            for (ResourceLocation location : toRefresh) {
                textureManager.release(location);
            }

        }
    }

    public static void applyPendingSoundRefreshes(SoundManager soundManager) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isSameThread()) {
            mc.execute(() -> applyPendingSoundRefreshes(soundManager));
            return;
        }

        Set<ResourceLocation> toRefresh;
        soundRefreshLock.writeLock().lock();
        try {
            if (pendingSoundRefreshes.isEmpty()) {
                return;
            }
            toRefresh = new HashSet<>(pendingSoundRefreshes);
            pendingSoundRefreshes.clear();
        } finally {
            soundRefreshLock.writeLock().unlock();
        }

        if (!toRefresh.isEmpty()) {
            Gal.LOGGER.info("Refreshing {} sounds in SoundManager.", toRefresh.size());

            // 强制SoundManager重新加载这些声音
            soundManager.prepare(mc.getResourceManager(), mc.getProfiler());
        }
    }

    public static void updateSoundsJson(String namespace, JsonObject soundsJson) {
        soundsJsonLock.writeLock().lock();
        try {
            NAMESPACED_SOUNDS_JSON.put(namespace, soundsJson);
            // 标记所有该命名空间下的声音需要刷新
            RESOURCE_MAP.keySet().stream()
                    .filter(loc -> loc.getNamespace().equals(namespace) && loc.getPath().startsWith("sounds/"))
                    .forEach(loc -> {
                        soundRefreshLock.writeLock().lock();
                        try {
                            pendingSoundRefreshes.add(loc);
                        } finally {
                            soundRefreshLock.writeLock().unlock();
                        }
                    });
        } finally {
            soundsJsonLock.writeLock().unlock();
        }
    }

    public static boolean containsResource(ResourceLocation resourceLocation) {
        mapLock.readLock().lock();
        try {
            return RESOURCE_MAP.containsKey(resourceLocation);
        } finally {
            mapLock.readLock().unlock();
        }
    }

    public static boolean containsSoundsJson() {
        soundsJsonLock.readLock().lock();
        try {
            return !NAMESPACED_SOUNDS_JSON.isEmpty();
        }finally {
            soundsJsonLock.readLock().unlock();
        }
    }

    @Override
    public @NotNull Set<String> getNamespaces(@NotNull PackType type) {
        mapLock.readLock().lock();
        try {
            Set<String> namespaces = new HashSet<>();
            namespaces.add("gal");
            namespaces.add("toneko");
            namespaces.add("gals");
            return namespaces;
        } finally {
            mapLock.readLock().unlock();
        }
    }

    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(String @NotNull ... elements) {
        return null;
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(@NotNull PackType packType, @NotNull ResourceLocation location) {
        return getResourceStatic(packType, location);
    }
    public static @Nullable IoSupplier<InputStream> getResourceStatic(@NotNull PackType packType, @NotNull ResourceLocation location) {
        // 处理sounds.json请求
        if (location.getPath().equals("sounds.json")) {
            soundsJsonLock.readLock().lock();
            try {
                JsonObject soundsJson = NAMESPACED_SOUNDS_JSON.get(location.getNamespace());
                if (soundsJson != null) {
                    return () -> new ByteArrayInputStream(soundsJson.toString().getBytes(StandardCharsets.UTF_8));
                }
            } finally {
                soundsJsonLock.readLock().unlock();
            }
            return null;
        }

        mapLock.readLock().lock();
        try {
            Path filePath = RESOURCE_MAP.get(location);
            if (filePath != null && Files.exists(filePath)) {
                return () -> Files.newInputStream(filePath);
            }
            return null;
        } finally {
            mapLock.readLock().unlock();
        }
    }

    @Override
    public void listResources(@NotNull PackType packType, @NotNull String namespace, @NotNull String path, @NotNull ResourceOutput resourceOutput) {
        mapLock.readLock().lock();
        // 遍历映射表，添加匹配的资源
        try {
            RESOURCE_MAP.forEach((location, filePath) -> {
                if (location.getNamespace().equals(namespace) &&
                        location.getPath().startsWith(path) &&
                        Files.exists(filePath)) {
                    resourceOutput.accept(location, () -> Files.newInputStream(filePath));
                }
            });
        } finally {
            mapLock.readLock().unlock();
        }

        // 如果请求的是根目录，包含sounds.json
        if (packType == PackType.CLIENT_RESOURCES && path.isEmpty()) {
            soundsJsonLock.readLock().lock();
            try {
                if (NAMESPACED_SOUNDS_JSON.containsKey(namespace)) {
                    ResourceLocation soundsJsonLoc = ResourceLocation.fromNamespaceAndPath(namespace, "sounds.json");
                    JsonObject soundsJson = NAMESPACED_SOUNDS_JSON.get(namespace);
                    resourceOutput.accept(soundsJsonLoc, () ->
                            new ByteArrayInputStream(soundsJson.toString().getBytes(StandardCharsets.UTF_8)));
                }
            } finally {
                soundsJsonLock.readLock().unlock();
            }
        }
    }

    @Override
    public @Nullable <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) throws IOException {
        return null;
    }

    @Override
    public PackLocationInfo location() {
        return this.locationInfo;
    }

    @Override
    public void close() {
    }

    private static final String packId = "gal_pack";
    private static final int PACK_FORMAT = 34;
    private static final Component PACK_DESCRIPTION = Component.literal("GAL Dynamic Resources");
    public static final PackLocationInfo LOCATION_INFO = new PackLocationInfo(
            packId,
            Component.literal("GAL Pack"),
            PackSource.DEFAULT,
            Optional.empty()
    );
    public static Pack createResourcePack() {
        //  创建 ResourcesSupplier
        Pack.ResourcesSupplier supplier = new Pack.ResourcesSupplier() {
            @Override
            public PackResources openPrimary(@NotNull PackLocationInfo info) {
                return new ExternalPack(info);
            }

            @Override
            public PackResources openFull(PackLocationInfo info, Pack.Metadata metadata) {
                return openPrimary(info); // 简化处理，不使用overlays
            }
        };

        //  创建 Metadata
        Pack.Metadata metadata = createPackMetadata();

        //  创建 PackSelectionConfig
        PackSelectionConfig selectionConfig = new PackSelectionConfig(
                true,   // required
                Pack.Position.TOP,
                false   // fixedPosition
        );

        // 创建 Pack 实例
        return new Pack(
                LOCATION_INFO,
                supplier,
                metadata,
                selectionConfig
        );
    }

    private static Pack.Metadata createPackMetadata() {
        return new Pack.Metadata(
                PACK_DESCRIPTION,
                PackCompatibility.COMPATIBLE,
                FeatureFlags.DEFAULT_FLAGS,
                List.of() // 空overlays列表
        );
    }
}