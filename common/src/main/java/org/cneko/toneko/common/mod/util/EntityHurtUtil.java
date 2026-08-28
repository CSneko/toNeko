package org.cneko.toneko.common.mod.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * 26.x：LivingEntity.hurt(DamageSource,float) 被移除，
 * 服务端受伤统一走 hurtServer(ServerLevel, DamageSource, float)。
 */
public final class EntityHurtUtil {
    private EntityHurtUtil() {}

    public static boolean hurt(LivingEntity target, DamageSource source, float amount) {
        if (target == null) return false;
        if (!(target.level() instanceof ServerLevel serverLevel)) return false;
        return target.hurtServer(serverLevel, source, amount);
    }
}
