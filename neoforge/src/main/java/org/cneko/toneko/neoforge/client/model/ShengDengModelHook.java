package org.cneko.toneko.neoforge.client.model;

import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.cneko.toneko.common.Bootstrap;
import org.cneko.toneko.common.mod.client.renderers.ShengDengSpecialModel;
import org.joml.Matrix4f;

/**
 * NeoForge 客户端模型钩子：把省凳法棍烘焙后的物品模型替换为特殊模型渲染器。
 * 与 Fabric 侧 {@code ShengDengModelHook} 等价：26.x 起 BEWLR 已移除，
 * 这里在 {@link ModelEvent.ModifyBakingResult} 中以 {@link SpecialModelWrapper} 接入
 * 新的提交式物品渲染管线。
 */
public final class ShengDengModelHook {
    public static final Identifier ITEM_ID =
            Identifier.fromNamespaceAndPath(Bootstrap.MODID, "sheng_deng");

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        var models = event.getBakingResult().itemStackModels();
        if (!models.containsKey(ITEM_ID)) return;

        // getTextureGetter() 固定查方块图集，参数是“精灵 id”（toneko:block/...），
        // 不涉及图集 id——千万不要传 textures/atlas/blocks.png 之类的图集路径。
        Identifier spriteId = Identifier.fromNamespaceAndPath(Bootstrap.MODID, "block/sheng_deng_red");
        TextureAtlasSprite sprite = event.getTextureGetter().apply(spriteId);
        var bakedMaterial = new Material.Baked(sprite, true);
        ModelRenderProperties properties =
                new ModelRenderProperties(true, bakedMaterial,
                        net.minecraft.client.resources.model.cuboid.ItemTransforms.NO_TRANSFORMS);
        models.put(ITEM_ID, new SpecialModelWrapper<>(ShengDengSpecialModel.INSTANCE, properties, new Matrix4f()));
    }

    private ShengDengModelHook() {
    }
}
