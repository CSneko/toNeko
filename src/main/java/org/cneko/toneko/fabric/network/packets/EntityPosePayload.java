package org.cneko.toneko.fabric.network.packets;

import net.minecraft.entity.EntityPose;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Identifier;

import static org.cneko.toneko.common.Bootstrap.MODID;

public record EntityPosePayload(EntityPose pose, boolean status) implements CustomPayload {
    public static final CustomPayload.Id<EntityPosePayload> ID = new CustomPayload.Id<>(Identifier.of(MODID, "entity_set_pose"));
    public static final PacketCodec<RegistryByteBuf, EntityPosePayload> CODEC = PacketCodec.tuple(
            EntityPose.PACKET_CODEC, EntityPosePayload::pose,
            PacketCodecs.BOOL, EntityPosePayload::status,
            EntityPosePayload::new);
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
