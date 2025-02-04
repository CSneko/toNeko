package org.cneko.toneko.common.mod.quirks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
                nekoPlayer.displayClientMessage(randomTranslatabledComponent("quirk.toneko.zako.damage.critical",5), true);
                return;
            }
            if (amount < 3) {
                nekoPlayer.displayClientMessage(randomTranslatabledComponent("quirk.toneko.zako.damage.low",5), true);
            } else if (amount < 6) {
                nekoPlayer.displayClientMessage(randomTranslatabledComponent("quirk.toneko.zako.damage.medium",5), true);
            } else if (amount >= 6) {
                nekoPlayer.displayClientMessage(randomTranslatabledComponent("quirk.toneko.zako.damage.high",5), true);
            }
        }
    }

    @Override
    public void onJoin(INeko neko) {
        super.onJoin(neko);
        if (neko instanceof Player nekoPlayer) {
            nekoPlayer.displayClientMessage(randomTranslatabledComponent("quirk.toneko.zako.join",5), true);
        }
    }

    @Override
    public InteractionResult onNekoAttack(INeko neko, Level level, InteractionHand interactionHand, LivingEntity entity, EntityHitResult entityHitResult) {
        super.onNekoAttack(neko, level, interactionHand, entity, entityHitResult);
        if (neko instanceof Player nekoPlayer) {
            float ratio = entity.getHealth() / entity.getMaxHealth(); // 比率
            if (ratio > 0.8) {
                nekoPlayer.displayClientMessage(randomTranslatabledComponent("quirk.toneko.zako.attack.high",5), true);
            } else if (ratio > 0.5) {
                nekoPlayer.displayClientMessage(randomTranslatabledComponent("quirk.toneko.zako.attack.medium",5), true);
            } else if (ratio > 0.2) {
                nekoPlayer.displayClientMessage(randomTranslatabledComponent("quirk.toneko.zako.attack.low",5), true);
            } else if (ratio <= 0.2) {
                nekoPlayer.displayClientMessage(randomTranslatabledComponent("quirk.toneko.zako.attack.critical",5), true);
            } else if (entity.getHealth() <= 0) {
                nekoPlayer.displayClientMessage(randomTranslatabledComponent("quirk.toneko.zako.attack.dead",5), true);
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onWeatherChange(INeko neko,ServerLevel serverLevel, int clearTime, int weatherTime, boolean isRaining, boolean isThundering) {
        super.onWeatherChange(neko,serverLevel, clearTime, weatherTime, isRaining, isThundering);
        if (neko instanceof Player player) {
            if (isRaining) {
                player.displayClientMessage(randomTranslatabledComponent("quirk.toneko.zako.weather.rain",5), true);
            } else if (isThundering) {
                player.displayClientMessage(randomTranslatabledComponent("quirk.toneko.zako.weather.thunder",5), true);
            } else {
                player.displayClientMessage(randomTranslatabledComponent("quirk.toneko.zako.weather.sunny",5), true);
            }
        }
    }

    @Override
    public void startSleep(INeko neko, BlockPos pos) {
        super.startSleep(neko, pos);
        if (neko instanceof Player player) {
            player.displayClientMessage(randomTranslatabledComponent("quirk.toneko.zako.sleep.start",5), true);
        }
    }

    @Override
    public void stopSleep(INeko neko, BlockPos pos) {
        super.stopSleep(neko, pos);
        if (neko instanceof Player player) {
            int daytime = (int) (player.level().getDayTime() % 24000);
            if (daytime>=0&&daytime<=12000){
                player.displayClientMessage(randomTranslatabledComponent("quirk.toneko.zako.sleep.stop.day",5), true);
            }else {
                player.displayClientMessage(randomTranslatabledComponent("quirk.toneko.zako.sleep.stop.night",5), true);
            }
        }
    }
}
