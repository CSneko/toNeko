package org.cneko.toneko.common.mod.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.common.Bootstrap.MODID;

public record NekoInfoSyncPayload(float energy) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NekoInfoSyncPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "neko_info_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NekoInfoSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, NekoInfoSyncPayload::energy,
            NekoInfoSyncPayload::new);
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
