package org.cneko.toneko.fabric.items;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.cneko.toneko.fabric.codecs.CountCodecs;
import org.cneko.toneko.fabric.misc.ToNekoAttributes;
import org.cneko.toneko.fabric.misc.ToNekoComponents;

import java.util.List;

public class NekoCollectorItem extends Item {
    public static String ID = "neko_collector";
    public static CountCodecs.FloatCountCodec DEFAULT_NEKO_PROGRESS_COMPONENT = new CountCodecs.FloatCountCodec(0.0f, 5000.0f);
    public NekoCollectorItem() {
        super(new Settings().maxCount(1).component(ToNekoComponents.NEKO_PROGRESS_COMPONENT, DEFAULT_NEKO_PROGRESS_COMPONENT));
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        float count = stack.getOrDefault(ToNekoComponents.NEKO_PROGRESS_COMPONENT, DEFAULT_NEKO_PROGRESS_COMPONENT).getCount();
        float maxCount = stack.getOrDefault(ToNekoComponents.NEKO_PROGRESS_COMPONENT, DEFAULT_NEKO_PROGRESS_COMPONENT).getMaxCount();
        tooltip.add(Text.translatable("item.toneko.neko_collector.info", count, maxCount).formatted(Formatting.GREEN));
    }

    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (! (entity instanceof PlayerEntity player)) return;
        // 获取玩家3格方块内的猫猫数量
        float radius = 3.0f;
        int catCount = 0;
        // 创建一个包围盒，它代表了以 centerEntity 为中心、半径为 radius 的区域
        Box box = new Box(entity.getX() - radius, entity.getY() - radius, entity.getZ() - radius,
                entity.getX() + radius, entity.getY() + radius, entity.getZ() + radius);
        List<Entity> entities = world.getOtherEntities(entity, box);
        for (Entity entity1 : entities) {
            if (entity1 instanceof CatEntity) {
                catCount++;
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
            entity.dropItem(ToNekoItems.NEKO_POTION);
        }else {
            stack.set(ToNekoComponents.NEKO_PROGRESS_COMPONENT, new CountCodecs.FloatCountCodec(count, maxCount));
        }
    }
}
