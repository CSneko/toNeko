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
    final NekoAggregatorRecipePattern pattern;
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
    public @NotNull ItemStack assemble(@NotNull NekoAggregatorInput input, HolderLookup.@NotNull Provider registries) {
        return getResultItem(registries);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= this.pattern.width() && height >= this.pattern.height();
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider registries) {
        return this.result.copy();
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ToNekoRecipes.NEKO_AGGREGATOR_SERIALIZER;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return ToNekoRecipes.NEKO_AGGREGATOR;
    }

    public static class Serializer implements RecipeSerializer<NekoAggregatorRecipe> {
        public static final MapCodec<NekoAggregatorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                NekoAggregatorRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.pattern),
                Codec.DOUBLE.fieldOf("energy").forGetter(recipe -> recipe.energy),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result)
        ).apply(instance, NekoAggregatorRecipe::new));


        public static final StreamCodec<RegistryFriendlyByteBuf, NekoAggregatorRecipe> STREAM_CODEC = StreamCodec.of(NekoAggregatorRecipe.Serializer::toNetwork, NekoAggregatorRecipe.Serializer::fromNetwork);

        public @NotNull MapCodec<NekoAggregatorRecipe> codec() {
            return CODEC;
        }

        public @NotNull StreamCodec<RegistryFriendlyByteBuf, NekoAggregatorRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static NekoAggregatorRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            NekoAggregatorRecipePattern shapedRecipePattern = NekoAggregatorRecipePattern.STREAM_CODEC.decode(buffer);
            ItemStack itemStack = ItemStack.STREAM_CODEC.decode(buffer);
            double energy = ByteBufCodecs.DOUBLE.decode(buffer);
            return new NekoAggregatorRecipe(shapedRecipePattern,energy, itemStack);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, NekoAggregatorRecipe recipe) {
            NekoAggregatorRecipePattern.STREAM_CODEC.encode(buffer, recipe.pattern);
            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
            buffer.writeDouble(recipe.energy);
        }
    }
}
