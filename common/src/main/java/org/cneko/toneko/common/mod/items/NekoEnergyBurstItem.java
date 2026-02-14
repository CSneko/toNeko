package org.cneko.toneko.common.mod.items;

import lombok.Getter;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.misc.ToNekoDamageTypes;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NekoEnergyBurstItem extends Item {
    @Getter
    private final float damage;
    @Getter
    private final float radius;
    @Getter
    private final float energyCost;
    public NekoEnergyBurstItem(float damage, float radius, float energyCost) {
        super(new Properties().stacksTo(1).durability(200));
        this.damage = damage;
        this.radius = radius;
        this.energyCost = energyCost;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand usedHand) {
        if (level.isClientSide) return super.use(level, player, usedHand);
        if (!player.isNeko()){
            player.displayClientMessage(Component.translatable("item.toneko.neko_energy_burst.not_neko"),true);
            return InteractionResultHolder.fail(player.getItemInHand(usedHand));
        }
        if (player.getNekoEnergy() < energyCost){
            player.displayClientMessage(Component.translatable("item.toneko.neko_energy_burst.not_enough_energy"),true);
            return InteractionResultHolder.fail(player.getItemInHand(usedHand));
        }
        float finalDamage = damage + damage * player.getNekoLevel()*0.02f;
        float finalRadius = radius + player.getNekoLevel()*0.02f;
        player.setNekoEnergy(player.getNekoEnergy()-energyCost);
        // 扩散粒子喵~ 超炫酷的呢~
        spawnBurstParticles((ServerLevel) level, player, finalRadius);
        // 获取周围的实体并造成伤害
        player.level().getEntities(player, player.getBoundingBox().inflate(finalRadius), entity -> entity.isAlive() && entity != player)
                .forEach(entity -> {
                    if (entity instanceof LivingEntity livingEntity) {
                        if (entity instanceof INeko neko && neko.isNeko()) {
                            livingEntity.heal(finalDamage * 0.5f);
                            // 爱心粒子
                            for (int i = 0; i < 10; i++) {
                                double offsetX = (entity.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
                                double offsetY = entity.getRandom().nextDouble() * entity.getBbHeight();
                                double offsetZ = (entity.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
                                entity.level().addParticle(ParticleTypes.HEART, entity.getX() + offsetX, entity.getY() + offsetY, entity.getZ() + offsetZ, 0, 0.1, 0);
                            }
                        } else {
                            // 伤害
                            entity.hurt(ToNekoDamageTypes.nekoDamage(player), finalDamage);
                            // 受伤粒子
                            for (int i = 0; i < 10; i++) {
                                double offsetX = (entity.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
                                double offsetY = entity.getRandom().nextDouble() * entity.getBbHeight();
                                double offsetZ = (entity.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
                                entity.level().addParticle(ParticleTypes.DAMAGE_INDICATOR, entity.getX() + offsetX, entity.getY() + offsetY, entity.getZ() + offsetZ, 0, 0.1, 0);
                            }
                        }
                    }

                });
        // 消耗耐久
        player.getItemInHand(usedHand).hurtAndBreak(1, player,usedHand == InteractionHand.MAIN_HAND ?  EquipmentSlot.MAINHAND: EquipmentSlot.OFFHAND);
        // 设置冷却
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.success(player.getItemInHand(usedHand));
    }

    private void spawnBurstParticles(ServerLevel level, Player player, float radius) {
        int particleCount = 40; // 粒子的密度
        double px = player.getX();
        double py = player.getY() + 0.1; // 稍微高于地面
        double pz = player.getZ();

        for (int i = 0; i < particleCount; i++) {
            // 计算角度 (0 到 2π)
            double angle = (i * Math.PI * 2) / particleCount;
            double dx = Math.cos(angle) * radius;
            double dz = Math.sin(angle) * radius;

            // 末地烛粒子看起来很有能量感呢
            level.sendParticles(ParticleTypes.END_ROD,
                    px + dx, py, pz + dz,
                    1, 0, 0.05, 0, 0.01);

            // 第二圈
            level.sendParticles(ParticleTypes.WITCH,
                    px + dx * 0.5, py, pz + dz * 0.5,
                    1, 0, 0.1, 0, 0.02);
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.toneko.neko_energy_burst.tip"));
        tooltipComponents.add(Component.translatable("item.toneko.neko_energy_burst.tip.damage",damage));
        tooltipComponents.add(Component.translatable("item.toneko.neko_energy_burst.tip.radius",radius));
        tooltipComponents.add(Component.translatable("item.toneko.neko_energy_burst.tip.energy_cost",energyCost));
    }
}
