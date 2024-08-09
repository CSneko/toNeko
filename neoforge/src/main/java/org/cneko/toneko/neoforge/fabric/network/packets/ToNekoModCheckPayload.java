package org.cneko.toneko.neoforge.fabric.network.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static org.cneko.toneko.common.Bootstrap.MODID;

/**
 * 这个包仅仅是用来检测客户端是否安装了ToNeko的
 * @param status 这个随便写
 */
public record ToNekoModCheckPayload(boolean status) implements CustomPacketPayload {
    public static final Type<ToNekoModCheckPayload> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "mod_check"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToNekoModCheckPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ToNekoModCheckPayload::status,
            ToNekoModCheckPayload::new
    );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }


}
