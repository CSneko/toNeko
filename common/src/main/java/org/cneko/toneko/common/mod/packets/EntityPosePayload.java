package org.cneko.toneko.common.mod.packets;

import static org.cneko.toneko.common.Bootstrap.MODID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record EntityPosePayload(@NotNull Pose pose, @Nullable String uuid , boolean status) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<EntityPosePayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "entity_set_pose"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EntityPosePayload> CODEC = StreamCodec.composite(
            Pose.STREAM_CODEC, EntityPosePayload::pose,
            ByteBufCodecs.STRING_UTF8, EntityPosePayload::uuid,
            ByteBufCodecs.BOOL, EntityPosePayload::status,
            EntityPosePayload::new);
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
