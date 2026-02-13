package org.cneko.toneko.common.mod.client.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
    private static final ResourceLocation CRAFTING_TABLE_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/crafting_table.png");

    // 颜色定义
    private static final int COST_COLOR_CONSUME = 0xFFFF5555; // 红色 (-消耗)
    private static final int COST_COLOR_GAIN = 0xFF55FF55;    // 绿色 (+获得)
    private static final int WARNING_COLOR = 0xFFFF0000;      // 警告符号颜色

    public NekoAggregatorScreen(NekoAggregatorBlock.NekoAggregatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 20;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. 渲染背景 (包含能量条和文字)
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 2. 渲染物品悬停提示 (原版逻辑)
        this.renderTooltip(guiGraphics, mouseX, mouseY);

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
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 1. 渲染工作台纹理
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(CRAFTING_TABLE_LOCATION, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 2. 获取数据
        float currentEnergy = this.menu.player.getNekoEnergy();
        float maxEnergy = this.menu.player.getMaxNekoEnergy();
        int requiredEnergy = getClientSideRequiredEnergy();

        // 3. 渲染能量条 (渐变)
        renderEnergyBar(guiGraphics, x + 158, y + 17, 8, 54, currentEnergy, maxEnergy);

        // 4. 渲染数值文本 (-Cost 或 +Gain) 和警告
        renderCostText(guiGraphics, x, y, currentEnergy, requiredEnergy);
    }

    private void renderEnergyBar(GuiGraphics guiGraphics, int x, int y, int width, int height, float current, float max) {
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
        guiGraphics.renderOutline(x - 1, y - 1, width + 2, height + 2, 0xFF000000);
    }

    private void renderCostText(GuiGraphics guiGraphics, int x, int y, float currentEnergy, int requiredEnergy) {
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

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(textX, textY, 200); // z=200 确保在最上层
        poseStack.scale(0.7f, 0.7f, 1.0f);
        guiGraphics.drawString(this.font, text, 0, 0, color, true);
        poseStack.popPose();

        // 如果能量不足，在箭头处画一个叹号
        if (isNotEnough) {
            int arrowX = x + 90;
            int arrowY = y + 35;
            guiGraphics.drawString(this.font, "!", arrowX + 5, arrowY - 5, WARNING_COLOR, false);
        }
    }

    private void renderEnergyTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        float current = this.menu.player.getNekoEnergy();
        float max = this.menu.player.getMaxNekoEnergy();
        int required = getClientSideRequiredEnergy();

        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(String.format("Neko Energy: %d / %d", (int)current, (int)max)));

        if (required > 0) {
            int color = (current >= required) ? 0xFFAAAAAA : 0xFFFF5555;
            tooltip.add(Component.literal("Cost: " + required).withStyle(s -> s.withColor(color)));
        } else if (required < 0) {
            tooltip.add(Component.literal("Gain: " + Math.abs(required)).withStyle(s -> s.withColor(0xFF55FF55)));
        }

        guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
    }

    private int getClientSideRequiredEnergy() {
        if (this.minecraft == null || this.minecraft.level == null) return 0;

        List<ItemStack> inputs = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            inputs.add(this.menu.getSlot(i).getItem());
        }

        NekoAggregatorInput input = NekoAggregatorInput.of(3, 3, inputs, 0);

        Optional<RecipeHolder<NekoAggregatorRecipe>> recipe = this.minecraft.level.getRecipeManager()
                .getRecipeFor(ToNekoRecipes.NEKO_AGGREGATOR, input, this.minecraft.level);

        // 使用 map 获取值，如果是 null (没配方) 默认为 0
        return recipe.map(holder -> holder.value().energy).orElse(0d).intValue();
    }
}