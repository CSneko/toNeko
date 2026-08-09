package org.cneko.toneko.common.mod.packets;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static org.cneko.toneko.common.Bootstrap.MODID;

/**
 * 服务端 → 客户端：AI 回复显示消息。
 * text 为 {@link org.cneko.toneko.common.mod.misc.Messaging#format} 格式化后的
 * 带 § 码字符串；客户端按客户端配置选择聊天栏显示或猫娘头顶气泡显示。
 */
public record NekoChatDisplayPayload(String nekoUuid, String text) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NekoChatDisplayPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "neko_chat_display"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NekoChatDisplayPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, NekoChatDisplayPayload::nekoUuid,
            ByteBufCodecs.STRING_UTF8, NekoChatDisplayPayload::text,
            NekoChatDisplayPayload::new
    );
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
