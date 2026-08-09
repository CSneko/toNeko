package org.cneko.toneko.common.mod.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.LevelEntityGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 暴露 ClientLevel 的实体表（Level#getEntities 为 protected），
 * 用于按 UUID 直接哈希查找实体，避免 AABB 范围扫描。
 */
@Mixin(ClientLevel.class)
public interface ClientLevelAccessor {
    @Invoker("getEntities")
    LevelEntityGetter<Entity> invokeGetEntities();
}
