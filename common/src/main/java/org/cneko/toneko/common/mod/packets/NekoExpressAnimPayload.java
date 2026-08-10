package org.cneko.toneko.common.mod.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static org.cneko.toneko.common.Bootstrap.MODID;

/**
 * 猫娘表现动画触发包（S2C）：服务端动作/触发反应播放一次性动画时，
 * 把实体 id + 动画名发给附近客户端，由客户端 GeckoLib triggerAnim 播放。
 */
public record NekoExpressAnimPayload(int entityId, String animName) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NekoExpressAnimPayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "neko_express_anim"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NekoExpressAnimPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, NekoExpressAnimPayload::entityId,
            ByteBufCodecs.STRING_UTF8, NekoExpressAnimPayload::animName,
            NekoExpressAnimPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
