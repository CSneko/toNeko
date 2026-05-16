package org.cneko.toneko.common.mod.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.common.Bootstrap.MODID;

public record NekoInfoSyncPayload(
        float energy,
        float maxEnergy,
        double interactionRaw,
        double combatRaw,
        double baseRaw,
        boolean isNeko
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NekoInfoSyncPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "neko_info_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NekoInfoSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, NekoInfoSyncPayload::energy,
            ByteBufCodecs.FLOAT, NekoInfoSyncPayload::maxEnergy,
            ByteBufCodecs.DOUBLE, NekoInfoSyncPayload::interactionRaw,
            ByteBufCodecs.DOUBLE, NekoInfoSyncPayload::combatRaw,
            ByteBufCodecs.DOUBLE, NekoInfoSyncPayload::baseRaw,
            ByteBufCodecs.BOOL, NekoInfoSyncPayload::isNeko,
            NekoInfoSyncPayload::new);
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
