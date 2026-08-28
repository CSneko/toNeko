package org.cneko.toneko.common.mod.client.renderers;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.entities.boss.NekoBoss;

import static org.cneko.toneko.common.Bootstrap.MODID;
import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

public class NekoBossRenderer<T extends NekoEntity & NekoBoss> extends NekoRenderer<T>{
    public NekoBossRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new NekoBossModel<>());
    }

    public static class NekoBossModel<T extends NekoEntity & NekoBoss> extends NekoModel<T> {
        private static NekoEntity nekoOf(com.geckolib.renderer.base.GeoRenderState renderState) {
            try {
                return renderState.getGeckolibData(NekoRenderer.TONEKO_ENTITY);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState renderState) {
            NekoEntity neko = nekoOf(renderState);
            if (neko != null) {
                // 物理存在性检测用 geckolib/models/ 全路径；GeckoLib 5.5 返回短 ID（ns:neko/boss/<skin>）
                Identifier physical = Identifier.fromNamespaceAndPath(
                        MODID, "geckolib/models/neko/boss/" + neko.getSkin() + ".geo.json");
                if (checkResource(physical)) return toNekoLoc("neko/boss/" + neko.getSkin());
            }
            return toNekoLoc("neko/common");
        }

        @Override
        public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState renderState) {
            NekoEntity neko = nekoOf(renderState);
            if (neko != null) {
                Identifier id = Identifier.fromNamespaceAndPath(
                        MODID, "textures/neko/boss/" + neko.getSkin() + ".png");
                if (checkResource(id)) return id;
            }
            return Identifier.fromNamespaceAndPath(MODID, "textures/neko/common.png");
        }

        @Override
        public Identifier getAnimationResource(T animatable) {
            // 物理存在性检测用 geckolib/animations/ 全路径；返回短 ID
            Identifier physical = Identifier.fromNamespaceAndPath(
                    MODID, "geckolib/animations/neko/boss/" + animatable.getSkin() + ".animation.json");
            if (checkResource(physical)) return toNekoLoc("neko/boss/" + animatable.getSkin());
            return toNekoLoc("neko/common");
        }
    }
}
