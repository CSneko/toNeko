package org.cneko.toneko.common.mod.client.items;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import org.cneko.toneko.common.mod.items.NekoArmor;
import org.cneko.toneko.common.mod.misc.ToNekoEnchantments;
import org.cneko.toneko.common.mod.util.EnchantmentUtil;

import static org.cneko.toneko.common.Bootstrap.MODID;

/**
 * 猫娘套装（尾巴/耳朵/爪爪）的 GeckoLib 渲染器。
 *
 * <h2>GeckoLib 5.5 渲染说明</h2>
 * 直接继承 {@link GeoArmorRenderer}（避免 GeckoLib 5.5.2 {@code DyeableGeoArmorRenderer}
 * 丢弃 {@code pose} 及不按槽位裁剪的缺陷），通过 {@link #getRenderColor} 统一染色。
 */
public class NekoArmorRenderer<T extends NekoArmor<T>, S extends HumanoidRenderState & GeoRenderState> extends GeoArmorRenderer<T, S> {
    /** 当前渲染的物品堆（capture 阶段写入渲染状态） */
    public static final DataTicket<ItemStack> ITEM_STACK =
            DataTicket.create("toneko:item_stack", ItemStack.class);
    /** 当前穿戴者实体（capture 阶段写入渲染状态） */
    public static final DataTicket<Entity> WEARER_ENTITY =
            DataTicket.create("toneko:wearer_entity", Entity.class);

    public NekoArmorRenderer() {
        super(new DefaultedItemGeoModel<>(Identifier.fromNamespaceAndPath(MODID, "armor/neko_armor")));
    }

    @Override
    public void captureDefaultRenderState(T item, GeoArmorRenderer.RenderData data,
                                          S state, float partialTick) {
        super.captureDefaultRenderState(item, data, state, partialTick);
        // 把物品堆与穿戴者塞进渲染状态，供后续阶段读取
        state.addGeckolibData(ITEM_STACK, data.itemStack());
        state.addGeckolibData(WEARER_ENTITY, data.entity());
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<S> renderPassInfo) {
        super.adjustRenderPose(renderPassInfo);
        PoseStack poseStack = renderPassInfo.poseStack();
        ItemStack stack = renderPassInfo.renderState().getOrDefaultGeckolibData(ITEM_STACK, ItemStack.EMPTY);

        // 如果有反转附魔
        if (!stack.isEmpty() && EnchantmentUtil.hasEnchantment(ToNekoEnchantments.REVERSION.identifier(), stack)) {
            // 旋转180度
            poseStack.mulPose(Axis.XN.rotationDegrees(180));
            // 前进1/16个单位并下移动1单位
            poseStack.translate(0, -1.5, -0.0625);
        }
    }

    // === 染色 ===

    @Override
    public int getRenderColor(T animatable, GeoArmorRenderer.RenderData stackAndSlot, float partialTick) {
        ItemStack stack = stackAndSlot.itemStack();
        if (stack == null || stack.isEmpty() || !stack.has(DataComponents.DYED_COLOR)) {
            return 0xFFFFFFFF;
        }
        int rgb = DyedItemColor.getOrDefault(stack, 6463722);
        // 全不透明 ARGB
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    public static class NekoTailRenderer<S extends HumanoidRenderState & GeoRenderState> extends NekoArmorRenderer<NekoArmor.NekoTailItem, S> {
        public NekoTailRenderer() {
            super();
        }

        @Override
        public void adjustRenderPose(RenderPassInfo<S> renderPassInfo) {
            super.adjustRenderPose(renderPassInfo);
            Entity entity = renderPassInfo.renderState().getGeckolibData(WEARER_ENTITY);
            if (entity != null && entity.isShiftKeyDown()) {
                // 向后移动一点
                renderPassInfo.poseStack().translate(0, 0, 0.35);
            }
        }
    }

    public static class NekoEarsRenderer<S extends HumanoidRenderState & GeoRenderState> extends NekoArmorRenderer<NekoArmor.NekoEarsItem, S> {
        public NekoEarsRenderer() {
            super();
        }
    }

    public static class NekoPawsRenderer<S extends HumanoidRenderState & GeoRenderState> extends NekoArmorRenderer<NekoArmor.NekoPawsItem, S> {
        public NekoPawsRenderer() {
            super();
        }
    }
}
