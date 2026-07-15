package org.cneko.toneko.neoforge.client.msic;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.cneko.toneko.common.mod.recipes.NekoAggregatorRecipe;
import org.cneko.toneko.common.mod.recipes.ToNekoRecipes;
import org.cneko.toneko.common.mod.recipes.emi.NekoAggregatorEmiRecipe;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

@EmiEntrypoint
public class ToNekoEmiPlugin implements EmiPlugin {

    public static final ResourceLocation NEKO_AGGREGATOR_ID = toNekoLoc("neko_aggregator");

    @Override
    public void register(EmiRegistry registry) {
        EmiStack workstation = EmiStack.of(Items.CRAFTING_TABLE);
        EmiRecipeCategory category = new EmiRecipeCategory(NEKO_AGGREGATOR_ID, workstation, workstation);
        registry.addCategory(category);
        registry.addWorkstation(category, workstation);

        RecipeManager manager = registry.getRecipeManager();
        for (RecipeHolder<NekoAggregatorRecipe> holder : manager.getAllRecipesFor(ToNekoRecipes.NEKO_AGGREGATOR)) {
            registry.addRecipe(new NekoAggregatorEmiRecipe(category, holder));
        }
    }
}
