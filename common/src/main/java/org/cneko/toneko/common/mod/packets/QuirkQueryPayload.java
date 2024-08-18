package org.cneko.toneko.common.mod.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static org.cneko.toneko.common.Bootstrap.MODID;

public record QuirkQueryPayload(List<String> quirks,List<String> allQuirks,boolean openScreen) implements CustomPacketPayload{
    public static final CustomPacketPayload.Type<QuirkQueryPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "quirk_query"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuirkQueryPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), QuirkQueryPayload::getQuirks,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), QuirkQueryPayload::getAllQuirks,
            ByteBufCodecs.BOOL, QuirkQueryPayload::isOpenScreen,
            QuirkQueryPayload::new
    );

    public List<String> getQuirks() {
        return quirks;
    }
    public List<String> getAllQuirks() {
        return allQuirks;
    }
    public boolean isOpenScreen() {
        return openScreen;
    }


    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
