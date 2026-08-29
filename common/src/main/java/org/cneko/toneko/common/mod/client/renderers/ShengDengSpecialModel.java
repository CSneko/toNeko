package org.cneko.toneko.common.mod.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.items.ShengDengItem;
import org.joml.Vector3fc;

/**
 * 省凳法棍「特殊模型渲染器」。
 * <p>
 * 手持叠加省凳时，按 stack_count 数量在手持方向（Y 轴）程序化画出一根"凳子串"：
 * N 张凳 = N 格长，长度完全动态，不需要预生成分档模型。
 * <p>
 * 几何与 UV 均照抄 sheng_deng_red_stack0.json（凳面 + 4 腿），保证与方块模型视觉一致。
 *
 * <h2>26.x 迁移说明</h2>
 * {@code BlockEntityWithoutLevelRenderer}（BEWLR）渲染路径已被移除，
 * 改为 {@code SpecialModelRenderer<Integer>}：物品模型烘焙后经平台钩子
 * （Fabric: modifyItemModelAfterBake；NeoForge: 客户端扩展）以
 * {@code SpecialModelWrapper} 接入新的提交式物品渲染管线。
 */
public class ShengDengSpecialModel implements net.minecraft.client.renderer.special.SpecialModelRenderer<Integer> {
    public static final ShengDengSpecialModel INSTANCE = new ShengDengSpecialModel();

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("toneko", "block/sheng_deng_red");

    private ShengDengSpecialModel() {
    }

    @Override
    public Integer extractArgument(ItemStack stack) {
        return ShengDengItem.getStackCount(stack);
    }

    @Override
    public void submit(Integer count, PoseStack pose, SubmitNodeCollector collector,
                       int light, int overlay, boolean foil, int seed) {
        if (count == null || count <= 0) return;
        final TextureAtlasSprite sprite = atlasSprite();

        // 自定义几何：提交系统会在正确的渲染通道内回调此 lambda
        collector.submitCustomGeometry(pose, Sheets.translucentItemSheet(), (poseEntry, vc) -> {
            for (int i = 0; i < count; i++) {
                renderStool(vc, poseEntry, light, sprite, i);
            }
        });
    }

    @Override
    public void getExtents(java.util.function.Consumer<Vector3fc> consumer) {
        // 单张凳的最大尺寸约 1x1x1；串长动态增长，这里保守返回单格（GUI 中只显示一张）
        consumer.accept(new org.joml.Vector3f(0.5f, 0.5f, 0.5f));
    }

    /** 图集里取凳面贴图精灵。 */
    private static TextureAtlasSprite atlasSprite() {
        // 26.x：InventoryMenu.BLOCK_ATLAS 常量被移除，图集按“图集定义 id”（AtlasIds.BLOCKS）索引，
        // texture 路径（textures/atlas/blocks.png）旧写法会触发 getAtlasOrThrow 的 IllegalArgumentException。
        var atlasId = Identifier.withDefaultNamespace("blocks");
        return Minecraft.getInstance().getAtlasManager()
                .getAtlasOrThrow(atlasId)
                .getSprite(TEXTURE);
    }

    /** 画一张大排档红凳（凳面 + 4 腿 + 环形横撑 + 防滑脚垫），第 index 张（0 起）沿 Y 偏移 index 格 */
    private static void renderStool(VertexConsumer vc, com.mojang.blaze3d.vertex.PoseStack.Pose pose, int light,
                                    TextureAtlasSprite sprite, int index) {
        float y = index;

        // 凳面 [2,14,2]-[14,16,14]（px），UV 照抄方块模型
        addBox(vc, pose, light, sprite,
                2 / 16f, 14 / 16f + y, 2 / 16f, 14 / 16f, 16 / 16f + y, 14 / 16f,
                uv(2, 2, 14, 14), uv(2, 2, 14, 14),
                uv(2, 14, 14, 16), uv(2, 14, 14, 16),
                uv(2, 14, 14, 16), uv(2, 14, 14, 16));

        // 4 条腿（2x2x13，外角对齐凳面四角，从底座顶到凳面底，UV 照抄方块模型）
        addBox(vc, pose, light, sprite,
                2 / 16f, 1 / 16f + y, 2 / 16f, 4 / 16f, 14 / 16f + y, 4 / 16f,
                uv(2, 1, 4, 14), uv(2, 1, 4, 14),
                uv(2, 1, 4, 14), uv(2, 1, 4, 14),
                uv(2, 1, 4, 14), uv(2, 1, 4, 14));
        addBox(vc, pose, light, sprite,
                12 / 16f, 1 / 16f + y, 2 / 16f, 14 / 16f, 14 / 16f + y, 4 / 16f,
                uv(12, 1, 14, 14), uv(12, 1, 14, 14),
                uv(12, 1, 14, 14), uv(12, 1, 14, 14),
                uv(12, 1, 14, 14), uv(12, 1, 14, 14));
        addBox(vc, pose, light, sprite,
                2 / 16f, 1 / 16f + y, 12 / 16f, 4 / 16f, 14 / 16f + y, 14 / 16f,
                uv(2, 1, 4, 14), uv(2, 1, 4, 14),
                uv(2, 1, 4, 14), uv(2, 1, 4, 14),
                uv(2, 1, 4, 14), uv(2, 1, 4, 14));
        addBox(vc, pose, light, sprite,
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
            addBox(vc, pose, light, sprite,
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
            addBox(vc, pose, light, sprite,
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
    private static void addBox(VertexConsumer vc, com.mojang.blaze3d.vertex.PoseStack.Pose pose, int light, TextureAtlasSprite sprite,
                               float x1, float y1, float z1, float x2, float y2, float z2,
                               float[] uvUp, float[] uvDown,
                               float[] uvNorth, float[] uvSouth, float[] uvEast, float[] uvWest) {
        // up (+Y)：u=x, v=z
        quad(vc, pose, light, sprite,
                x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2, uvUp, 0, 1, 0);
        // down (-Y)：u=x, v=z
        quad(vc, pose, light, sprite,
                x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, uvDown, 0, -1, 0);
        // north (-Z)：u=x, v=y
        quad(vc, pose, light, sprite,
                x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1, uvNorth, 0, 0, -1);
        // south (+Z)：u=x, v=y
        quad(vc, pose, light, sprite,
                x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, uvSouth, 0, 0, 1);
        // east (+X)：u=z, v=y
        quad(vc, pose, light, sprite,
                x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1, uvEast, 1, 0, 0);
        // west (-X)：u=z, v=y
        quad(vc, pose, light, sprite,
                x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, uvWest, -1, 0, 0);
    }

    /** 画一个四边形（4 顶点），UV 按 a=(u1,v1) b=(u2,v1) c=(u2,v2) d=(u1,v2) 铺满，法线 (nx,ny,nz) */
    private static void quad(VertexConsumer vc, com.mojang.blaze3d.vertex.PoseStack.Pose pose, int light, TextureAtlasSprite sprite,
                             float ax, float ay, float az,
                             float bx, float by, float bz,
                             float cx, float cy, float cz,
                             float dx, float dy, float dz,
                             float[] uv, float nx, float ny, float nz) {
        float u1 = sprite.getU(uv[0]);
        float v1 = sprite.getV(uv[1]);
        float u2 = sprite.getU(uv[2]);
        float v2 = sprite.getV(uv[3]);

        // 26.x：Pose 自身即变换载体（pose()/normal()）
        // NEW_ENTITY 格式要求写满 6 个元素：POSITION/COLOR/TEX/OVERLAY/LIGHTMAP/NORMAL
        vc.addVertex(pose, ax, ay, az).setColor(1f, 1f, 1f, 1f).setUv(u1, v1).setLight(light).setOverlay(0).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, bx, by, bz).setColor(1f, 1f, 1f, 1f).setUv(u2, v1).setLight(light).setOverlay(0).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, cx, cy, cz).setColor(1f, 1f, 1f, 1f).setUv(u2, v2).setLight(light).setOverlay(0).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, dx, dy, dz).setColor(1f, 1f, 1f, 1f).setUv(u1, v2).setLight(light).setOverlay(0).setNormal(pose, nx, ny, nz);
    }
}
