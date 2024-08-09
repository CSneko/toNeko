package org.cneko.toneko.neoforge.fabric.network.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;

import static org.cneko.toneko.common.Bootstrap.MODID;

public record EntityPosePayload(Pose pose, boolean status) implements CustomPacketPayload {
    public static final Type<EntityPosePayload> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "entity_set_pose"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EntityPosePayload> CODEC = StreamCodec.composite(
            Pose.STREAM_CODEC, EntityPosePayload::pose,
            ByteBufCodecs.BOOL, EntityPosePayload::status,
            EntityPosePayload::new);
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
