package org.cneko.toneko.common.mod.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.cneko.toneko.common.mod.entities.AmmunitionEntity;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;

import static org.cneko.toneko.common.Bootstrap.MODID;

/**
 * 弹药实体渲染器。
 *
 * <h2>26.x / GeckoLib 5.5 迁移说明</h2>
 * preRender 缩放挂钩改为 {@link #firePreRenderEvent}；模型资源方法改用渲染状态入参。
 * 弹药实体是 ThrowableProjectile（非生物），状态类必须用
 * {@link TonekoGeoEntityState}（EntityRenderState 系），否则会在
 * GeoEntityRenderer#extractRenderState 里被 (LivingEntity) 强转崩溃。
 */
public class AmmunitionRenderer extends GeoEntityRenderer<AmmunitionEntity, TonekoGeoEntityState> {
    public AmmunitionRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AmmunitionModel());
    }

    @Override
    public TonekoGeoEntityState createRenderState(AmmunitionEntity entity, Void unused) {
        return new TonekoGeoEntityState();
    }

    @Override
    public boolean firePreRenderEvent(RenderPassInfo<TonekoGeoEntityState> info,
                                      SubmitNodeCollector collector) {
        boolean proceed = super.firePreRenderEvent(info, collector);
        if (proceed) {
            PoseStack poseStack = info.poseStack();
            poseStack.scale(0.1f, 0.1f, 0.1f);
        }
        return proceed;
    }

    public static class AmmunitionModel extends GeoModel<AmmunitionEntity> {

        @Override
        public Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState renderState) {
            // 物理文件在 assets/toneko/geckolib/models/neko/crystal_neko.geo.json，返回 GeckoLib 5.5 短 ID
            return Identifier.fromNamespaceAndPath(
                    MODID,"neko/crystal_neko"
            );
        }

        @Override
        public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState renderState) {
            return Identifier.fromNamespaceAndPath(
                    MODID,"textures/neko/crystal_neko.png"
            );
        }

        @Override
        public Identifier getAnimationResource(AmmunitionEntity ammunitionEntity) {
            return Identifier.fromNamespaceAndPath(
                    MODID,"neko/crystal_neko"
            );
        }
    }
}
