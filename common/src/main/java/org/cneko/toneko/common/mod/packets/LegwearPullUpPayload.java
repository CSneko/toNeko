package org.cneko.toneko.common.mod.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

/**
 * 提袜（C2S）：把穿着的过膝袜袜口复位到自然高度。
 * 服务端权威处理并带冷却，见 ToNekoNetworkEvents.onLegwearPullUp。
 */
public record LegwearPullUpPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LegwearPullUpPayload> ID =
            new CustomPacketPayload.Type<>(toNekoLoc("legwear_pull_up"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LegwearPullUpPayload> CODEC =
            StreamCodec.unit(new LegwearPullUpPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
