package org.cneko.toneko.common.mod.client.events;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.effects.ToNekoEffects;
import org.joml.Matrix4f;

import java.io.IOException;

import static org.cneko.toneko.common.Bootstrap.LOGGER;
import static org.cneko.toneko.common.Bootstrap.MODID;
import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

public class HudRenderEvent {

    public static void init() {
        HudRenderCallback.EVENT.register((guiGraphics, deltaTracker) -> {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;
            renderNekoEnergyBar(guiGraphics);
            // 检查玩家是否有魅惑效果
            if (player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ToNekoEffects.BEWITCHED_EFFECT))) {
                renderBewitchedOverlay(guiGraphics,player);
            }
        });
    }

    /**
     * 渲染魅惑效果的粉色晕影覆盖层。
     * 此效果没有纹理，而是通过在屏幕边缘绘制带颜色梯度的顶点来动态生成。
     *
     * @param guiGraphics GuiGraphics 实例
     * @param player      玩家实体
     */
    private static void renderBewitchedOverlay(GuiGraphics guiGraphics, Player player) {
        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();

        // --- 渲染状态设置 ---
        RenderSystem.disableDepthTest(); // 在所有东西之上绘制
        RenderSystem.depthMask(false);   // 不写入深度缓冲区
        RenderSystem.enableBlend();      // 启用混合以实现半透明
        // 设置标准的alpha混合函数
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        // 使用仅处理位置和颜色的基础着色器
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // --- 顶点数据准备 ---
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // 定义颜色和透明度
        int r = 255, g = 105, b = 180; // 靓粉色
        int edgeAlpha = 120; // 屏幕边缘的透明度
        float z = 0.0f; // 2D屏幕空间的Z坐标

        // 定义晕影的内部边界（完全透明的区域）
        float innerX1 = width * 0.25f;
        float innerY1 = height * 0.25f;
        float innerX2 = width * 0.75f;
        float innerY2 = height * 0.75f;

        // 我们将晕影绘制为环绕中心透明区域的四个梯形（四边形）
        // 颜色将从边缘（有alpha）渐变到内部（alpha为0）

        // 顶部四边形
        bufferBuilder.addVertex(0, innerY1, z).setColor(r, g, b, 0);
        bufferBuilder.addVertex(width, innerY1, z).setColor(r, g, b, 0);
        bufferBuilder.addVertex(width, 0, z).setColor(r, g, b, edgeAlpha);
        bufferBuilder.addVertex(0, 0, z).setColor(r, g, b, edgeAlpha);

        // 底部四边形
        bufferBuilder.addVertex(0, height, z).setColor(r, g, b, edgeAlpha);
        bufferBuilder.addVertex(width, height, z).setColor(r, g, b, edgeAlpha);
        bufferBuilder.addVertex(width, innerY2, z).setColor(r, g, b, 0);
        bufferBuilder.addVertex(0, innerY2, z).setColor(r, g, b, 0);

        // 左侧四边形（高度覆盖整个屏幕）
        bufferBuilder.addVertex(0, height, z).setColor(r, g, b, edgeAlpha);
        bufferBuilder.addVertex(innerX1, height, z).setColor(r, g, b, 0);
        bufferBuilder.addVertex(innerX1, 0, z).setColor(r, g, b, 0);
        bufferBuilder.addVertex(0, 0, z).setColor(r, g, b, edgeAlpha);

        // 右侧四边形（高度覆盖整个屏幕）
        bufferBuilder.addVertex(innerX2, height, z).setColor(r, g, b, 0);
        bufferBuilder.addVertex(width, height, z).setColor(r, g, b, edgeAlpha);
        bufferBuilder.addVertex(width, 0, z).setColor(r, g, b, edgeAlpha);
        bufferBuilder.addVertex(innerX2, 0, z).setColor(r, g, b, 0);

        // --- 绘制 ---
        // 构建网格数据并使用着色器进行绘制
        MeshData mesh = bufferBuilder.build();
        if (mesh != null) {
            // BufferUploader会处理VBO上传和绘制调用
            BufferUploader.drawWithShader(mesh);
        }

        // --- 恢复渲染状态 ---
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }




    private static final ResourceLocation CATNIP_ICON = ResourceLocation.fromNamespaceAndPath(MODID,"textures/item/catnip.png");
    public static void renderNekoEnergyBar(GuiGraphics context) {
        Minecraft client = Minecraft.getInstance();
        if (client.options.hideGui) return;

        Player player = client.player;

        float nekoEnergy = player.getNekoEnergy();
        float maxNekoEnergy = player.getMaxNekoEnergy();

        // 如果能量是满的，则隐藏
        if (nekoEnergy >= maxNekoEnergy) return;

        // 获取屏幕尺寸
        int width = context.guiWidth();
        int height = context.guiHeight();

        // 设置位置和尺寸
        int barWidth = 91;
        int barHeight = 4;
        int iconSize = 8;
        int margin = 10;
        int x = width - margin - barWidth;
        int y = height - margin - barHeight;

        // 计算能量百分比
        float percent = nekoEnergy / maxNekoEnergy;
        int energyWidth = (int)(barWidth * percent);

        // 绘制背景条
        context.fill(
                x, y,
                x + barWidth, y + barHeight,
                0x80000000 // 半透明黑色背景
        );

        // 绘制渐变能量条（绿色调）
        for (int i = 0; i < energyWidth; i++) {
            float progress = (float)i / barWidth;
            // 渐变从暗绿色(100,200,0)到亮绿色(150,255,100)
            int red = (int)(100 * (1 - progress) + 150 * progress);
            int green = (int)(200 * (1 - progress) + 255 * progress);
            int blue = (int)(0 * (1 - progress) + 100 * progress);
            int color = (0xFF << 24) | (red << 16) | (green << 8) | blue;

            context.fill(
                    x + i, y,
                    x + i + 1, y + barHeight,
                    color
            );
        }

        // 绘制猫薄荷图标（在能量条左侧）
        context.blit(CATNIP_ICON,
                x - iconSize - 2,  // 放在能量条左边，间隔2像素
                y - (iconSize - barHeight) / 2,
                0, 0,
                iconSize, iconSize,
                iconSize, iconSize);
    }
}