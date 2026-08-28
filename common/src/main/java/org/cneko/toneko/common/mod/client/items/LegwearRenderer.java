package org.cneko.toneko.common.mod.client.items;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;

import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.items.LegwearItem;
import org.cneko.toneko.common.mod.misc.WetnessUtil;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.specialty.DyeableGeoArmorRenderer;

import java.util.List;

import static org.cneko.toneko.common.Bootstrap.MODID;

/**
 * 丝袜渲染器：按 D 值切换半透明管线实现透肉感，
 * 按袜口高度隐藏超出部分的骨骼段，左右腿独立染色。
 *
 * <h2>GeckoLib 5.5 渲染状态说明</h2>
 * GeoArmorRenderer 使用「穿戴者实体的原版渲染状态」（玩家为 AvatarRenderState，
 * 盔甲层路径为 vanilla HumanoidRenderState）——运行期由 GeckoLib 的
 * EntityRenderStateMixin 给原版状态注入 GeoRenderState 接口。
 * 因此渲染器不能持有自定义状态类（会 ClassCastException）；这里用泛型
 * {@code S}（双上界，擦除后为 HumanoidRenderState）同时满足编译期约束，
 * 运行期直接接收原版状态对象。
 */
public class LegwearRenderer<S extends HumanoidRenderState & GeoRenderState> extends DyeableGeoArmorRenderer<LegwearItem<?>, S> {

    // 各段显隐阈值（对应 geo 模型：foot y-0.05~3.0 / calf y3.0~6.5 / thigh y6.5~12.0）
    private static final float SEGMENT_THRESHOLD_FOOT = 0.20f;
    private static final float SEGMENT_THRESHOLD_CALF = 0.50f;
    private static final float SEGMENT_THRESHOLD_THIGH = 0.80f;

    /** 单腿全部骨骼前缀（按腿侧后缀 L/R 判定归属） */
    private static final List<String> LEG_BONES = List.of(
            "legwearFoot", "legwearCalf", "legwearThigh", "legwearCuffFoot", "legwearCuffCalf");

    public LegwearRenderer() {
        super(new DefaultedItemGeoModel<>(Identifier.fromNamespaceAndPath(MODID, "legwear/legwear")));
    }

    @Override
    public void captureDefaultRenderState(LegwearItem<?> item, com.geckolib.renderer.GeoArmorRenderer.RenderData data,
                                          S state, float partialTick) {
        super.captureDefaultRenderState(item, data, state, partialTick);
        // 物品堆入状态，供 getRenderType / 染色读取
        state.addGeckolibData(NekoArmorRenderer.ITEM_STACK, data.itemStack());
    }

    // ===== 26.1.2 实体位移补偿（同 NekoArmorRenderer，问题二修复）=====
    private static final org.slf4j.Logger DBG =
            org.slf4j.LoggerFactory.getLogger("ToNekoArmorDebug");
    private static long dbgLast = 0L;

    private void applyEntityTransform(RenderPassInfo<S> info) {
        try {
            var st = info.renderState();
            // 猫娘形态路径（ItemArmorGeoLayer）栈内已带骨骼变换，交由 GeckoLib 自行处理
            Boolean geckoWearer = st.getOrDefaultGeckolibData(
                    com.geckolib.renderer.GeoArmorRenderer.IS_GECKOLIB_WEARER, Boolean.FALSE);
            if (Boolean.TRUE.equals(geckoWearer)) return;

            net.minecraft.client.Camera cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
            float ex = (float) (st.x - cam.position().x);
            float ey = (float) (st.y - cam.position().y);
            float ez = (float) (st.z - cam.position().z);
            long now = System.currentTimeMillis();
            boolean log = now - dbgLast > 4000L;
            if (log) {
                dbgLast = now;
                DBG.info("[legwear] rebuild entity transform: pos=({}, {}, {}) bodyRot={} scale={}",
                        String.format("%.2f", ex), String.format("%.2f", ey), String.format("%.2f", ez),
                        String.format("%.1f", st.bodyRot), String.format("%.2f", st.scale));
            }
            // 重建原版 LivingEntityRenderer.submit 的模型空间：
            // T(实体相对相机) · scale(scale) · rotY(180−bodyRot) · S(-1,-1,1) · T(0,-1.501,0)
            org.joml.Matrix4f m = info.poseStack().last().pose();
            m.identity();
            m.translate(ex, ey, ez);
            m.scale(st.scale, st.scale, st.scale);
            m.rotate(com.mojang.math.Axis.YP.rotationDegrees(180f - st.bodyRot));
            m.scale(-1f, -1f, 1f);
            m.translate(0f, -1.501f, 0f);
        } catch (Throwable t) {
            DBG.info("[legwear] applyEntityTransform failed: {}", t);
        }
    }
    // ===== END FIX =====

    @Override
    public boolean firePreRenderEvent(RenderPassInfo<S> info, SubmitNodeCollector collector) {
        applyEntityTransform(info);
        return super.firePreRenderEvent(info, collector);
    }

    // === 按 denier 切 RenderType：40D+ 不透明，以下半透明透肉 ===

    @Override
    public RenderType getRenderType(S state, Identifier texture) {
        ItemStack stack = state.getOrDefaultGeckolibData(NekoArmorRenderer.ITEM_STACK, ItemStack.EMPTY);
        return pickRenderType(stack, texture);
    }

    /** 按 denier / 湿度选择渲染管线。 */
    private static RenderType pickRenderType(ItemStack stack, Identifier texture) {
        if (stack.isEmpty()) return RenderTypes.armorCutoutNoCull(texture);
        int denier = LegwearItem.getDenier(stack);
        int wetness = WetnessUtil.get(stack);
        // 40D+ 不透明，但湿透（>=50）时贴肉透出肤色，切回半透明管线
        if (denier >= 40 && wetness < 50) return RenderTypes.armorCutoutNoCull(texture);
        return RenderTypes.entityTranslucent(texture);
    }

    // === 左右腿独立染色 + 透肉 alpha ===

    @Override
    protected boolean isBoneDyeable(GeoBone bone) {
        // 只有丝袜本体/袜口骨骼参与染色，其余保持纹理原色
        String name = bone.name();
        return LEG_BONES.stream().anyMatch(name::startsWith);
    }

    @Override
    protected int getColorForBone(S state, GeoBone bone, int baseColor) {
        ItemStack stack = state.getOrDefaultGeckolibData(NekoArmorRenderer.ITEM_STACK, ItemStack.EMPTY);
        if (stack.isEmpty()) return baseColor;

        // 骨骼名以 L 结尾 → 左腿色，否则右腿色
        boolean leftSide = bone.name().endsWith("L");
        int rgb = leftSide ? LegwearItem.getLeftRenderColor(stack) : LegwearItem.getRightRenderColor(stack);
        int alpha = renderAlpha(stack);
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    /** 按袜口高度隐藏段（每帧在骨骼编译阶段重设） */
    @Override
    public void adjustModelBonesForRender(RenderPassInfo<S> info, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(info, snapshots);
        ItemStack stack = info.renderState().getOrDefaultGeckolibData(NekoArmorRenderer.ITEM_STACK, ItemStack.EMPTY);
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
