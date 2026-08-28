package org.cneko.toneko.common.mod.client.items;

import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.items.LegwearItem;
import org.cneko.toneko.common.mod.misc.WetnessUtil;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;

import java.util.List;

import static org.cneko.toneko.common.Bootstrap.MODID;

/**
 * 丝袜物品渲染器（背包 / 手持 / 掉落物 / 展示框）。
 * <p>
 * 与 {@link LegwearRenderer} 保持同一套 D 值透肉和袜口高度显隐逻辑，
 * 让物品图标与身上穿的模型一致。
 *
 * <h2>26.x / GeckoLib 5.5 迁移说明</h2>
 * 旧版双 pass 直接绘制管线已被提交式渲染取代；整模颜色通过
 * {@link #getRenderColor} 提供（左右腿独立染色仅穿在身上时呈现，
 * 由 {@link LegwearRenderer} 按骨骼处理）；骨骼显隐改用
 * {@link BoneSnapshot#skipRender(boolean)}（经 {@link #applyAnimationControllers}）。
 */
public class LegwearItemRenderer extends GeoItemRenderer<LegwearItem<?>> {

    private static final float SEGMENT_THRESHOLD_FOOT = 0.20f;
    private static final float SEGMENT_THRESHOLD_CALF = 0.50f;
    private static final float SEGMENT_THRESHOLD_THIGH = 0.80f;

    /** 单腿全部骨骼前缀（按腿侧后缀 L/R 判定归属） */
    private static final List<String> LEG_BONES = List.of(
            "legwearFoot", "legwearCalf", "legwearThigh", "legwearCuffFoot", "legwearCuffCalf");

    /** 当前渲染的物品堆（capture 阶段写入渲染状态） */
    public static final DataTicket<ItemStack> ITEM_STACK =
            DataTicket.create("toneko:item_stack_item", ItemStack.class);

    public LegwearItemRenderer() {
        super(new DefaultedItemGeoModel<>(Identifier.fromNamespaceAndPath(MODID, "legwear/legwear")));
        useAlternateGuiLighting();
    }

    @Override
    public void captureDefaultRenderState(LegwearItem<?> item,
                                          com.geckolib.renderer.GeoItemRenderer.RenderData data,
                                          GeoRenderState state, float partialTick) {
        super.captureDefaultRenderState(item, data, state, partialTick);
        state.addGeckolibData(ITEM_STACK, data.itemStack());
    }

    // === 按 denier 切 RenderType：40D+ 不透明，以下半透明透肉 ===

    @Override
    public RenderType getRenderType(GeoRenderState state, Identifier texture) {
        ItemStack stack = state.getOrDefaultGeckolibData(ITEM_STACK, ItemStack.EMPTY);
        if (stack.isEmpty()) return RenderTypes.entityCutout(texture);
        int denier = LegwearItem.getDenier(stack);
        int wetness = WetnessUtil.get(stack);
        // 40D+ 不透明，但湿透（>=50）时贴肉透出肤色，切回半透明管线
        if (denier >= 40 && wetness < 50) return RenderTypes.entityCutout(texture);
        return RenderTypes.entityTranslucent(texture);
    }

    // === 整模颜色：白色 + D 值 alpha ===

    @Override
    public int getRenderColor(LegwearItem<?> animatable,
                              com.geckolib.renderer.GeoItemRenderer.RenderData data, float partialTick) {
        ItemStack stack = data.itemStack();
        if (stack == null) return 0xFFFFFFFF;
        return (renderAlpha(stack) << 24) | 0xFFFFFF;
    }

    /** 按袜口高度隐藏段（每帧在动画编译后重设） */
    @Override
    public void applyAnimationControllers(RenderPassInfo<GeoRenderState> info, BoneSnapshots snapshots) {
        super.applyAnimationControllers(info, snapshots);
        ItemStack stack = info.renderState().getOrDefaultGeckolibData(ITEM_STACK, ItemStack.EMPTY);
        if (stack.isEmpty()) return;

        float length = LegwearItem.getStockingTopHeight(stack);
        boolean foot = length >= SEGMENT_THRESHOLD_FOOT;
        boolean calf = length >= SEGMENT_THRESHOLD_CALF;
        boolean thigh = length >= SEGMENT_THRESHOLD_THIGH;

        for (String side : List.of("L", "R")) {
            // 三段本体
            setBoneSkipped(snapshots, "legwearFoot" + side, !foot);
            setBoneSkipped(snapshots, "legwearCalf" + side, !calf);
            setBoneSkipped(snapshots, "legwearThigh" + side, !thigh);
            // 勒痕环只显示在"当前袜口所在的那一段"顶部（大腿段顶部即髋部，不做环）
            setBoneSkipped(snapshots, "legwearCuffFoot" + side, !(foot && !calf));
            setBoneSkipped(snapshots, "legwearCuffCalf" + side, !(calf && !thigh));
        }
    }

    private static void setBoneSkipped(BoneSnapshots snapshots, String name, boolean skipped) {
        if (skipped) {
            snapshots.ifPresent(name, snapshot -> snapshot.skipRender(true));
        }
    }

    /** denier → alpha（0~255）：5D 极透、20D 透肉、40D 不透明 */
    private static int denierAlpha(int denier) {
        if (denier >= 40) return 255;
        if (denier >= 20) return 160;
        return 70;
    }

    /** 渲染 alpha：D 值透肉基础上，湿透进一步贴肉透出肤色（alpha 下调 = 更透） */
    private static int renderAlpha(ItemStack stack) {
        int alpha = denierAlpha(LegwearItem.getDenier(stack));
        int wetness = WetnessUtil.get(stack);
        if (wetness >= 80) alpha = (int) (alpha * 0.6f);
        else if (wetness >= 50) alpha = (int) (alpha * 0.8f);
        return Math.max(0, Math.min(255, alpha));
    }
}
