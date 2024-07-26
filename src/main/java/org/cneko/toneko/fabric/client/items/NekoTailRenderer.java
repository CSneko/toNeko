package org.cneko.toneko.fabric.client.items;


import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
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
        super(new DefaultedItemGeoModel<>(Identifier.of(MODID, "armor/neko_armor")));
    }


    @Override
    public void preRender(MatrixStack poseStack, NekoTailItem item, BakedGeoModel model, @Nullable VertexConsumerProvider bufferSource,
                          @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight,
                          int packedOverlay, int colour) {
        super.preRender(poseStack,item,model,bufferSource,buffer,isReRender,partialTick,packedLight,packedOverlay,colour);
        // 如果有反转附魔
        if (EnchantmentUtil.hasEnchantment(ToNekoEnchantments.REVERSION.getValue(),this.currentStack)){
            // 旋转180度
            poseStack.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(180));
            // 前进1/16个单位并下移动1单位
            poseStack.translate(0,-1.5,-0.0625);
        }
    }
}
