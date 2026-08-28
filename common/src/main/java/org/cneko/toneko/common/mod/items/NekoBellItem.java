package org.cneko.toneko.common.mod.items;

import org.cneko.toneko.common.mod.util.NekoIds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
        super(NekoIds.itemProps(ID).stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public @NotNull InteractionResult use(@NotNull Level level, @NotNull Player player,
                                                            @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        // 获取范围内所有猫娘，筛选属于该玩家的 NekoEntity
        List<INeko> nearby = EntityUtil.getNekoInRange(player, level, RECALL_RANGE);
        List<NekoEntity> owned = new java.util.ArrayList<>();
        for (INeko neko : nearby) {
            if (neko instanceof NekoEntity nekoEntity && nekoEntity.hasOwner(player.getUUID())) {
                owned.add(nekoEntity);
            }
        }

        if (owned.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("item.toneko.neko_bell.no_nekos"));
            return InteractionResult.FAIL;
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
        if (((INeko) player).isNeko()) {
            float cost = ENERGY_PER_NEKO * count;
            ((INeko) player).setNekoEnergy(Math.max(0, ((INeko) player).getNekoEnergy() - cost));
        }

        // 冷却
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);

        // 反馈
        player.sendOverlayMessage(Component.translatable("item.toneko.neko_bell.recall", count));

        return InteractionResult.SUCCESS;
    }

        @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull net.minecraft.world.item.component.TooltipDisplay display, @NotNull java.util.function.Consumer<Component> adder, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, display, adder, tooltipFlag);
        // 兼容旧实现：先收集到 List，再逐条发送
        java.util.List<Component> tooltips = new java.util.ArrayList<>();
        tooltips.add(Component.translatable("item.toneko.neko_bell.tip"));
    
        for (Component t : tooltips) adder.accept(t);
    }
}
