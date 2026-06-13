package org.cneko.toneko.common.mod.client.emi;

import dev.emi.emi.api.EmiEntrypoint;
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

@EmiEntrypoint
public class ToNekoEmiPlugin implements EmiPlugin {

    public static final ResourceLocation NEKO_AGGREGATOR_ID = toNekoLoc("neko_aggregator");

    @Override
    public void register(EmiRegistry registry) {
        // 延迟创建 EmiStack，避免 DeferredRegister 未解析时 item 为 null
        EmiStack workstation = EmiStack.of(ToNekoItems.NEKO_AGGREGATOR_ITEM);
        EmiRecipeCategory category = new EmiRecipeCategory(NEKO_AGGREGATOR_ID, workstation, workstation);

        // 1. 注册分类
        registry.addCategory(category);

        // 2. 注册工作站
        registry.addWorkstation(category, workstation);

        // 3. 获取 RecipeManager 并添加所有配方
        RecipeManager manager = registry.getRecipeManager();
        for (RecipeHolder<NekoAggregatorRecipe> holder : manager.getAllRecipesFor(ToNekoRecipes.NEKO_AGGREGATOR)) {
            registry.addRecipe(new NekoAggregatorEmiRecipe(category, holder));
        }

        // 4. 注册配方处理器
        registry.addRecipeHandler(ToNekoMenuTypes.NEKO_AGGREGATOR, new StandardRecipeHandler<>() {
            @Override
            public List<Slot> getCraftingSlots(NekoAggregatorBlock.NekoAggregatorMenu handler) {
                return handler.slots.subList(0, 9);
            }

            @Override
            public List<Slot> getInputSources(NekoAggregatorBlock.NekoAggregatorMenu handler) {
                int inventoryStartIndex = 9;
                int totalSlots = handler.slots.size();
                if (inventoryStartIndex < totalSlots) {
                    return handler.slots.subList(inventoryStartIndex, totalSlots);
                }
                return List.of();
            }

            @Override
            public boolean supportsRecipe(EmiRecipe recipe) {
                return recipe instanceof NekoAggregatorEmiRecipe;
            }
        });
    }
}
