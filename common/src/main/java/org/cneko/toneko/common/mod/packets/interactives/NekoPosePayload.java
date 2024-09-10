package org.cneko.toneko.common.mod.packets.interactives;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.cneko.toneko.common.Bootstrap.MODID;

public record NekoPosePayload(@NotNull Pose pose, @NotNull String uuid , boolean status) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NekoPosePayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "entity_set_pose"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NekoPosePayload> CODEC = StreamCodec.composite(
            Pose.STREAM_CODEC, NekoPosePayload::pose,
            ByteBufCodecs.STRING_UTF8, NekoPosePayload::uuid,
            ByteBufCodecs.BOOL, NekoPosePayload::status,
            NekoPosePayload::new);
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}