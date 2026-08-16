package org.cneko.toneko.common.mod.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.blocks.ClotheslineBlockEntity;
import org.cneko.toneko.common.mod.items.LegwearItem;
import org.cneko.toneko.common.mod.misc.WetnessUtil;
import org.joml.Matrix4f;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

/**
 * 晾衣架方块实体渲染器：把挂着的丝袜按 legwear geo 模型（脚/小腿/大腿/勒痕环）
 * 真实 UV 渲染成 3D 挂垂袜，染色 + 透肉（D值/湿度），随风轻微摆动。
 * 模型数据由脚本从 geo/item/legwear/legwear.geo.json 生成。
 */
@Environment(EnvType.CLIENT)
public class ClotheslineBlockEntityRenderer implements BlockEntityRenderer<ClotheslineBlockEntity> {
    private static final ResourceLocation LEGWEAR_TEXTURE = toNekoLoc("textures/item/legwear/legwear.png");

    public ClotheslineBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(ClotheslineBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack stack = be.getItem();
        if (stack.isEmpty() || !LegwearItem.isLegwear(stack)) return;

        int rgb = ((LegwearItem.getLeftRenderColor(stack) + LegwearItem.getRightRenderColor(stack)) / 2) & 0xFFFFFF;
        int denier = LegwearItem.getDenier(stack);
        int a = denier >= 40 ? 255 : (denier >= 20 ? 160 : 70);
        int wetness = WetnessUtil.get(stack);
        if (wetness >= 80) a = (int) (a * 0.6f);
        else if (wetness >= 50) a = (int) (a * 0.8f);

        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        // 段显隐（与 LegwearRenderer 阈值一致）：袜口高度决定挂垂的段数
        float length = LegwearItem.getStockingTopHeight(stack);
        boolean footV = length >= 0.2f;
        boolean calfV = length >= 0.5f;
        boolean thighV = length >= 0.8f;
        // 可见段顶部（geo y）：裤袜=大腿顶，过膝袜=小腿勒痕环顶，滑落到脚=脚部勒痕环顶
        float topY = thighV ? 12.0f : (calfV ? 6.55f : 3.05f);

        poseStack.pushPose();
        // 挂点：腿顶（geo y=12）对齐晾绳，中心对齐方块中心；geo 单位 1/16 方块
        poseStack.translate(0.5, 0.85, 0.5);
        float t = be.getLevel() == null ? 0f : be.getLevel().getGameTime() + partialTick;
        float sway = (float) Math.sin(t * 0.03) * 6.0f;
        poseStack.mulPose(Axis.XP.rotationDegrees(sway));
        poseStack.scale(1.3f, 1.3f, 1.3f);
        poseStack.translate(-2.0f / 16f, -topY / 16f, 0f);
        poseStack.scale(1f / 16f, 1f / 16f, 1f / 16f);

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(LEGWEAR_TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f mat = pose.pose();
        int light = packedLight;

        if (footV) { // legwearFootL
        quad(vc, pose, mat, r, g, b, a, light, 0f, 0f, -1f, 0.0f, 0.0f, -0.2f, 3.0f, -2.2f, 0.13671875f, 0.0f, 4.2f, 3.0f, -2.2f, 0.13671875f, 0.09375f, 4.2f, -0.05f, -2.2f, 0.0f, 0.09375f, -0.2f, -0.05f, -2.2f);
        quad(vc, pose, mat, r, g, b, a, light, 0f, 0f, 1f, 0.0f, 0.1875f, 4.2f, 3.0f, 2.2f, 0.13671875f, 0.1875f, -0.2f, 3.0f, 2.2f, 0.13671875f, 0.28125f, -0.2f, -0.05f, 2.2f, 0.0f, 0.28125f, 4.2f, -0.05f, 2.2f);
        quad(vc, pose, mat, r, g, b, a, light, 1f, 0f, 0f, 0.0f, 0.09375f, 4.2f, 3.0f, 2.2f, 0.13671875f, 0.09375f, 4.2f, 3.0f, -2.2f, 0.13671875f, 0.1875f, 4.2f, -0.05f, -2.2f, 0.0f, 0.1875f, 4.2f, -0.05f, 2.2f);
        quad(vc, pose, mat, r, g, b, a, light, -1f, 0f, 0f, 0.0f, 0.28125f, -0.2f, 3.0f, -2.2f, 0.13671875f, 0.28125f, -0.2f, 3.0f, 2.2f, 0.13671875f, 0.375f, -0.2f, -0.05f, 2.2f, 0.0f, 0.375f, -0.2f, -0.05f, -2.2f);
        quad(vc, pose, mat, r, g, b, a, light, 0f, 1f, 0f, 0.0f, 0.375f, -0.2f, 3.0f, -2.2f, 0.015625f, 0.375f, 4.2f, 3.0f, -2.2f, 0.015625f, 0.390625f, 4.2f, 3.0f, 2.2f, 0.0f, 0.390625f, -0.2f, 3.0f, 2.2f);
        quad(vc, pose, mat, r, g, b, a, light, 0f, -1f, 0f, 0.0f, 0.390625f, -0.2f, -0.05f, -2.2f, 0.015625f, 0.390625f, 4.2f, -0.05f, -2.2f, 0.015625f, 0.40625f, 4.2f, -0.05f, 2.2f, 0.0f, 0.40625f, -0.2f, -0.05f, 2.2f);
        }
        if (calfV) { // legwearCalfL
        quad(vc, pose, mat, r, g, b, a, light, 0f, 0f, -1f, 0.140625f, 0.0f, -0.2f, 6.5f, -2.2f, 0.27734375f, 0.0f, 4.2f, 6.5f, -2.2f, 0.27734375f, 0.109375f, 4.2f, 3.0f, -2.2f, 0.140625f, 0.109375f, -0.2f, 3.0f, -2.2f);
        quad(vc, pose, mat, r, g, b, a, light, 0f, 0f, 1f, 0.140625f, 0.21875f, 4.2f, 6.5f, 2.2f, 0.27734375f, 0.21875f, -0.2f, 6.5f, 2.2f, 0.27734375f, 0.328125f, -0.2f, 3.0f, 2.2f, 0.140625f, 0.328125f, 4.2f, 3.0f, 2.2f);
        quad(vc, pose, mat, r, g, b, a, light, 1f, 0f, 0f, 0.140625f, 0.109375f, 4.2f, 6.5f, 2.2f, 0.27734375f, 0.109375f, 4.2f, 6.5f, -2.2f, 0.27734375f, 0.21875f, 4.2f, 3.0f, -2.2f, 0.140625f, 0.21875f, 4.2f, 3.0f, 2.2f);
        quad(vc, pose, mat, r, g, b, a, light, -1f, 0f, 0f, 0.140625f, 0.328125f, -0.2f, 6.5f, -2.2f, 0.27734375f, 0.328125f, -0.2f, 6.5f, 2.2f, 0.27734375f, 0.4375f, -0.2f, 3.0f, 2.2f, 0.140625f, 0.4375f, -0.2f, 3.0f, -2.2f);
        quad(vc, pose, mat, r, g, b, a, light, 0f, 1f, 0f, 0.140625f, 0.4375f, -0.2f, 6.5f, -2.2f, 0.15625f, 0.4375f, 4.2f, 6.5f, -2.2f, 0.15625f, 0.453125f, 4.2f, 6.5f, 2.2f, 0.140625f, 0.453125f, -0.2f, 6.5f, 2.2f);
        quad(vc, pose, mat, r, g, b, a, light, 0f, -1f, 0f, 0.140625f, 0.453125f, -0.2f, 3.0f, -2.2f, 0.15625f, 0.453125f, 4.2f, 3.0f, -2.2f, 0.15625f, 0.46875f, 4.2f, 3.0f, 2.2f, 0.140625f, 0.46875f, -0.2f, 3.0f, 2.2f);
        }
        if (thighV) { // legwearThighL
        quad(vc, pose, mat, r, g, b, a, light, 0f, 0f, -1f, 0.28125f, 0.0f, -0.2f, 12.0f, -2.2f, 0.41796875f, 0.0f, 4.2f, 12.0f, -2.2f, 0.41796875f, 0.171875f, 4.2f, 6.5f, -2.2f, 0.28125f, 0.171875f, -0.2f, 6.5f, -2.2f);
        quad(vc, pose, mat, r, g, b, a, light, 0f, 0f, 1f, 0.28125f, 0.34375f, 4.2f, 12.0f, 2.2f, 0.41796875f, 0.34375f, -0.2f, 12.0f, 2.2f, 0.41796875f, 0.515625f, -0.2f, 6.5f, 2.2f, 0.28125f, 0.515625f, 4.2f, 6.5f, 2.2f);
        quad(vc, pose, mat, r, g, b, a, light, 1f, 0f, 0f, 0.28125f, 0.171875f, 4.2f, 12.0f, 2.2f, 0.41796875f, 0.171875f, 4.2f, 12.0f, -2.2f, 0.41796875f, 0.34375f, 4.2f, 6.5f, -2.2f, 0.28125f, 0.34375f, 4.2f, 6.5f, 2.2f);
        quad(vc, pose, mat, r, g, b, a, light, -1f, 0f, 0f, 0.28125f, 0.515625f, -0.2f, 12.0f, -2.2f, 0.41796875f, 0.515625f, -0.2f, 12.0f, 2.2f, 0.41796875f, 0.6875f, -0.2f, 6.5f, 2.2f, 0.28125f, 0.6875f, -0.2f, 6.5f, -2.2f);
        quad(vc, pose, mat, r, g, b, a, light, 0f, 1f, 0f, 0.28125f, 0.6875f, -0.2f, 12.0f, -2.2f, 0.296875f, 0.6875f, 4.2f, 12.0f, -2.2f, 0.296875f, 0.703125f, 4.2f, 12.0f, 2.2f, 0.28125f, 0.703125f, -0.2f, 12.0f, 2.2f);
        quad(vc, pose, mat, r, g, b, a, light, 0f, -1f, 0f, 0.28125f, 0.703125f, -0.2f, 6.5f, -2.2f, 0.296875f, 0.703125f, 4.2f, 6.5f, -2.2f, 0.296875f, 0.71875f, 4.2f, 6.5f, 2.2f, 0.28125f, 0.71875f, -0.2f, 6.5f, 2.2f);
        }
        if (footV && !calfV) { // legwearCuffFootL
        quad(vc, pose, mat, r, g, b, a, light, 0f, 0f, -1f, 0.421875f, 0.0f, -0.45f, 3.05f, -2.45f, 0.57421875f, 0.0f, 4.45f, 3.05f, -2.45f, 0.57421875f, 0.021484375f, 4.45f, 2.35f, -2.45f, 0.421875f, 0.021484375f, -0.45f, 2.35f, -2.45f);
        quad(vc, pose, mat, r, g, b, a, light, 0f, 0f, 1f, 0.421875f, 0.04296875f, 4.45f, 3.05f, 2.45f, 0.57421875f, 0.04296875f, -0.45f, 3.05f, 2.45f, 0.57421875f, 0.064453125f, -0.45f, 2.35f, 2.45f, 0.421875f, 0.064453125f, 4.45f, 2.35f, 2.45f);
        quad(vc, pose, mat, r, g, b, a, light, 1f, 0f, 0f, 0.421875f, 0.021484375f, 4.45f, 3.05f, 2.45f, 0.57421875f, 0.021484375f, 4.45f, 3.05f, -2.45f, 0.57421875f, 0.04296875f, 4.45f, 2.35f, -2.45f, 0.421875f, 0.04296875f, 4.45f, 2.35f, 2.45f);
        quad(vc, pose, mat, r, g, b, a, light, -1f, 0f, 0f, 0.421875f, 0.064453125f, -0.45f, 3.05f, -2.45f, 0.57421875f, 0.064453125f, -0.45f, 3.05f, 2.45f, 0.57421875f, 0.0859375f, -0.45f, 2.35f, 2.45f, 0.421875f, 0.0859375f, -0.45f, 2.35f, -2.45f);
        quad(vc, pose, mat, r, g, b, a, light, 0f, 1f, 0f, 0.421875f, 0.0859375f, -0.45f, 3.05f, -2.45f, 0.4375f, 0.0859375f, 4.45f, 3.05f, -2.45f, 0.4375f, 0.1015625f, 4.45f, 3.05f, 2.45f, 0.421875f, 0.1015625f, -0.45f, 3.05f, 2.45f);
        quad(vc, pose, mat, r, g, b, a, light, 0f, -1f, 0f, 0.421875f, 0.1015625f, -0.45f, 2.35f, -2.45f, 0.4375f, 0.1015625f, 4.45f, 2.35f, -2.45f, 0.4375f, 0.1171875f, 4.45f, 2.35f, 2.45f, 0.421875f, 0.1171875f, -0.45f, 2.35f, 2.45f);
        }
        if (calfV && !thighV) { // legwearCuffCalfL
        quad(vc, pose, mat, r, g, b, a, light, 0f, 0f, -1f, 0.421875f, 0.15625f, -0.45f, 6.55f, -2.45f, 0.57421875f, 0.15625f, 4.45f, 6.55f, -2.45f, 0.57421875f, 0.177734375f, 4.45f, 5.85f, -2.45f, 0.421875f, 0.177734375f, -0.45f, 5.85f, -2.45f);
        quad(vc, pose, mat, r, g, b, a, light, 0f, 0f, 1f, 0.421875f, 0.19921875f, 4.45f, 6.55f, 2.45f, 0.57421875f, 0.19921875f, -0.45f, 6.55f, 2.45f, 0.57421875f, 0.220703125f, -0.45f, 5.85f, 2.45f, 0.421875f, 0.220703125f, 4.45f, 5.85f, 2.45f);
        quad(vc, pose, mat, r, g, b, a, light, 1f, 0f, 0f, 0.421875f, 0.177734375f, 4.45f, 6.55f, 2.45f, 0.57421875f, 0.177734375f, 4.45f, 6.55f, -2.45f, 0.57421875f, 0.19921875f, 4.45f, 5.85f, -2.45f, 0.421875f, 0.19921875f, 4.45f, 5.85f, 2.45f);
        quad(vc, pose, mat, r, g, b, a, light, -1f, 0f, 0f, 0.421875f, 0.220703125f, -0.45f, 6.55f, -2.45f, 0.57421875f, 0.220703125f, -0.45f, 6.55f, 2.45f, 0.57421875f, 0.2421875f, -0.45f, 5.85f, 2.45f, 0.421875f, 0.2421875f, -0.45f, 5.85f, -2.45f);
        quad(vc, pose, mat, r, g, b, a, light, 0f, 1f, 0f, 0.421875f, 0.2421875f, -0.45f, 6.55f, -2.45f, 0.4375f, 0.2421875f, 4.45f, 6.55f, -2.45f, 0.4375f, 0.2578125f, 4.45f, 6.55f, 2.45f, 0.421875f, 0.2578125f, -0.45f, 6.55f, 2.45f);
        quad(vc, pose, mat, r, g, b, a, light, 0f, -1f, 0f, 0.421875f, 0.2578125f, -0.45f, 5.85f, -2.45f, 0.4375f, 0.2578125f, 4.45f, 5.85f, -2.45f, 0.4375f, 0.2734375f, 4.45f, 5.85f, 2.45f, 0.421875f, 0.2734375f, -0.45f, 5.85f, 2.45f);
        }

        poseStack.popPose();
    }

    private void quad(VertexConsumer vc, PoseStack.Pose pose, Matrix4f mat, int r, int g, int b, int a, int light,
                      float nx, float ny, float nz,
                      float u1, float v1, float x1, float y1, float z1,
                      float u2, float v2, float x2, float y2, float z2,
                      float u3, float v3, float x3, float y3, float z3,
                      float u4, float v4, float x4, float y4, float z4) {
        vc.addVertex(mat, x1, y1, z1).setColor(r, g, b, a).setUv(u1, v1).setLight(light).setOverlay(0).setNormal(pose, nx, ny, nz);
        vc.addVertex(mat, x2, y2, z2).setColor(r, g, b, a).setUv(u2, v2).setLight(light).setOverlay(0).setNormal(pose, nx, ny, nz);
        vc.addVertex(mat, x3, y3, z3).setColor(r, g, b, a).setUv(u3, v3).setLight(light).setOverlay(0).setNormal(pose, nx, ny, nz);
        vc.addVertex(mat, x4, y4, z4).setColor(r, g, b, a).setUv(u4, v4).setLight(light).setOverlay(0).setNormal(pose, nx, ny, nz);
    }
}
