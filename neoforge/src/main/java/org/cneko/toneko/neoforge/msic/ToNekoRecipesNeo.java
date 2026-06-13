package org.cneko.toneko.neoforge.msic;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.cneko.toneko.common.mod.recipes.NekoAggregatorRecipe;

import static org.cneko.toneko.common.mod.recipes.ToNekoRecipes.*;
import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

public class ToNekoRecipesNeo {
    public static void init(IEventBus bus){
        // 立即创建对象并赋值，避免后续调用时静态字段为 null
        NEKO_AGGREGATOR = new RecipeType<NekoAggregatorRecipe>() {
            public String toString() {
                return "neko_aggregator";
            }
        };
        NEKO_AGGREGATOR_SERIALIZER = new NekoAggregatorRecipe.Serializer();
        // 延迟到 RegisterEvent 再注册到原版注册表
        bus.addListener(ToNekoRecipesNeo::onRegisterRecipes);
    }

    public static void onRegisterRecipes(RegisterEvent event) {
        event.register(Registries.RECIPE_TYPE, helper -> {
            helper.register(toNekoLoc("neko_aggregator"), NEKO_AGGREGATOR);
        });
        event.register(Registries.RECIPE_SERIALIZER, helper -> {
            helper.register(toNekoLoc("neko_aggregator"), NEKO_AGGREGATOR_SERIALIZER);
        });
    }

    public static void reg(){
        // 已在 init 中赋值，无需额外操作
    }
}
