package org.cneko.toneko.common.mod.items.ammo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.cneko.toneko.common.mod.blocks.ToNekoBlocks;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.items.BazookaItem;

public class NekoEnergyBombItem extends AmmoItem {
    public NekoEnergyBombItem() {
        super(new Properties());
    }

    @Override
    public void hitOnEntity(LivingEntity shooter, LivingEntity target, ItemStack bazooka, ItemStack ammunition) {
        float damage = BazookaItem.getAttackDamage(bazooka, ammunition, shooter.level());
        if (target instanceof INeko neko && neko.isNeko()) {
            // 对方是猫娘的话，为对方恢复生命和能量
            target.heal(damage * 0.5f);
            neko.setNekoEnergy(neko.getNekoEnergy() + damage * 2);

            // 给被治疗的猫娘一点爱心特效
            if (target.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HEART, target.getX(), target.getEyeY(), target.getZ(), 3, 0.5, 0.5, 0.5, 0.1);
            }
        } else {
            // 造成伤害
            target.hurt(BazookaItem.getDamageSource(shooter), damage);
        }
    }

    @Override
    public void hitOnBlock(LivingEntity shooter, BlockPos pos, ItemStack bazooka, ItemStack ammunition) {
        Level level = shooter.level();

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            // 定义搜索半径
            int radius = 4;
            // 垂直搜索范围稍微小一点，因为作物通常在同一平面
            int yRadius = 1;

            boolean anyConverted = false;

            for (int x = -radius; x <= radius; x++) {
                for (int y = -yRadius; y <= yRadius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos targetPos = pos.offset(x, y, z);
                        BlockState state = level.getBlockState(targetPos);

                        // 检查是否为作物 (使用Tags兼容性最好，包括小麦、胡萝卜、土豆等)
                        if (state.is(BlockTags.CROPS)) {
                            // 替换为猫薄荷
                            level.setBlock(targetPos, ToNekoBlocks.CATNIP.defaultBlockState(), 3);

                            // 在被转换的作物位置播放少量绿色粒子
                            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                                    targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                                    3, 0.3, 0.3, 0.3, 0.1);

                            anyConverted = true;
                        }
                    }
                }
            }

            // 中心位置的基础撞击特效 (少量烟雾或绿色粒子)
            serverLevel.sendParticles(ParticleTypes.COMPOSTER,
                    pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                    10, 0.5, 0.5, 0.5, 0.1);

            // 如果成功转换了作物，中心再稍微爆发一点特效
            if (anyConverted) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                        15, 1.0, 0.5, 1.0, 0.1);
            }
        }
    }

    @Override
    public void hitOnAir(LivingEntity shooter, BlockPos pos, ItemStack bazooka, ItemStack ammunition) {
        Level level = shooter.level();

        // 空气中引爆：纯特效展示
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            // 1. HAPPY_VILLAGER (绿色星星，类似骨粉效果) - 量大，范围广
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    40, 1.5, 1.5, 1.5, 0.1); // count=40, delta(xyz)=1.5

            // 2. 辅助特效：COMPOSTER (堆肥桶绿色粒子) 增加质感
            serverLevel.sendParticles(ParticleTypes.COMPOSTER,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    20, 0.8, 0.8, 0.8, 0.1);

            // 3. 辅助特效：WAX_OFF (白色/淡色粒子) 模拟能量消散
            serverLevel.sendParticles(ParticleTypes.WAX_OFF,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    10, 0.5, 0.5, 0.5, 0.1);
        }
    }
}