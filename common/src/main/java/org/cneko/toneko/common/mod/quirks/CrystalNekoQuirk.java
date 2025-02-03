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

import static org.cneko.toneko.common.mod.util.TextUtil.randomTranslatabledComponent;
public class CrystalNekoQuirk extends ToNekoQuirk{
    public static final String ID = "crystal_neko";
    public CrystalNekoQuirk() {
        super(ID);
    }

    @Nullable
    @Override
    public Component getTooltip() {
        return Component.translatable("quirk.toneko.crystal_neko.des");
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
                nekoPlayer.displayClientMessage(randomTranslatabledComponent("quirk.toneko.crystal_neko.damage.critical",5), true);
                return;
            }
            if (amount < 3) {
                nekoPlayer.displayClientMessage(randomTranslatabledComponent("quirk.toneko.crystal_neko.damage.low",5), true);
            } else if (amount < 6) {
                nekoPlayer.displayClientMessage(randomTranslatabledComponent("quirk.toneko.crystal_neko.damage.medium",5), true);
            } else if (amount >= 6) {
                nekoPlayer.displayClientMessage(randomTranslatabledComponent("quirk.toneko.crystal_neko.damage.high",5), true);
            }
        }
    }

    @Override
    public void onJoin(INeko neko) {
        super.onJoin(neko);
        if (neko instanceof Player nekoPlayer) {
            nekoPlayer.displayClientMessage(randomTranslatabledComponent("quirk.toneko.crystal_neko.join",5), true);
        }
    }

    @Override
    public InteractionResult onNekoAttack(INeko neko, Level level, InteractionHand interactionHand, LivingEntity entity, EntityHitResult entityHitResult) {
        super.onNekoAttack(neko, level, interactionHand, entity, entityHitResult);
        if (neko instanceof Player nekoPlayer) {
            float ratio = entity.getHealth() / entity.getMaxHealth(); // 比率
            if (ratio > 0.8) {
                nekoPlayer.displayClientMessage(randomTranslatabledComponent("quirk.toneko.crystal_neko.attack.high",5), true);
            } else if (ratio > 0.5) {
                nekoPlayer.displayClientMessage(randomTranslatabledComponent("quirk.toneko.crystal_neko.attack.medium",5), true);
            } else if (ratio > 0.2) {
                nekoPlayer.displayClientMessage(randomTranslatabledComponent("quirk.toneko.crystal_neko.attack.low",5), true);
            } else if (ratio <= 0.2) {
                nekoPlayer.displayClientMessage(randomTranslatabledComponent("quirk.toneko.crystal_neko.attack.critical",5), true);
            } else if (entity.getHealth() <= 0) {
                nekoPlayer.displayClientMessage(randomTranslatabledComponent("quirk.toneko.crystal_neko.attack.dead",5), true);
            }
        }
        return InteractionResult.PASS;
    }
}
