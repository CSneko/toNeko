package org.cneko.toneko.common.mod.packets.interactives;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static org.cneko.toneko.common.Bootstrap.MODID;

/**
 * C2S: Player toggles chat mode (area=false means global, area=true means 64-block area chat).
 */
public record ChatModePayload(boolean area) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ChatModePayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "chat_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChatModePayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, ChatModePayload::area, ChatModePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
}
