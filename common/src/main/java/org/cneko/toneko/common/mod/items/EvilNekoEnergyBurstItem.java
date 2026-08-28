package org.cneko.toneko.common.mod.items;
import org.cneko.toneko.common.mod.util.NekoIds;
import net.minecraft.world.item.Item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.cneko.toneko.common.mod.entities.INeko;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Predicate;

/**
 * 邪恶猫能爆哈器 —— 反向版本，专门伤害猫娘（包括使用者自己）
 */
public class EvilNekoEnergyBurstItem extends NekoEnergyBurstItem {

    public EvilNekoEnergyBurstItem(String idPath, float damage, float radius, float energyCost) {
        super(idPath, damage, radius, energyCost);
    }

    @Override
    protected Predicate<Entity> getEntityFilter(Player player) {
        // 包括自己：使用者如果是猫娘也会受伤
        return Entity::isAlive;
    }

    @Override
    protected boolean isHostileTarget(LivingEntity entity) {
        // 只对猫娘造成伤害
        return entity instanceof INeko neko && neko.isNeko();
    }

    @Override
    protected boolean isFriendlyTarget(LivingEntity entity) {
        // 邪恶版不治疗任何人
        return false;
    }

    @Override
    protected boolean isEvil() {
        return true;
    }

    @Override
    protected String getBroadcastKeyPrefix() {
        return "item.toneko.evil_neko_energy_burst";
    }

        @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull net.minecraft.world.item.component.TooltipDisplay display, @NotNull java.util.function.Consumer<Component> adder, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, display, adder, tooltipFlag);
        // 兼容旧实现：先收集到 List，再逐条发送
        java.util.List<Component> tooltips = new java.util.ArrayList<>();

        tooltips.add(Component.translatable("item.toneko.evil_neko_energy_burst.tip"));
        tooltips.add(Component.translatable("item.toneko.evil_neko_energy_burst.tip.damage", getDamage()));
        tooltips.add(Component.translatable("item.toneko.evil_neko_energy_burst.tip.radius", getRadius()));
        tooltips.add(Component.translatable("item.toneko.evil_neko_energy_burst.tip.energy_cost", getEnergyCost()));
    
        for (Component t : tooltips) adder.accept(t);
    }
}
