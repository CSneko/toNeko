package org.cneko.toneko.common.mod.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class NekoAggregatorRecipe implements Recipe<NekoAggregatorInput> {
    public final NekoAggregatorRecipePattern pattern;
    final ItemStack result;
    public final double energy;
    public NekoAggregatorRecipe(NekoAggregatorRecipePattern pattern,double energy,ItemStack result){
        this.pattern = pattern;
        this.result = result;
        this.energy = energy;
    }
    @Override
    public boolean matches(@NotNull NekoAggregatorInput input, @NotNull Level level) {
        return this.pattern.matches(input);
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull NekoAggregatorInput input) {
        return this.result.copy();
    }

    @Override
    public @NotNull RecipeSerializer<NekoAggregatorRecipe> getSerializer() {
        return ToNekoRecipes.NEKO_AGGREGATOR_SERIALIZER;
    }

    @Override
    public @NotNull RecipeType<NekoAggregatorRecipe> getType() {
        return ToNekoRecipes.NEKO_AGGREGATOR;
    }

    @Override
    public @NotNull net.minecraft.world.item.crafting.PlacementInfo placementInfo() {
        return PlacementInfo.create(this.pattern.ingredients().stream().flatMap(java.util.Optional::stream).toList());
    }

    @Override
    public boolean showNotification() { return true; }

    @Override
    public boolean isSpecial() {
        // 自定义机器配方（非工作台）：跳过原版配方簿 placement 检查，
        // 消除加载时 "Recipe ... can't be placed due to empty ingredients" 警告
        return true;
    }

    @Override
    public @NotNull String group() { return ""; }

    @Override
    public @NotNull net.minecraft.world.item.crafting.RecipeBookCategory recipeBookCategory() {
        return net.minecraft.world.item.crafting.RecipeBookCategories.CRAFTING_MISC;
    }

    private static final MapCodec<NekoAggregatorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            NekoAggregatorRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.pattern),
            Codec.DOUBLE.fieldOf("energy").forGetter(recipe -> recipe.energy),
            ItemStack.MAP_CODEC.fieldOf("result").forGetter(recipe -> recipe.result)
    ).apply(instance, NekoAggregatorRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, NekoAggregatorRecipe> STREAM_CODEC =
            StreamCodec.of(NekoAggregatorRecipe::toNetwork, NekoAggregatorRecipe::fromNetwork);

    // 26.x：RecipeSerializer 从接口改为 record，直接用 codec + streamCodec 构造
    public static final RecipeSerializer<NekoAggregatorRecipe> Serializer = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    private static NekoAggregatorRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        NekoAggregatorRecipePattern shapedRecipePattern = NekoAggregatorRecipePattern.STREAM_CODEC.decode(buffer);
        ItemStack itemStack = ItemStack.STREAM_CODEC.decode(buffer);
        double energy = ByteBufCodecs.DOUBLE.decode(buffer);
        return new NekoAggregatorRecipe(shapedRecipePattern, energy, itemStack);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buffer, NekoAggregatorRecipe recipe) {
        NekoAggregatorRecipePattern.STREAM_CODEC.encode(buffer, recipe.pattern);
        ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
        buffer.writeDouble(recipe.energy);
    }
}
