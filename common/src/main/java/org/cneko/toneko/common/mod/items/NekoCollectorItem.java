package org.cneko.toneko.common.mod.items;
import org.cneko.toneko.common.mod.util.NekoIds;
import org.jetbrains.annotations.NotNull;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.cneko.toneko.common.mod.codecs.CountCodecs;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.misc.ToNekoComponents;
import org.cneko.toneko.common.mod.misc.ToNekoAttributes;

import java.util.List;

public class NekoCollectorItem extends Item {
    public static String ID = "neko_collector";
    public static CountCodecs.FloatCountCodec DEFAULT_NEKO_PROGRESS_COMPONENT = new CountCodecs.FloatCountCodec(0.0f, 1000.0f);
    public NekoCollectorItem() {
        super(NekoIds.itemProps(ID).stacksTo(1).component(ToNekoComponents.NEKO_PROGRESS_COMPONENT, DEFAULT_NEKO_PROGRESS_COMPONENT));
    }

        @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull net.minecraft.world.item.component.TooltipDisplay display, @NotNull java.util.function.Consumer<Component> adder, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, display, adder, tooltipFlag);
        // 兼容旧实现：先收集到 List，再逐条发送
        java.util.List<Component> tooltips = new java.util.ArrayList<>();

        float count = stack.getOrDefault(ToNekoComponents.NEKO_PROGRESS_COMPONENT, DEFAULT_NEKO_PROGRESS_COMPONENT).getCount();
        float maxCount = stack.getOrDefault(ToNekoComponents.NEKO_PROGRESS_COMPONENT, DEFAULT_NEKO_PROGRESS_COMPONENT).getMaxCount();
        tooltips.add(Component.translatable("item.toneko.neko_collector.info", count, maxCount).withStyle(ChatFormatting.GREEN));
    
        for (Component t : tooltips) adder.accept(t);
    }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel world, Entity entity, net.minecraft.world.entity.EquipmentSlot slot) {
        if (!(entity instanceof Player player) || ((INeko) player).isNeko()) return;
        // 获取玩家3格方块内的猫猫数量
        float radius = 3.0f;
        int catCount = 0;
        // 创建一个包围盒，它代表了以 centerEntity 为中心、半径为 radius 的区域
        AABB box = new AABB(entity.getX() - radius, entity.getY() - radius, entity.getZ() - radius,
                entity.getX() + radius, entity.getY() + radius, entity.getZ() + radius);
        List<Entity> entities = world.getEntities(entity, box);
        for (Entity entity1 : entities) {
            if (entity1 instanceof INeko) {
                catCount+= ((INeko) entity1).getNekoAbility();
            }
        }
        if (catCount==0) return;
        // 获取玩家的 属性附加值/100 + 1
        double neko_degree_addition = player.getAttributes().getValue(ToNekoAttributes.NEKO_DEGREE) / 100.0 + 1;
        // 原来的 count + 猫猫数量/100*neko_degree_addition
        float count = (float) (stack.getOrDefault(ToNekoComponents.NEKO_PROGRESS_COMPONENT, DEFAULT_NEKO_PROGRESS_COMPONENT).getCount() + catCount / 100.0f * neko_degree_addition);
        // 原来的maxCount
        float maxCount = stack.getOrDefault(ToNekoComponents.NEKO_PROGRESS_COMPONENT, DEFAULT_NEKO_PROGRESS_COMPONENT).getMaxCount();
        // 如果count >= maxCount，则清零并掉落一瓶猫娘药水
        if (count >= maxCount) {
            stack.set(ToNekoComponents.NEKO_PROGRESS_COMPONENT, new CountCodecs.FloatCountCodec(0.0f, maxCount));
            ((net.minecraft.world.entity.player.Player) entity).drop(new ItemStack(ToNekoItems.NEKO_POTION), false);
        }else {
            stack.set(ToNekoComponents.NEKO_PROGRESS_COMPONENT, new CountCodecs.FloatCountCodec(count, maxCount));
        }


    }
}
