package org.cneko.toneko.common.mod.client.screens;
import org.cneko.toneko.common.mod.entities.INeko;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.cneko.toneko.common.mod.blocks.NekoAggregatorBlock;
import org.cneko.toneko.common.mod.recipes.NekoAggregatorInput;
import org.cneko.toneko.common.mod.recipes.NekoAggregatorRecipe;
import org.cneko.toneko.common.mod.recipes.ToNekoRecipes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NekoAggregatorScreen extends AbstractContainerScreen<NekoAggregatorBlock.NekoAggregatorMenu> {
    private static final Identifier CRAFTING_TABLE_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/crafting_table.png");

    // 颜色定义
    private static final int COST_COLOR_CONSUME = 0xFFFF5555; // 红色 (-消耗)
    private static final int COST_COLOR_GAIN = 0xFF55FF55;    // 绿色 (+获得)
    private static final int WARNING_COLOR = 0xFFFF0000;      // 警告符号颜色

    public NekoAggregatorScreen(NekoAggregatorBlock.NekoAggregatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166); // 26.x：imageWidth/Height 改为 final，经构造器传入
        this.titleLabelX = 20;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. 渲染背景 (包含能量条和文字)
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        // 2. 渲染物品悬停提示 (原版逻辑)
        this.extractTooltip(guiGraphics, mouseX, mouseY);

        // 3. 渲染能量条的悬停提示 (自定义逻辑)
        // 放在这里是为了保证 Tooltip 绘制在所有图层最上方
        int barX = this.leftPos + 158;
        int barY = this.topPos + 17;
        int barWidth = 8;
        int barHeight = 54;

        if (isHovering(barX - this.leftPos, barY - this.topPos, barWidth, barHeight, mouseX, mouseY)) {
            renderEnergyTooltip(guiGraphics, mouseX, mouseY);
        }
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 26.1.2：界面贴图应在 extractBackground 中绘制（同原版 CraftingScreen）
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, CRAFTING_TABLE_LOCATION,
                this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    public void extractContents(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 26.1.2：extractContents 负责标题标签 + 全部槽位物品（含玩家背包），必须先调 super
        super.extractContents(guiGraphics, mouseX, mouseY, partialTick);

        // 叠加自定义内容：能量条与消耗文本
        int x = this.leftPos;
        int y = this.topPos;
        float currentEnergy = ((INeko) this.menu.player).getNekoEnergy();
        float maxEnergy = ((INeko) this.menu.player).getMaxNekoEnergy();
        int requiredEnergy = getClientSideRequiredEnergy();

        renderEnergyBar(guiGraphics, x + 158, y + 17, 8, 54, currentEnergy, maxEnergy);
        renderCostText(guiGraphics, x, y, currentEnergy, requiredEnergy);
    }

    private void renderEnergyBar(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, float current, float max) {
        // 绘制背景 (半透明黑色)
        guiGraphics.fill(x, y, x + width, y + height, 0x80000000);

        if (max <= 0) return;

        // 计算填充高度
        float ratio = Mth.clamp(current / max, 0.0f, 1.0f);
        int fillHeight = (int) (height * ratio);

        // 绘制渐变能量条 (从下往上)
        // 这里适配了你提供的水平渐变逻辑改为垂直渐变
        int bottomY = y + height;

        for (int i = 0; i < fillHeight; i++) {
            float progress = (float) i / height; // 注意：这里用总高度做分母，让渐变色对应绝对位置

            // 渐变逻辑:
            // 暗绿色(100,200,0) -> 亮绿色(150,255,100)
            int red = (int) (100 * (1 - progress) + 150 * progress);
            int green = (int) (200 * (1 - progress) + 255 * progress);
            int blue = (int) (0 * (1 - progress) + 100 * progress);
            int color = (0xFF << 24) | (red << 16) | (green << 8) | blue;

            // 绘制 1px 高的横线
            guiGraphics.fill(
                    x,
                    bottomY - i - 1,
                    x + width,
                    bottomY - i,
                    color
            );
        }

        // 绘制边框
        guiGraphics.outline(x - 1, y - 1, width + 2, height + 2, 0xFF000000);
    }

    private void renderCostText(GuiGraphicsExtractor guiGraphics, int x, int y, float currentEnergy, int requiredEnergy) {
        // 只有当配方需要能量(>0) 或者 产生能量(<0) 时才显示
        // 如果是0，通常也可以显示 +0

        String text;
        int color;
        boolean isNotEnough = false;

        if (requiredEnergy > 0) {
            // 消耗能量
            text = "-" + requiredEnergy;
            // 检查是否足够
            if (currentEnergy < requiredEnergy) {
                color = COST_COLOR_CONSUME; // 红色
                isNotEnough = true;
            } else {
                color = COST_COLOR_CONSUME; // 依然显示红色表示这是“扣除”，但在足够时也许你想用灰色？按需求描述维持红色
            }
        } else {
            // 产生能量 (负数消耗 = 增加) 或 0
            text = "+" + Math.abs(requiredEnergy);
            color = COST_COLOR_GAIN; // 绿色
        }

        // 渲染文本
        // 放在能量条左侧一点
        int textX = x + 158 + 10;
        int textY = y + 17;

        // 26.x GUI：姿态栈为 Matrix3x2fStack（无 z 轴），缩放 + 平移组合实现同样的缩小文字
        var pose = guiGraphics.pose();
        pose.pushMatrix();
        pose.translate(textX, textY);
        pose.scale(0.7f, 0.7f);
        guiGraphics.text(this.font, text, 0, 0, color, true);
        pose.popMatrix();

        // 如果能量不足，在箭头处画一个叹号
        if (isNotEnough) {
            int arrowX = x + 90;
            int arrowY = y + 35;
            guiGraphics.text(this.font, "!", arrowX + 5, arrowY - 5, WARNING_COLOR, false);
        }
    }

    private void renderEnergyTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        float current = ((INeko) this.menu.player).getNekoEnergy();
        float max = ((INeko) this.menu.player).getMaxNekoEnergy();
        int required = getClientSideRequiredEnergy();

        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(String.format("%d / %d", (int)current, (int)max)));

        if (required > 0) {
            int color = (current >= required) ? 0xFFAAAAAA : 0xFFFF5555;
            tooltip.add(Component.literal("-" + required).withStyle(s -> s.withColor(color)));
        } else if (required < 0) {
            tooltip.add(Component.literal("+" + Math.abs(required)).withStyle(s -> s.withColor(0xFF55FF55)));
        }

        guiGraphics.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
    }

    private int getClientSideRequiredEnergy() {
        // 26.1.2 客户端不再持有完整配方表（level.recipeAccess() 返回 ClientRecipeContainer），
        // 所需能量由服务端在 updateResult 中写入容器数据槽并自动同步
        return this.menu.getRequiredEnergy();
    }
}
