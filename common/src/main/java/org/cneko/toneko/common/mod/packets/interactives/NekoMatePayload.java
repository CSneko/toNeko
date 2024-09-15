package org.cneko.toneko.common.mod.packets.interactives;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static org.cneko.toneko.common.Bootstrap.MODID;

public record NekoMatePayload(String uuid, String mateUuid) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NekoMatePayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "neko_entity_interactive_mate"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NekoMatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, NekoMatePayload::uuid,
            ByteBufCodecs.STRING_UTF8, NekoMatePayload::mateUuid,
            NekoMatePayload::new
    );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
