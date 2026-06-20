package org.cneko.toneko.common.mod.packets.interactives;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static org.cneko.toneko.common.Bootstrap.MODID;

/**
 * C2S: Client requests chat history for a specific neko.
 */
public record ChatHistoryRequestPayload(String nekoUuid) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ChatHistoryRequestPayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "chat_history_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChatHistoryRequestPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ChatHistoryRequestPayload::nekoUuid, ChatHistoryRequestPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
}
