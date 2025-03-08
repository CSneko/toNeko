package org.cneko.toneko.common.mod.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static org.cneko.toneko.common.Bootstrap.MODID;

public record TTSSendPayload(String text) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TTSSendPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "tts_send"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TTSSendPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TTSSendPayload::text,
            TTSSendPayload::new
    );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
