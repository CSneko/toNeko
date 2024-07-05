package org.cneko.toneko.fabric.network.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.util.Identifier;

import static org.cneko.toneko.common.Bootstrap.MODID;

/**
 * 这个包仅仅是用来检测客户端是否安装了ToNeko的
 * @param status 这个随便写
 */
public record ToNekoModCheckPayload(boolean status) implements CustomPayload {
    public static final CustomPayload.Id<ToNekoModCheckPayload> ID = new CustomPayload.Id<>(Identifier.of(MODID, "mod_check"));
    public static final PacketCodec<RegistryByteBuf, ToNekoModCheckPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, ToNekoModCheckPayload::status,
            ToNekoModCheckPayload::new
    );
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }


}
