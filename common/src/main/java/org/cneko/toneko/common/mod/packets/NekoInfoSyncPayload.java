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
        boolean isNeko,
        int age
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NekoInfoSyncPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "neko_info_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NekoInfoSyncPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                ByteBufCodecs.FLOAT.encode(buf, payload.energy());
                ByteBufCodecs.FLOAT.encode(buf, payload.maxEnergy());
                ByteBufCodecs.DOUBLE.encode(buf, payload.interactionRaw());
                ByteBufCodecs.DOUBLE.encode(buf, payload.combatRaw());
                ByteBufCodecs.DOUBLE.encode(buf, payload.baseRaw());
                ByteBufCodecs.BOOL.encode(buf, payload.isNeko());
                ByteBufCodecs.INT.encode(buf, payload.age());
            },
            buf -> new NekoInfoSyncPayload(
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.INT.decode(buf)
            )
    );
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
