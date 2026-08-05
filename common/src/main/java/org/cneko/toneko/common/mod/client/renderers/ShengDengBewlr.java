package org.cneko.toneko.common.mod.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.items.ShengDengItem;

/**
 * 省凳法棍渲染器。
 * <p>
 * 手持叠加省凳时，按 stack_count 数量在手持方向（Y 轴）程序化画出一根"凳子串"：
 * N 张凳 = N 格长，长度完全动态，不需要预生成分档模型。
 * <p>
 * 触发链路：物品模型 parent 为 builtin/entity → BakedModel.isCustomRenderer() = true
 * → ItemRenderer 走 BEWLR 路径。
 * Fabric 通过 BuiltinItemRendererRegistry 注册，NeoForge 通过 IClientItemExtensions。
 * <p>
 * 几何与 UV 均照抄 sheng_deng_red_stack0.json（凳面 + 4 腿），保证与方块模型视觉一致。
 */
public class ShengDengBewlr extends BlockEntityWithoutLevelRenderer {
    public static final ShengDengBewlr INSTANCE = new ShengDengBewlr();

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("toneko", "block/sheng_deng_red");

    public ShengDengBewlr() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        int count = ShengDengItem.getStackCount(stack);
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(TEXTURE);
        // 必须用物品渲染通道（format = NEW_ENTITY，含 OVERLAY/NORMAL 元素）：
        // cutout 的 format 只有 POSITION/COLOR/TEX/LIGHTMAP，setOverlay/setNormal 会让顶点元素状态错乱崩溃
        VertexConsumer vc = buffers.getBuffer(Sheets.translucentItemSheet());

        pose.pushPose();
        for (int i = 0; i < count; i++) {
            renderStool(vc, pose, light, overlay, sprite, i);
        }
        pose.popPose();
    }

    /** 画一张大排档红凳（凳面 + 4 腿 + 环形横撑 + 防滑脚垫），第 index 张（0 起）沿 Y 偏移 index 格 */
    private static void renderStool(VertexConsumer vc, PoseStack pose, int light, int overlay,
                                    TextureAtlasSprite sprite, int index) {
        float y = index;

        // 凳面 [2,14,2]-[14,16,14]（px），UV 照抄方块模型
        addBox(vc, pose, light, overlay, sprite,
                2 / 16f, 14 / 16f + y, 2 / 16f, 14 / 16f, 16 / 16f + y, 14 / 16f,
                uv(2, 2, 14, 14), uv(2, 2, 14, 14),
                uv(2, 14, 14, 16), uv(2, 14, 14, 16),
                uv(2, 14, 14, 16), uv(2, 14, 14, 16));

        // 4 条腿（2x2x13，外角对齐凳面四角，从底座顶到凳面底，UV 照抄方块模型）
        addBox(vc, pose, light, overlay, sprite,
                2 / 16f, 1 / 16f + y, 2 / 16f, 4 / 16f, 14 / 16f + y, 4 / 16f,
                uv(2, 1, 4, 14), uv(2, 1, 4, 14),
                uv(2, 1, 4, 14), uv(2, 1, 4, 14),
                uv(2, 1, 4, 14), uv(2, 1, 4, 14));
        addBox(vc, pose, light, overlay, sprite,
                12 / 16f, 1 / 16f + y, 2 / 16f, 14 / 16f, 14 / 16f + y, 4 / 16f,
                uv(12, 1, 14, 14), uv(12, 1, 14, 14),
                uv(12, 1, 14, 14), uv(12, 1, 14, 14),
                uv(12, 1, 14, 14), uv(12, 1, 14, 14));
        addBox(vc, pose, light, overlay, sprite,
                2 / 16f, 1 / 16f + y, 12 / 16f, 4 / 16f, 14 / 16f + y, 14 / 16f,
                uv(2, 1, 4, 14), uv(2, 1, 4, 14),
                uv(2, 1, 4, 14), uv(2, 1, 4, 14),
                uv(2, 1, 4, 14), uv(2, 1, 4, 14));
        addBox(vc, pose, light, overlay, sprite,
                12 / 16f, 1 / 16f + y, 12 / 16f, 14 / 16f, 14 / 16f + y, 14 / 16f,
                uv(12, 1, 14, 14), uv(12, 1, 14, 14),
                uv(12, 1, 14, 14), uv(12, 1, 14, 14),
                uv(12, 1, 14, 14), uv(12, 1, 14, 14));

        // 4 个底座（3x3x1，贴地，与凳身同色，包住腿底）
        float[][] pads = {
                {1.5f, 1.5f, 4.5f, 4.5f, 1.5f, 0f, 4.5f, 1f},
                {11.5f, 1.5f, 14.5f, 4.5f, 11.5f, 0f, 14.5f, 1f},
                {1.5f, 11.5f, 4.5f, 14.5f, 1.5f, 0f, 4.5f, 1f},
                {11.5f, 11.5f, 14.5f, 14.5f, 11.5f, 0f, 14.5f, 1f},
        };
        for (float[] p : pads) {
            addBox(vc, pose, light, overlay, sprite,
                    p[0] / 16f, p[5] / 16f + y, p[1] / 16f, p[2] / 16f, p[6] / 16f + y, p[3] / 16f,
                    uv(p[4], p[5], p[6], p[7]), uv(p[4], p[5], p[6], p[7]),
                    uv(p[4], p[5], p[6], p[7]), uv(p[4], p[5], p[6], p[7]),
                    uv(p[4], p[5], p[6], p[7]), uv(p[4], p[5], p[6], p[7]));
        }

        // 4 条环形横撑（1px 厚，腿中部，连接相邻腿）
        float[][] beams = {
                {2f, 4f, 4f, 12f, 2f, 7f, 4f, 8f},
                {12f, 4f, 14f, 12f, 12f, 7f, 14f, 8f},
                {4f, 2f, 12f, 4f, 4f, 7f, 12f, 8f},
                {4f, 12f, 12f, 14f, 4f, 7f, 12f, 8f},
        };
        for (float[] b : beams) {
            addBox(vc, pose, light, overlay, sprite,
                    b[0] / 16f, b[5] / 16f + y, b[1] / 16f, b[2] / 16f, b[6] / 16f + y, b[3] / 16f,
                    uv(b[4], b[5], b[6], b[7]), uv(b[4], b[5], b[6], b[7]),
                    uv(b[4], b[5], b[6], b[7]), uv(b[4], b[5], b[6], b[7]),
                    uv(b[4], b[5], b[6], b[7]), uv(b[4], b[5], b[6], b[7]));
        }
    }

    private static float[] uv(float u1, float v1, float u2, float v2) {
        return new float[]{u1, v1, u2, v2};
    }

    /**
     * 画一个轴对齐盒子：6 个面，每面独立 UV（16x16 贴图坐标），法线朝外。
     * 顶点顺序保证 UV 沿面正方向递增：up/down 面 u=x,v=z；north/south 面 u=x,v=y；
     * east/west 面 u=z,v=y（与方块模型的 UV 映射一致，避免贴图颠倒）。
     */
    private static void addBox(VertexConsumer vc, PoseStack pose, int light, int overlay, TextureAtlasSprite sprite,
                               float x1, float y1, float z1, float x2, float y2, float z2,
                               float[] uvUp, float[] uvDown,
                               float[] uvNorth, float[] uvSouth, float[] uvEast, float[] uvWest) {
        // up (+Y)：u=x, v=z
        quad(vc, pose, light, overlay, sprite,
                x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2, uvUp, 0, 1, 0);
        // down (-Y)：u=x, v=z
        quad(vc, pose, light, overlay, sprite,
                x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, uvDown, 0, -1, 0);
        // north (-Z)：u=x, v=y
        quad(vc, pose, light, overlay, sprite,
                x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1, uvNorth, 0, 0, -1);
        // south (+Z)：u=x, v=y
        quad(vc, pose, light, overlay, sprite,
                x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, uvSouth, 0, 0, 1);
        // east (+X)：u=z, v=y
        quad(vc, pose, light, overlay, sprite,
                x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1, uvEast, 1, 0, 0);
        // west (-X)：u=z, v=y
        quad(vc, pose, light, overlay, sprite,
                x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, uvWest, -1, 0, 0);
    }

    /** 画一个四边形（4 顶点），UV 按 a=(u1,v1) b=(u2,v1) c=(u2,v2) d=(u1,v2) 铺满，法线 (nx,ny,nz) */
    private static void quad(VertexConsumer vc, PoseStack pose, int light, int overlay, TextureAtlasSprite sprite,
                             float ax, float ay, float az,
                             float bx, float by, float bz,
                             float cx, float cy, float cz,
                             float dx, float dy, float dz,
                             float[] uv, float nx, float ny, float nz) {
        float u1 = sprite.getU(uv[0]);
        float v1 = sprite.getV(uv[1]);
        float u2 = sprite.getU(uv[2]);
        float v2 = sprite.getV(uv[3]);

        var last = pose.last();
        // NEW_ENTITY 格式要求写满 6 个元素：POSITION/COLOR/TEX/OVERLAY/LIGHTMAP/NORMAL
        vc.addVertex(last.pose(), ax, ay, az).setColor(1f, 1f, 1f, 1f).setUv(u1, v1).setLight(light).setOverlay(overlay).setNormal(last, nx, ny, nz);
        vc.addVertex(last.pose(), bx, by, bz).setColor(1f, 1f, 1f, 1f).setUv(u2, v1).setLight(light).setOverlay(overlay).setNormal(last, nx, ny, nz);
        vc.addVertex(last.pose(), cx, cy, cz).setColor(1f, 1f, 1f, 1f).setUv(u2, v2).setLight(light).setOverlay(overlay).setNormal(last, nx, ny, nz);
        vc.addVertex(last.pose(), dx, dy, dz).setColor(1f, 1f, 1f, 1f).setUv(u1, v2).setLight(light).setOverlay(overlay).setNormal(last, nx, ny, nz);
    }
}
