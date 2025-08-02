package org.cneko.toneko.common.mod.entities.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.cneko.toneko.common.mod.api.EntityPoseManager;
import org.cneko.toneko.common.mod.entities.NekoEntity;

import java.util.EnumSet;
import java.util.Optional;

public class NekoSleepInBedGoal extends Goal {
    private final NekoEntity neko;
    private final Level level;
    private final PathNavigation navigation;
    private BlockPos targetBedPos;
    private int cooldown;

    public NekoSleepInBedGoal(NekoEntity neko) {
        this.neko = neko;
        this.level = neko.level();
        this.navigation = neko.getNavigation();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // 冷却时间检查
        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        // 检查是否满足睡觉条件
        if (!shouldSleep()) return false;

        // 寻找附近的床
        Optional<BlockPos> bedPos = findNearbyBed();
        if (bedPos.isEmpty()) return false;

        this.targetBedPos = bedPos.get();
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        // 如果已经到达床边，保持睡眠状态直到天亮
        return isSleeping() && shouldContinueSleeping();
    }

    @Override
    public void start() {
        // 移动到床边
        this.navigation.moveTo(
                this.navigation.createPath(this.targetBedPos, 1),
                1.2
        );
        this.neko.getLookControl().setLookAt(
                this.targetBedPos.getX(),
                this.targetBedPos.getY(),
                this.targetBedPos.getZ()
        );
    }

    @Override
    public void tick() {
        // 检查是否到达床边
        if (this.neko.distanceToSqr(
                this.targetBedPos.getX(),
                this.targetBedPos.getY(),
                this.targetBedPos.getZ()) < 2.5
        ) {
            startSleeping();
        }
    }

    @Override
    public void stop() {
        // 离开床铺
        if (isSleeping()) {
            stopSleeping();
        }
        this.targetBedPos = null;
        this.navigation.stop();
        this.cooldown = 200; // 设置10秒冷却时间（200 ticks）
    }

    private boolean shouldSleep() {
        // 检查是否晚上且不在水中
        return isNightTime() &&
                !this.neko.isInWater() &&
                !this.neko.isSitting();
    }

    private boolean shouldContinueSleeping() {
        // 检查是否仍是夜晚
        return isNightTime() ||
                this.level.isThundering() ||
                this.level.isRaining();
    }

    private boolean isNightTime() {
        long time = this.level.getDayTime() % 24000;
        return time > 12500 && time < 23000; // 夜晚时间段
    }

    private Optional<BlockPos> findNearbyBed() {
        // 在16格范围内寻找床
        BlockPos currentPos = this.neko.blockPosition();
        int range = 16;

        for (int x = -range; x <= range; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = currentPos.offset(x, y, z);
                    BlockState state = this.level.getBlockState(pos);

                    if (state.getBlock() instanceof BedBlock &&
                            this.level.isEmptyBlock(pos.above())) { // 检查床上方有空间
                        return Optional.of(pos);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private void startSleeping() {
        EntityPoseManager.setPose(this.neko, Pose.SLEEPING);
        this.neko.setSleepingPos(this.targetBedPos);
        this.neko.getNavigation().stop();
    }

    private void stopSleeping() {
        EntityPoseManager.remove(this.neko);
        this.neko.setSleepingPos(null);
    }

    private boolean isSleeping() {
        return this.neko.getPose() == Pose.SLEEPING;
    }
}