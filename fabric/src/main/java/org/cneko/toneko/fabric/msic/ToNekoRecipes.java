package org.cneko.toneko.fabric.msic;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import org.cneko.toneko.common.mod.recipes.NekoAggregatorRecipe;

import static org.cneko.toneko.common.mod.recipes.ToNekoRecipes.*;
import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

public class ToNekoRecipes {
    public static void init(){
        NEKO_AGGREGATOR = registerType("neko_aggregator");
        NEKO_AGGREGATOR_SERIALIZER = registerSer("neko_aggregator", new NekoAggregatorRecipe.Serializer());
    }


    static <T extends Recipe<?>> RecipeType<T> registerType(final String identifier) {
        return Registry.register(BuiltInRegistries.RECIPE_TYPE,toNekoLoc(identifier), new RecipeType<T>() {
            public String toString() {
                return identifier;
            }
        });
    }

    static <S extends RecipeSerializer<T>, T extends Recipe<?>> S registerSer(String key, S recipeSerializer) {
        return (Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, toNekoLoc(key), recipeSerializer));
    }
}
