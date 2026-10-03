package org.cneko.toneko.common.mod.client.api;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 客户端本地姿势钉定状态（仅对「本人玩家」生效）。
 *
 * 服务端只在钉定/解除的瞬间发一次 EntityPosePayload("self")，
 * 客户端据此在本地做与服务端一致的姿势重申（见 LivingEntityMixin），
 * 保证第一人称预测与服务端一致；其它玩家的姿势完全由原版 DATA_POSE 同步。
 *
 * 注意：本类不得引用任何 net.minecraft.client 类，否则公共侧无法编译。
 */
public final class ClientPoseState {
    /** 是否处于钉定状态。 */
    public static boolean active = false;
    /** 钉定的姿势。 */
    public static Pose pose = Pose.STANDING;
    /** 钉定归属（接收 "self" 包时的本人 UUID），用于实体匹配。 */
    public static @Nullable UUID ownerUuid = null;

    private ClientPoseState() {}

    public static void activate(UUID owner, Pose pinned) {
        active = true;
        ownerUuid = owner;
        pose = pinned;
    }

    public static void deactivate() {
        active = false;
        ownerUuid = null;
    }

    /** 若该实体是本人的钉定姿势，返回钉定姿势，否则返回 null。 */
    public static @Nullable Pose applyTo(Entity entity) {
        return active && ownerUuid != null && ownerUuid.equals(entity.getUUID()) ? pose : null;
    }
}
