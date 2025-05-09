package org.cneko.gal.common.util.pack;

import net.minecraft.client.Minecraft;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ExternalPack implements PackResources {
    // 静态资源映射表，存储ResourceLocation到实际文件路径的映射
    private static final Map<ResourceLocation, Path> RESOURCE_MAP = new HashMap<>();
    private final PackLocationInfo locationInfo;

    public ExternalPack(PackLocationInfo locationInfo) {
        this.locationInfo = locationInfo;
    }

    /**
     * 添加资源映射
     * @param location 资源定位符
     * @param filePath 实际文件路径
     */
    public static void addResource(ResourceLocation location, Path filePath) {
        RESOURCE_MAP.put(location, filePath);
    }

    /**
     * 移除资源映射
     * @param location 资源定位符
     */
    public static void removeResource(ResourceLocation location) {
        RESOURCE_MAP.remove(location);
    }

    /**
     * 清空所有资源映射
     */
    public static void clearResources() {
        RESOURCE_MAP.clear();
    }

    @Override
    public @NotNull Set<String> getNamespaces(@NotNull PackType type) {
        return Set.of("gal","toneko","minecraft");
    }

    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(String @NotNull ... elements) {
        return null;
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(@NotNull PackType packType, @NotNull ResourceLocation location) {
        Path filePath = RESOURCE_MAP.get(location);
        if (filePath != null && Files.exists(filePath)) {
            return () -> Files.newInputStream(filePath);
        }
        return null;
    }

    @Override
    public void listResources(@NotNull PackType packType, @NotNull String namespace, @NotNull String path, @NotNull ResourceOutput resourceOutput) {
        // 遍历映射表，添加匹配的资源
        RESOURCE_MAP.forEach((location, filePath) -> {
            if (location.getNamespace().equals(namespace) &&
                    location.getPath().startsWith(path) &&
                    Files.exists(filePath)) {
                resourceOutput.accept(location, () -> Files.newInputStream(filePath));
            }
        });
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