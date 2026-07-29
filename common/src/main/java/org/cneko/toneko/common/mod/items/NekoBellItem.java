package org.cneko.toneko.common.mod.items;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NekoBellItem extends Item {
    public static final String ID = "neko_bell";
    public static final float RECALL_RANGE = 64.0f;
    public static final int COOLDOWN_TICKS = 600;  // 30s
    public static final int INVULNERABLE_TICKS = 60; // 3s
    public static final float ENERGY_PER_NEKO = 10.0f;

    public NekoBellItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                            @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        // 获取范围内所有猫娘，筛选属于该玩家的 NekoEntity
        List<INeko> nearby = EntityUtil.getNekoInRange(player, level, RECALL_RANGE);
        List<NekoEntity> owned = new java.util.ArrayList<>();
        for (INeko neko : nearby) {
            if (neko instanceof NekoEntity nekoEntity && nekoEntity.hasOwner(player.getUUID())) {
                owned.add(nekoEntity);
            }
        }

        if (owned.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("item.toneko.neko_bell.no_nekos"), true);
            return InteractionResultHolder.fail(stack);
        }

        // 召回每只猫娘
        int count = 0;
        for (NekoEntity neko : owned) {
            // 战斗脱战
            if (neko.getTarget() != null) {
                neko.setTarget(null);
            }
            // 传送
            neko.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            neko.invulnerableTime = INVULNERABLE_TICKS;
            // 音符粒子
            ((ServerLevel) level).sendParticles(ParticleTypes.NOTE,
                    neko.getX(), neko.getY() + 1.5, neko.getZ(),
                    5, 0.3, 0.2, 0.3, 0);
            count++;
        }

        // 铃铛音效
        level.playSound(null, player.blockPosition(),
                SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.8F, 1.2F);

        // 能量消耗
        if (player.isNeko()) {
            float cost = ENERGY_PER_NEKO * count;
            player.setNekoEnergy(Math.max(0, player.getNekoEnergy() - cost));
        }

        // 冷却
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

        // 反馈
        player.displayClientMessage(
                Component.translatable("item.toneko.neko_bell.recall", count), true);

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.toneko.neko_bell.tip"));
    }
}
