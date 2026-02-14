package org.cneko.toneko.fabric.client.msic;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.cneko.toneko.common.mod.blocks.NekoAggregatorBlock;
import org.cneko.toneko.common.mod.items.ToNekoItems;
import org.cneko.toneko.common.mod.recipes.NekoAggregatorRecipe;
import org.cneko.toneko.common.mod.recipes.ToNekoMenuTypes;
import org.cneko.toneko.common.mod.recipes.ToNekoRecipes;
import org.cneko.toneko.common.mod.recipes.emi.NekoAggregatorEmiRecipe;

import java.util.List;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;


public class ToNekoEmiPlugin implements EmiPlugin {

    // 定义分类的 ID
    public static final ResourceLocation NEKO_AGGREGATOR_ID = toNekoLoc("neko_aggregator");

    // 定义工作站图标 (请替换为你的实际物品)
    public static final EmiStack WORKSTATION = EmiStack.of(ToNekoItems.NEKO_AGGREGATOR_ITEM);

    // 定义分类
    // 这里的 EmiTexture 指向该分类在 EMI 界面显示的图标，这里简单使用了工作站本身作为图标
    public static final EmiRecipeCategory NEKO_AGGREGATOR_CATEGORY = new EmiRecipeCategory(NEKO_AGGREGATOR_ID, WORKSTATION, WORKSTATION);

    @Override
    public void register(EmiRegistry registry) {
        // 1. 注册分类
        registry.addCategory(NEKO_AGGREGATOR_CATEGORY);

        // 2. 注册工作站 (点击这个方块可以查看该类别的配方)
        registry.addWorkstation(NEKO_AGGREGATOR_CATEGORY, WORKSTATION);

        // 3. 获取 RecipeManager 并添加所有配方
        RecipeManager manager = registry.getRecipeManager();

        // 遍历所有该类型的配方
        // 注意：manager.getAllRecipesFor 返回的是 List<RecipeHolder<T>>
        for (RecipeHolder<NekoAggregatorRecipe> holder : manager.getAllRecipesFor(ToNekoRecipes.NEKO_AGGREGATOR)) {
            registry.addRecipe(new NekoAggregatorEmiRecipe(NEKO_AGGREGATOR_CATEGORY, holder));
        }


        registry.addRecipeHandler(ToNekoMenuTypes.NEKO_AGGREGATOR, new StandardRecipeHandler<>() {

            // 这里返回 Menu 中所有的“输入槽位”
            // EMI 会按顺序将配方的第1个原料填入这里的第1个槽位，以此类推
            @Override
            public List<Slot> getCraftingSlots(NekoAggregatorBlock.NekoAggregatorMenu handler) {

                // 写法 A：如果前9个就是输入槽
                return handler.slots.subList(0, 9);

                // 写法 B (更安全，如果不确定顺序)：
                // List<Slot> inputs = new ArrayList<>();
                // inputs.add(handler.getSlot(0));
                // ...
                // inputs.add(handler.getSlot(8));
                // return inputs;
            }

            // 这里返回玩家背包的槽位 (用于 EMI 检查玩家是否有足够的材料)
            // 也是 EMI 在清空网格时将物品移回的地方
            @Override
            public List<Slot> getInputSources(NekoAggregatorBlock.NekoAggregatorMenu handler) {
                // 输入是 0-8，输出是 9，玩家背包从 10 开始
                int inventoryStartIndex = 9;
                int totalSlots = handler.slots.size();

                if (inventoryStartIndex < totalSlots) {
                    return handler.slots.subList(inventoryStartIndex, totalSlots);
                }
                return List.of(); // 防止越界
            }

            // 告诉 EMI 这个 Handler 只处理你自己的配方类型
            @Override
            public boolean supportsRecipe(EmiRecipe recipe) {
                return recipe instanceof NekoAggregatorEmiRecipe;
            }
        });
    }
}
