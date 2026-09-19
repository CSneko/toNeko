package org.cneko.toneko.fabric.client.model;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import org.cneko.toneko.common.Bootstrap;
import org.cneko.toneko.common.mod.client.renderers.ShengDengSpecialModel;
import org.joml.Matrix4f;

/**
 * Fabric 客户端模型钩子：把省凳法棍烘焙后的物品模型替换为特殊模型渲染器。
 *
 * <h2>26.x 迁移说明</h2>
 * BEWLR / BuiltinItemRendererRegistry 均已移除；这里用 Fabric 的
 * {@code modifyItemModelAfterBake} 在烘焙完成后以 {@link SpecialModelWrapper}
 * 包装，使新提交式物品管线调用 {@link ShengDengSpecialModel}。
 */
public final class ShengDengModelHook {
    private static final Identifier ITEM_ID =
            Identifier.fromNamespaceAndPath(Bootstrap.MODID, "sheng_deng");

    public static void register() {
        ModelLoadingPlugin.register(pluginContext -> pluginContext.modifyItemModelAfterBake().register((model, context) -> {
            if (!ITEM_ID.equals(context.itemId())) return model;

            // SpriteId 的第一个参数是图集“纹理”id（TextureAtlas.LOCATION_BLOCKS），
            // 不要写成图集“定义”id（minecraft:blocks）——那是 AtlasManager.getAtlasOrThrow 用的键。
            SpriteId spriteId = new SpriteId(TextureAtlas.LOCATION_BLOCKS,
                    Identifier.fromNamespaceAndPath(Bootstrap.MODID, "block/sheng_deng_red"));
            var bakedMaterial = new net.minecraft.client.resources.model.sprite.Material.Baked(
                    context.bakingContext().sprites().get(spriteId), true);
            ModelRenderProperties properties =
                    new ModelRenderProperties(true, bakedMaterial,
                            net.minecraft.client.resources.model.cuboid.ItemTransforms.NO_TRANSFORMS);
            return new SpecialModelWrapper<>(ShengDengSpecialModel.INSTANCE, properties, new Matrix4f());
        }));
    }
}
