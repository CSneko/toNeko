package org.cneko.toneko.common.mod.packets;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static org.cneko.toneko.common.Bootstrap.MODID;

/**
 * 服务端 → 客户端：猫娘 AI 回复的流式增量（打字机效果）。
 * 语义：finished=false → 增量文本块（chunk 可能为空串）；
 * finished=true 且 error=null → 成功收尾；finished=true 且 error!=null → 失败收尾（error 为展示文本）。
 */
public record ChatStreamPayload(String nekoUuid, String chunk, boolean finished, String error) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ChatStreamPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "chat_stream"));

    /** 可空字符串 codec（error 字段：null 表示成功收尾） */
    private static final StreamCodec<ByteBuf, String> NULLABLE_STRING = StreamCodec.of(
            (buf, v) -> {
                buf.writeBoolean(v != null);
                if (v != null) ByteBufCodecs.STRING_UTF8.encode(buf, v);
            },
            buf -> buf.readBoolean() ? ByteBufCodecs.STRING_UTF8.decode(buf) : null
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ChatStreamPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ChatStreamPayload::nekoUuid,
            ByteBufCodecs.STRING_UTF8, ChatStreamPayload::chunk,
            ByteBufCodecs.BOOL, ChatStreamPayload::finished,
            NULLABLE_STRING, ChatStreamPayload::error,
            ChatStreamPayload::new
    );
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
