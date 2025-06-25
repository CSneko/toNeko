package org.cneko.toneko.common.mod.entities.boss.mouflet;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

public class MoufletFlyOutOfWaterGoal extends Goal {
    private final MoufletNekoBoss neko;
    private BlockPos targetPos;
    private final int searchRange = 16; // 搜索范围

    public MoufletFlyOutOfWaterGoal(MoufletNekoBoss neko) {
        this.neko = neko;
    }

    @Override
    public boolean canUse() {
        // 如果能量不足或已离开水中则停止
        return !(neko.getNekoEnergy() <= 0) && neko.isInWater();
    }

    @Override
    public void start() {
        // 寻找最近的陆地位置
        targetPos = findNearestLand();
        // 暂时关掉重力
        neko.setNoGravity(true);
    }

    @Override
    public void tick() {
        // 当前距离水面的高度
        double waterHeight = neko.level().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, neko.blockPosition()).getY();
        // 如果当前高度小于水面高度+3则上升
        if (neko.getY() < waterHeight) {
            neko.setDeltaMovement(neko.getDeltaMovement().add(0, 0.1, 0));
        }
        // 开始向目标位置飞行
        if (targetPos == null){
            // 为null时始终向x轴正方向飞行
            neko.setDeltaMovement(neko.getDeltaMovement().add(0.3, 0, 0));
            // 如果目标位置无效则重新搜索
            targetPos = findNearestLand();
        }else {
            // 向目标位置飞行
            double dx = targetPos.getX() - neko.getX();
            double dz = targetPos.getZ() - neko.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            // 判断y轴高度差
            double dy = targetPos.getY() - neko.getY();
            double yMovement = 0;
            // 如果y轴高度差小于0则上升
            if (dy < 0) {
                yMovement = 0.3; // 上升0.3
            }
            if (distance > 0) {
                neko.setDeltaMovement(neko.getDeltaMovement().add(dx / distance * 0.4, yMovement, dz / distance * 0.4));
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        // 能量不足或已经到达了目标则停止（x，z相差小于0.5，y在上面）
        if (neko.getNekoEnergy() <= 0) {
            return false;
        }
        if (neko.isInWater()){
            return true;
        }
        if (targetPos != null) {
            double dx = targetPos.getX() - neko.getX();
            double dz = targetPos.getZ() - neko.getZ();
            double dy = targetPos.getY() - neko.getY();
            return !(Math.abs(dx) > 0.5 || Math.abs(dz) > 0.5 || dy < 0);
        }else {
            return true; // 如果目标位置无效则继续飞行
        }
    }

    @Override
    public void stop() {
        targetPos = null; // 停止时清除目标位置
        // 恢复重力
        neko.setNoGravity(false);
    }

    // 寻找最近的陆地位置
    private BlockPos findNearestLand() {
        BlockPos entityPos = neko.blockPosition();
        Level level = neko.level();

        // 从内向外搜索陆地
        for (int r = 0; r <= searchRange; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    // 只检查最外层
                    if (Math.abs(x) != r && Math.abs(z) != r) continue;

                    BlockPos testPos = entityPos.offset(x, 0, z);
                    // 获取地表高度
                    BlockPos surfacePos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, testPos);

                    // 检查该位置是否非水（陆地）
                    if (!level.getFluidState(surfacePos.above()).is(FluidTags.WATER)) {
                        return surfacePos.above();
                    }
                }
            }
        }
        return null; // 未找到陆地
    }
}