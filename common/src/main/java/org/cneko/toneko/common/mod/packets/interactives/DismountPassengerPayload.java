package org.cneko.toneko.common.mod.packets.interactives;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static org.cneko.toneko.common.Bootstrap.MODID;

/**
 * C2S 数据包：玩家请求让头上的乘客下来
 */
public record DismountPassengerPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DismountPassengerPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "dismount_passenger"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DismountPassengerPayload> CODEC = StreamCodec.unit(new DismountPassengerPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}