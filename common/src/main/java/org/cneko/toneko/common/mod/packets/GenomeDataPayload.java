package org.cneko.toneko.common.mod.packets;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

public record GenomeDataPayload(int entityId, CompoundTag genomeNbt,boolean canEdit) implements CustomPacketPayload {
    public static final Type<GenomeDataPayload> ID = new Type<>(toNekoLoc("genome_data"));

    public static final StreamCodec<FriendlyByteBuf, GenomeDataPayload> CODEC = CustomPacketPayload.codec(
            (payload, buf) -> {
                buf.writeInt(payload.entityId);
                buf.writeNbt(payload.genomeNbt);
                buf.writeBoolean(payload.canEdit);
            },
            buf -> new GenomeDataPayload(buf.readInt(), buf.readNbt(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}