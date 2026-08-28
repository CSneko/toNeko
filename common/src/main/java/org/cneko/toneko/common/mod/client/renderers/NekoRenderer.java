package org.cneko.toneko.common.mod.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.cneko.toneko.common.mod.client.renderers.layers.NekoArmorLayer;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.util.ConfigUtil;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;
import com.geckolib.renderer.GeoEntityRenderer;

import java.util.Optional;

import static org.cneko.toneko.common.Bootstrap.MODID;
import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

/**
 * 猫娘实体渲染器（GeckoLib）。
 *
 * <h2>26.x / GeckoLib 5.5 迁移说明</h2>
 * 旧的 preRender/renderRecursively 直接绘制挂钩已被提交式渲染管线取代：
 * <ul>
 *   <li>幼体/坐下/游泳姿态变换 → {@link #firePreRenderEvent}；</li>
 *   <li>胸部按基因缩放 → {@link #applyAnimationControllers} 修改骨骼快照；</li>
 *   <li>手持物品渲染 → 内置 {@link ItemInHandGeoLayer}（挂到 RightArm/LeftArm 骨骼）；</li>
 *   <li>盔甲渲染层 → {@link NekoArmorLayer}（基于新版 ItemArmorGeoLayer）。</li>
 * </ul>
 */
public class NekoRenderer<T extends NekoEntity> extends GeoEntityRenderer<T, org.cneko.toneko.common.mod.client.renderers.TonekoLivingEntityGeoState> {
    /** 当前渲染的猫娘实体（capture 阶段写入渲染状态，供模型/动画阶段读取） */
    public static final DataTicket<NekoEntity> TONEKO_ENTITY =
            DataTicket.create("toneko:neko_entity", NekoEntity.class);

    public NekoRenderer(EntityRendererProvider.Context context) {
        super(context, new NekoModel<>());
        this.withRenderLayer(new NekoArmorLayer<>(this, context));
        this.withRenderLayer(new ItemInHandGeoLayer<>(context, this, "RightArm", "LeftArm"));
    }

    public NekoRenderer(EntityRendererProvider.Context context, GeoModel<T> model) {
        super(context, model);
        this.withRenderLayer(new NekoArmorLayer<>(this, context));
        this.withRenderLayer(new ItemInHandGeoLayer<>(context, this, "RightArm", "LeftArm"));
    }

    @Override
    public TonekoLivingEntityGeoState createRenderState(T neko, Void unused) {
        return new TonekoLivingEntityGeoState();
    }

    /** 是否显示猫娘身上的盔甲（含丝袜）；渲染时实时读配置，避免 ConfigUtil 引用 client 类 */
    public static boolean isArmorDisplayEnabled() {
        return ConfigUtil.CONFIG.getBooleanOr("client.neko_armor.display", false);
    }

    @Override
    public void captureDefaultRenderState(T neko, Void unused,
                                          org.cneko.toneko.common.mod.client.renderers.TonekoLivingEntityGeoState state,
                                          float partialTick) {
        super.captureDefaultRenderState(neko, unused, state, partialTick);
        // 实体引用入状态：供模型纹理选择、动画缩放与预渲染变换读取
        state.addGeckolibData(TONEKO_ENTITY, neko);
    }

    @Override
    public boolean firePreRenderEvent(RenderPassInfo<org.cneko.toneko.common.mod.client.renderers.TonekoLivingEntityGeoState> info,
                                      SubmitNodeCollector collector) {
        PoseStack poseStack = info.poseStack();
        T neko = (T) info.renderState().getGeckolibData(TONEKO_ENTITY);
        if (neko != null) {
            if (neko.isNekoBaby()) {
                poseStack.scale(0.5F, 0.5F, 0.5F); // 将幼年实体的尺寸缩小为原来的一半
            }
            // 坐下时向下移动
            if (neko.isSitting()) {
                poseStack.translate(0, -0.7, 0);
            }
            // 游泳/爬行时向下移动
            if (neko.getPose() == net.minecraft.world.entity.Pose.SWIMMING) {
                poseStack.translate(0, -0.5, 0);
            }
        }
        return true;
    }

    @Override
    public void applyAnimationControllers(
            RenderPassInfo<org.cneko.toneko.common.mod.client.renderers.TonekoLivingEntityGeoState> info,
            BoneSnapshots snapshots) {
        super.applyAnimationControllers(info, snapshots);
        // 根据基因表达缩放胸部骨骼
        T neko = (T) info.renderState().getGeckolibData(TONEKO_ENTITY);
        if (neko != null) {
            float scale = neko.getChestScale();
            snapshots.ifPresent("chest", snapshot -> snapshot.setScale(scale, scale, scale));
        }
    }

    public static class NekoModel<T extends NekoEntity> extends GeoModel<T> {
        @Override
        public Identifier getModelResource(GeoRenderState renderState) {
             // GeckoLib 5.5 资源 ID 为短 ID（无 geckolib/ 前缀、无扩展名），物理文件在 assets/toneko/geckolib/models/
             return toNekoLoc("neko/common");
        }

        @Override
        @SuppressWarnings("unchecked")
        public Identifier getTextureResource(GeoRenderState renderState) {
            NekoEntity neko = renderState.getGeckolibData(TONEKO_ENTITY);
            if (neko == null) return toNekoLoc("textures/neko/default.png");
            Identifier id = Identifier.fromNamespaceAndPath(
                    MODID,"textures/neko/"+neko.getSkin()+".png"
            );
            if (checkResource(id)){
                return id;
            }
            return Identifier.fromNamespaceAndPath(
                    MODID,"textures/neko/"+neko.getRandomSkin()+".png"
            );
        }

        @Override
        public Identifier getAnimationResource(T animatable) {
            // 物理文件在 assets/toneko/geckolib/animations/neko/common.animation.json
            return toNekoLoc("neko/common");
        }
    }

    public static ResourceManager getResourceManager(){
        return net.minecraft.client.Minecraft.getInstance().getResourceManager();
    }
    public static boolean checkResource(Identifier location){
        try {
            Optional<Resource> resource = getResourceManager().getResource(location);
            return resource.isPresent();
        } catch (Exception e) {
            return false;
        }
    }
}
