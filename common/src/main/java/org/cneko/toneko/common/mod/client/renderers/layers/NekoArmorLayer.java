package org.cneko.toneko.common.mod.client.renderers.layers;
import org.cneko.toneko.common.mod.client.renderers.TonekoLivingEntityGeoState;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.cneko.toneko.common.mod.client.renderers.NekoRenderer;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.ItemArmorGeoLayer;

import java.util.List;

/**
 * 猫娘实体盔甲渲染层：让猫娘身上（捡起/被赠送/AI 穿戴）的盔甲显示出来。
 * 骨骼 → 槽位映射只覆盖有实际部件意义的 4 根骨骼（"chest" 胸骨不映射，避免胸甲双渲染）。
 * 丝袜（LegwearItem）的 legwear 模型使用 GeckoLib 标准 armorLeftLeg/armorRightLeg 骨骼名；
 * 玩家穿戴路径（HumanoidArmorLayerMixin）不走本层，互不影响。
 *
 * <h2>26.x / GeckoLib 5.5 迁移说明</h2>
 * 新版 {@link ItemArmorGeoLayer} 改为提交式渲染：
 * {@link #getRelevantBones} 只负责「骨骼 → 装备槽」映射（返回静态映射表），
 * 装备物品由基类在 addRenderData 阶段自动从实体读取；配置开关在其前置阶段拦截。
 */
public class NekoArmorLayer<T extends NekoEntity> extends ItemArmorGeoLayer<T, Void, org.cneko.toneko.common.mod.client.renderers.TonekoLivingEntityGeoState> {

    public NekoArmorLayer(GeoRenderer<T, Void, org.cneko.toneko.common.mod.client.renderers.TonekoLivingEntityGeoState> renderer,
                          EntityRendererProvider.Context context) {
        super(renderer, context);
    }

    @Override
    public void addRenderData(T neko, Void unused, org.cneko.toneko.common.mod.client.renderers.TonekoLivingEntityGeoState state, float partialTick) {
        // 配置关闭时不收集盔甲数据，整层跳过
        if (!NekoRenderer.isArmorDisplayEnabled()) return;
        super.addRenderData(neko, unused, state, partialTick);
    }

    @Override
    protected List<ItemArmorGeoLayer.RenderData> getRelevantBones(RenderPassInfo<org.cneko.toneko.common.mod.client.renderers.TonekoLivingEntityGeoState> info) {
        return List.of(
                ItemArmorGeoLayer.RenderData.head("Head"),
                ItemArmorGeoLayer.RenderData.body("Body"),
                ItemArmorGeoLayer.RenderData.rightLeg("RightLeg"),
                ItemArmorGeoLayer.RenderData.leftLeg("LeftLeg"));
    }
}
