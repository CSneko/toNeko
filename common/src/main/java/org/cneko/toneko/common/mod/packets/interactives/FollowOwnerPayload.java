package org.cneko.toneko.common.mod.packets.interactives;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static org.cneko.toneko.common.Bootstrap.MODID;

public record FollowOwnerPayload(String uuid) implements CustomPacketPayload{
    public static final CustomPacketPayload.Type<FollowOwnerPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "neko_entity_interactive_follow_owner"));
    public static final StreamCodec<RegistryFriendlyByteBuf,FollowOwnerPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,FollowOwnerPayload::uuid,
            FollowOwnerPayload::new
    );
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
