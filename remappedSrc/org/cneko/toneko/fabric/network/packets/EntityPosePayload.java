package org.cneko.toneko.fabric.network.packets;

import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.codec.PacketCodecs;
public record EntityPosePayload(EntityPose pose, boolean status) implements CustomPayload {
    public static final CustomPayload.Id<EntityPosePayload> ID = new CustomPayload.Id<>(EntitySetPoseContents.ID);
    public static final PacketCodec<RegistryByteBuf, EntityPosePayload> CODEC = PacketCodec.tuple(
            EntityPose.PACKET_CODEC, EntityPosePayload::pose,
            PacketCodecs.BOOL, EntityPosePayload::status,
            EntityPosePayload::new);
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
