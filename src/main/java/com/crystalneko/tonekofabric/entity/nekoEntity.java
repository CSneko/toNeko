package com.crystalneko.tonekofabric.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class nekoEntity extends AnimalEntity {
    private double targetX;
    private double targetY;
    private double targetZ;
    public nekoEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return this;
    }
    @Override
    public void tickMovement() {
        super.tickMovement();

        if (this.isAlive()) {
            // 如果实体没有移动目标，则随机选择一个新的目标位置
            if (this.getMoveTarget() == null) {
                int targetX = MathHelper.floor(this.getX() + this.getRandom().nextInt(16) - 8);
                int targetY = MathHelper.floor(this.getY());
                int targetZ = MathHelper.floor(this.getZ() + this.getRandom().nextInt(16) - 8);
                this.setMoveTarget(new BlockPos(targetX, targetY, targetZ));
            }

            // 计算实体到移动目标位置的距离
            double dx = this.getMoveTarget().getX() + 0.5 - this.getX();
            double dy = this.getMoveTarget().getY() + 0.5 - this.getY();
            double dz = this.getMoveTarget().getZ() + 0.5 - this.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            // 如果已经到达移动目标位置，则重新选择一个新的目标位置
            if (distance < 1.0) {
                this.setMoveTarget(null);
            } else {
                // 向目标位置移动
                double speed = 0.15;

                this.setVelocity(dx / distance * speed, dy / distance * speed, dz / distance * speed);
            }
        }
    }

    private void setMoveTarget(BlockPos pos) {
        if (pos == null) {
            this.targetX = Double.NaN;
            this.targetY = Double.NaN;
            this.targetZ = Double.NaN;
        } else {
            this.targetX = pos.getX() + 0.5;
            this.targetY = pos.getY() + 0.5;
            this.targetZ = pos.getZ() + 0.5;
        }
    }

    private BlockPos getMoveTarget() {
        if (Double.isNaN(this.targetX) || Double.isNaN(this.targetY) || Double.isNaN(this.targetZ)) {
            return null; // 没有移动目标
        } else {
            return new BlockPos(MathHelper.floor(this.targetX), MathHelper.floor(this.targetY), MathHelper.floor(this.targetZ));
        }
    }



}
