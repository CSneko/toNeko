package org.cneko.toneko.common.mod.packets.interactives;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static org.cneko.toneko.common.Bootstrap.MODID;

/**
 * 客户端 → 服务端：向猫娘要一份日记成品。
 */
public record NekoDiaryRequestPayload(String uuid) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NekoDiaryRequestPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "neko_diary_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NekoDiaryRequestPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, NekoDiaryRequestPayload::uuid,
            NekoDiaryRequestPayload::new
    );
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
