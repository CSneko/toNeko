package org.cneko.toneko.common.mod.abilities;

import java.util.UUID;

/**
 * 扑跃的客户端预测状态（只跟踪本地玩家）。
 *
 * 服务端的权威状态在 {@link PounceHandler} 的 {@code AIRBORNE} 表里；这里只负责
 * 「本人客户端」在起跳后到落地前的姿势预测。放在 common 是为了让 Mixin 能不区分
 * 平台地读取，且本类不引用任何 {@code net.minecraft.client} 类型。
 */
public final class PounceClientState {
    private static volatile boolean pouncing = false;
    private static volatile UUID owner = null;

    private PounceClientState() {
    }

    public static void start(UUID uuid) {
        owner = uuid;
        pouncing = true;
    }

    public static void stop() {
        pouncing = false;
        owner = null;
    }

    public static boolean isPouncing(UUID uuid) {
        return pouncing && uuid != null && uuid.equals(owner);
    }
}
