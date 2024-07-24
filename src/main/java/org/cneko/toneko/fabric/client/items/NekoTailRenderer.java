package org.cneko.toneko.fabric.client.items;


import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.cneko.toneko.fabric.enchanments.ToNekoEnchantments;
import org.cneko.toneko.fabric.items.NekoTailItem;
import org.cneko.toneko.fabric.util.EnchantmentUtil;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

import static org.cneko.toneko.common.Bootstrap.MODID;


public class NekoTailRenderer extends GeoArmorRenderer<NekoTailItem> {
    public NekoTailRenderer() {
        super(new DefaultedItemGeoModel<>(Identifier.of(MODID, "armor/neko_armor"))); // Using DefaultedItemGeoModel like this puts our 'location' as item/armor/example armor in the assets folders.
        //addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }


    @Override
    public void actuallyRender(MatrixStack poseStack, NekoTailItem item, BakedGeoModel model, @Nullable RenderLayer renderType,
                               VertexConsumerProvider bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick,
                               int packedLight, int packedOverlay, int colour) {
        poseStack.push();
        // 如果有反转附魔,就将其旋转180度后向前移动4个单位
        if (EnchantmentUtil.hasEnchantment(ToNekoEnchantments.REVERSION.getValue(),this.currentStack)){
            // 旋转180度
            Vector3f axis = new Vector3f(1, 0, 0);
            poseStack.multiply(new Quaternionf().rotateAxis((float) Math.toRadians(180), axis));
            // 前进4个单位
            poseStack.translate(0,0,-4);
        }
        poseStack.pop();
        super.actuallyRender(poseStack, item, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }
}
