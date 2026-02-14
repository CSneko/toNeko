package org.cneko.toneko.common.mod.recipes.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.cneko.toneko.common.mod.recipes.NekoAggregatorRecipe;

import java.util.List;

public class NekoAggregatorEmiRecipe extends BasicEmiRecipe {

    private final double energy;
    private static final int COST_COLOR_CONSUME = 0xFFFF5555; // 红色
    private static final int COST_COLOR_GAIN = 0xFF55FF55;    // 绿色

    public NekoAggregatorEmiRecipe(EmiRecipeCategory category, RecipeHolder<NekoAggregatorRecipe> holder) {
        super(category, holder.id(), 150, 60); // 设置宽度150，高度60
        NekoAggregatorRecipe recipe = holder.value();
        this.energy = recipe.energy;

        // 设置输入 (假设 Pattern 是标准的 3x3 或者可以通过 list 获取)
        // 注意：这里需要确保按照 3x3 的顺序填充，不够的补空
        List<Ingredient> ingredients = recipe.pattern.ingredients();
        for (Ingredient ingredient : ingredients) {
            this.inputs.add(EmiIngredient.of(ingredient));
        }

        // 设置输出
        this.outputs.add(EmiStack.of(recipe.getResultItem(null)));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        // 1. 添加 3x3 输入槽位 (位于左侧)
        // 坐标计算：从 (0, 2) 开始，为了垂直居中一点
        int gridX = 0;
        int gridY = 2;
        int width = 3; // 3列

        for (int i = 0; i < inputs.size(); i++) {
            int x = gridX + (i % width) * 18;
            int y = gridY + (i / width) * 18;
            widgets.addSlot(inputs.get(i), x, y);
        }

        // 2. 添加中间的箭头 (模仿原版工作台位置，在网格右侧)
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 60, 20);

        // 3. 添加输出槽位 (大尺寸，带配方上下文)
        widgets.addSlot(outputs.get(0), 90, 16).recipeContext(this).large(true);

        // 4. 添加自定义能量条组件 (位于最右侧)
        int barX = 130;
        int barY = 2;
        int barW = 8;
        int barH = 54;

        // 5. 添加能量数值文本 (类似 Screen 中的 renderCostText)
        // 根据正负值决定显示的文本
        String text = (energy > 0 ? "-" : "+") + (int)Math.abs(energy);
        int color = energy > 0 ? COST_COLOR_CONSUME : COST_COLOR_GAIN;

        // 将文本绘制在能量条左侧或者上方，这里放在箭头下方稍微错开的位置，防止遮挡
        // 或者模仿 Screen 放在能量条上面/旁边。
        // EMI 中 widgets.addText 会自动处理 layer
        widgets.addText(Component.literal(text), barX - 5, barY + barH - 10, color, true);
    }

    /**
     * 从 NekoAggregatorScreen 移植过来的渲染逻辑
     */
    private void renderEnergyBar(GuiGraphics guiGraphics, int x, int y, int width, int height, float current, float max) {
        // 绘制背景 (半透明黑色)
        guiGraphics.fill(x, y, x + width, y + height, 0x80000000);

        if (max <= 0) return;

        // 计算填充高度
        float ratio = Mth.clamp(current / max, 0.0f, 1.0f);
        int fillHeight = (int) (height * ratio);

        int bottomY = y + height;

        // 绘制渐变能量条 (从下往上)
        for (int i = 0; i < fillHeight; i++) {
            float progress = (float) i / height;

            // 渐变逻辑: 暗绿色 -> 亮绿色
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
        guiGraphics.renderOutline(x - 1, y - 1, width + 2, height + 2, 0xFF000000);
    }
}