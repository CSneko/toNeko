package org.cneko.toneko.common.mod.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.common.Bootstrap.MODID;

/**
 * 客户端→服务端：猫爪爬墙状态更新。
 * @param active       是否正在爬墙（R 键按住）
 * @param verticalInput 垂直方向输入：>0 向上爬，<0 向下移动，0 悬挂
 */
public record ClimbWallPayload(boolean active, float verticalInput) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClimbWallPayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "climb_wall"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClimbWallPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, ClimbWallPayload::active,
                    ByteBufCodecs.FLOAT, ClimbWallPayload::verticalInput,
                    ClimbWallPayload::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
