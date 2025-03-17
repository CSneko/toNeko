package org.cneko.toneko.common.mod.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
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

import static org.cneko.toneko.common.mod.items.ToNekoItems.key;

public class NekoCollectorItem extends Item {
    public static String ID = "neko_collector";
    public static CountCodecs.FloatCountCodec DEFAULT_NEKO_PROGRESS_COMPONENT = new CountCodecs.FloatCountCodec(0.0f, 5000.0f);
    public NekoCollectorItem() {
        super(new Properties().stacksTo(1).component(ToNekoComponents.NEKO_PROGRESS_COMPONENT, DEFAULT_NEKO_PROGRESS_COMPONENT).setId(key(ID)));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        float count = stack.getOrDefault(ToNekoComponents.NEKO_PROGRESS_COMPONENT, DEFAULT_NEKO_PROGRESS_COMPONENT).getCount();
        float maxCount = stack.getOrDefault(ToNekoComponents.NEKO_PROGRESS_COMPONENT, DEFAULT_NEKO_PROGRESS_COMPONENT).getMaxCount();
        tooltip.add(Component.translatable("item.toneko.neko_collector.info", count, maxCount).withStyle(ChatFormatting.GREEN));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        if (! (entity instanceof Player player) ||(world.isClientSide())) return;
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
            if (entity.level() instanceof ServerLevel sl) {
                entity.spawnAtLocation(sl, ToNekoItems.NEKO_POTION);
            }
        }else {
            stack.set(ToNekoComponents.NEKO_PROGRESS_COMPONENT, new CountCodecs.FloatCountCodec(count, maxCount));
        }


    }
}
