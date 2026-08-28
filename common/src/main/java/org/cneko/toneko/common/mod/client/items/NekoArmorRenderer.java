package org.cneko.toneko.common.mod.client.items;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import org.cneko.toneko.common.mod.misc.ToNekoEnchantments;
import org.cneko.toneko.common.mod.util.EnchantmentUtil;
import org.cneko.toneko.common.mod.items.NekoArmor;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.specialty.DyeableGeoArmorRenderer;

import static org.cneko.toneko.common.Bootstrap.MODID;

/**
 * 猫娘套装（尾巴/耳朵/爪爪）的 GeckoLib 渲染器。
 *
 * <h2>GeckoLib 5.5 渲染状态说明</h2>
 * GeoArmorRenderer 使用「穿戴者实体的原版渲染状态」（玩家为 AvatarRenderState，
 * 盔甲层路径为 vanilla HumanoidRenderState）——运行期由 GeckoLib 的
 * EntityRenderStateMixin 给原版状态注入 GeoRenderState 接口。
 * 因此渲染器不能持有自定义状态类（会 ClassCastException）；这里用泛型
 * {@code S}（双上界，擦除后为 HumanoidRenderState）同时满足编译期约束，
 * 运行期直接接收原版状态对象。
 */
public class NekoArmorRenderer<T extends NekoArmor<T>, S extends HumanoidRenderState & GeoRenderState> extends DyeableGeoArmorRenderer<T, S> {
    /** 当前渲染的物品堆（capture 阶段写入渲染状态） */
    public static final DataTicket<ItemStack> ITEM_STACK =
            DataTicket.create("toneko:item_stack", ItemStack.class);
    /** 当前穿戴者实体（capture 阶段写入渲染状态） */
    public static final DataTicket<Entity> WEARER_ENTITY =
            DataTicket.create("toneko:wearer_entity", Entity.class);

    public NekoArmorRenderer() {
        super(new DefaultedItemGeoModel<>(Identifier.fromNamespaceAndPath(MODID, "armor/neko_armor")));
    }

    // ===== 26.1.2 实体位移补偿（问题二修复）=====
    // 26.1.2 渲染图把「实体 → 相机」的平移放在实体渲染节点上，模型类提交（submitModel）在节点内
    // 补偿，而 GeckoLib 盔甲走全局 custom-geometry 存储，拿不到节点位移 → 盔甲画在世界原点（漂浮）。
    // 这里在盔甲提交前，把实体的相机相对位移强制写回姿态矩阵的平移分量；
    // 对栈里已有正确位移的实体（其它生物）这是幂等操作。
    private static final org.slf4j.Logger DBG =
            org.slf4j.LoggerFactory.getLogger("ToNekoArmorDebug");
    private static long dbgLast = 0L;

    @SuppressWarnings("unchecked")
    private void applyEntityTransform(RenderPassInfo<S> info, String tag) {
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
                DBG.info("[{}] rebuild entity transform: pos=({}, {}, {}) bodyRot={} scale={}",
                        tag, String.format("%.2f", ex), String.format("%.2f", ey), String.format("%.2f", ez),
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
            DBG.info("[{}] applyEntityTransform failed: {}", tag, t);
        }
    }
    // ===== END FIX =====

    @Override
    public void captureDefaultRenderState(T item, com.geckolib.renderer.GeoArmorRenderer.RenderData data,
                                          S state, float partialTick) {
        super.captureDefaultRenderState(item, data, state, partialTick);
        // 把物品堆与穿戴者塞进渲染状态，供后续阶段读取
        state.addGeckolibData(ITEM_STACK, data.itemStack());
        state.addGeckolibData(WEARER_ENTITY, data.entity());
    }

    @Override
    public boolean firePreRenderEvent(RenderPassInfo<S> info, SubmitNodeCollector collector) {
        applyEntityTransform(info, "neko_armor");
        boolean proceed = super.firePreRenderEvent(info, collector);
        if (proceed) {
            PoseStack poseStack = info.poseStack();
            ItemStack stack = info.renderState().getOrDefaultGeckolibData(ITEM_STACK, ItemStack.EMPTY);

            // 如果有反转附魔
            if (!stack.isEmpty() && EnchantmentUtil.hasEnchantment(ToNekoEnchantments.REVERSION.identifier(), stack)) {
                // 旋转180度
                poseStack.mulPose(Axis.XN.rotationDegrees(180));
                // 前进1/16个单位并下移动1单位
                poseStack.translate(0, -1.5, -0.0625);
            }

            // 尾巴：蹲下时向后移动一点（由子类覆写 prependTailPose 调用）
        }
        return proceed;
    }

    // === 染色 ===

    @Override
    protected boolean isBoneDyeable(GeoBone bone) {
        return true; // 整个模型按染色组件着色
    }

    @Override
    protected int getColorForBone(S state, GeoBone bone, int baseColor) {
        ItemStack stack = state.getOrDefaultGeckolibData(ITEM_STACK, ItemStack.EMPTY);
        if (stack.isEmpty() || !stack.has(DataComponents.DYED_COLOR)) {
            return baseColor;
        }
        int rgb = DyedItemColor.getOrDefault(stack, 6463722);
        // 全不透明 ARGB
        return 0xFF000000 | rgb;
    }

    public static class NekoTailRenderer<S extends HumanoidRenderState & GeoRenderState> extends NekoArmorRenderer<NekoArmor.NekoTailItem, S> {
        public NekoTailRenderer() {
            super();
        }

        @Override
        public boolean firePreRenderEvent(RenderPassInfo<S> info, SubmitNodeCollector collector) {
            boolean proceed = super.firePreRenderEvent(info, collector);
            if (proceed) {
                Entity entity = info.renderState().getGeckolibData(WEARER_ENTITY);
                if (entity != null && entity.isShiftKeyDown()) {
                    // 向后移动一点
                    info.poseStack().translate(0, 0, 0.35);
                }
            }
            return proceed;
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
