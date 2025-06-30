package org.cneko.toneko.neoforge.msic;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.cneko.toneko.common.mod.recipes.NekoAggregatorRecipe;

import static org.cneko.toneko.common.mod.recipes.ToNekoRecipes.*;
import static org.cneko.toneko.neoforge.ToNekoNeoForge.RECIPE_TYPES;
import static org.cneko.toneko.neoforge.ToNekoNeoForge.RECIPE_SERIALIZERS;
public class ToNekoRecipesNeo {
    static DeferredHolder<RecipeType<?>,RecipeType<NekoAggregatorRecipe>> NEKO_AGGREGATOR_HOLDER;
    static DeferredHolder<RecipeSerializer<?>,RecipeSerializer<NekoAggregatorRecipe>> NEKO_AGGREGATOR_SERIALIZER_HOLDER;
    public static void init(){
        NEKO_AGGREGATOR_HOLDER = RECIPE_TYPES.register("neko_aggregator", ()-> new RecipeType<>() {
            public String toString() {
                return "neko_aggregator";
            }
        });
        NEKO_AGGREGATOR_SERIALIZER_HOLDER = RECIPE_SERIALIZERS.register("neko_aggregator", NekoAggregatorRecipe.Serializer::new);
    }

    public static void reg(){
        NEKO_AGGREGATOR = NEKO_AGGREGATOR_HOLDER.get();
        NEKO_AGGREGATOR_SERIALIZER = NEKO_AGGREGATOR_SERIALIZER_HOLDER.get();
    }
}
