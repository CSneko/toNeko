package org.cneko.toneko.common.mod.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

/**
 * 腿部服饰工作台滑杆调节（C2S）。
 * 服务端校验菜单类型 + 槽位物品 + clamp 后写组件，幂等免费，无需 containerId/防重放。
 */
public record LegwearAdjustPayload(int denier, float length) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LegwearAdjustPayload> ID =
            new CustomPacketPayload.Type<>(toNekoLoc("legwear_adjust"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LegwearAdjustPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LegwearAdjustPayload::denier,
            ByteBufCodecs.FLOAT, LegwearAdjustPayload::length,
            LegwearAdjustPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
