package org.cneko.toneko.common.mod.quirks;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.cneko.toneko.common.mod.entities.INeko;
import org.jetbrains.annotations.Nullable;

public class ZakoQuirk extends ToNekoQuirk{
    public static final String id = "zako";
    public ZakoQuirk() {
        super(id);
    }

    @Nullable
    @Override
    public Component getTooltip() {
        return Component.translatable("quirk.toneko.zako.des");
    }

    @Override
    public int getInteractionValue() {
        return 1;
    }

    @Override
    public void onDamage(INeko neko, DamageSource damageSource, float amount) {
        super.onDamage(neko, damageSource, amount);
        if (neko instanceof Player nekoPlayer) {
            if (nekoPlayer.getHealth() <= 4) {
                nekoPlayer.displayClientMessage(Component.translatable("quirk.toneko.zako.damage.critical"), true);
                return;
            }
            if (amount < 3) {
                nekoPlayer.displayClientMessage(Component.translatable("quirk.toneko.zako.damage.low"), true);
            } else if (amount < 6) {
                nekoPlayer.displayClientMessage(Component.translatable("quirk.toneko.zako.damage.medium"), true);
            } else if (amount >= 6) {
                nekoPlayer.displayClientMessage(Component.translatable("quirk.toneko.zako.damage.high"), true);
            }
        }
    }

    @Override
    public void onJoin(INeko neko) {
        super.onJoin(neko);
        if (neko instanceof Player nekoPlayer) {
            nekoPlayer.displayClientMessage(Component.translatable("quirk.toneko.zako.join"), true);
        }
    }

    @Override
    public InteractionResult onNekoAttack(INeko neko, Level level, InteractionHand interactionHand, LivingEntity entity, EntityHitResult entityHitResult) {
        super.onNekoAttack(neko, level, interactionHand, entity, entityHitResult);
        if (neko instanceof Player nekoPlayer) {
            float ratio = entity.getHealth() / entity.getMaxHealth(); // 比率
            if (ratio > 0.8) {
                nekoPlayer.displayClientMessage(Component.translatable("quirk.toneko.zako.attack.high"), true);
            } else if (ratio > 0.5) {
                nekoPlayer.displayClientMessage(Component.translatable("quirk.toneko.zako.attack.medium"), true);
            } else if (ratio > 0.2) {
                nekoPlayer.displayClientMessage(Component.translatable("quirk.toneko.zako.attack.low"), true);
            } else if (ratio <= 0.2) {
                nekoPlayer.displayClientMessage(Component.translatable("quirk.toneko.zako.attack.critical"), true);
            } else if (entity.getHealth() <= 0) {
                nekoPlayer.displayClientMessage(Component.translatable("quirk.toneko.zako.attack.dead"), true);
            }
        }
        return InteractionResult.PASS;
    }
}
