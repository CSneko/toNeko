package org.cneko.toneko.common.mod.packets;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

public record ToNekoManagementDataPayload(CompoundTag data) implements CustomPacketPayload {
    public static final Type<ToNekoManagementDataPayload> ID = new Type<>(toNekoLoc("toneko_management_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToNekoManagementDataPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, ToNekoManagementDataPayload::data,
            ToNekoManagementDataPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
