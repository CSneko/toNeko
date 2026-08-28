package org.cneko.toneko.common.mod.recipes;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.chars.CharArraySet;
import it.unimi.dsi.fastutil.chars.CharSet;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class NekoAggregatorRecipePattern {
    // 26.x：Ingredient.EMPTY 已彻底移除，且空配方成分构造会抛
    // "Ingredients can't be empty"；与原版 ShapedRecipePattern 一致，空槽位用 Optional.empty() 表示。
    private static final int MAX_SIZE = 3;
    public static final MapCodec<NekoAggregatorRecipePattern> MAP_CODEC;
    public static final StreamCodec<RegistryFriendlyByteBuf, NekoAggregatorRecipePattern> STREAM_CODEC;
    private final int width;
    private final int height;
    private final List<Optional<Ingredient>> ingredients;
    private final Optional<NekoAggregatorRecipePattern.Data> data;
    private final int ingredientCount;
    private final boolean symmetrical;

    public NekoAggregatorRecipePattern(int width, int height, List<Optional<Ingredient>> ingredients, Optional<NekoAggregatorRecipePattern.Data> data) {
        this.width = width;
        this.height = height;
        this.ingredients = ingredients;
        this.data = data;
        int i = 0;

        for(Optional<Ingredient> optionalIngredient : ingredients) {
            if (optionalIngredient.isPresent()) {
                ++i;
            }
        }

        this.ingredientCount = i;
        this.symmetrical = isSymmetrical(width, height, ingredients);
    }

    // 26.x：原 net.minecraft.Util#isSymmetrical 已移除，此处保留原有左右对称判断
    private static boolean isSymmetrical(int width, int height, List<Optional<Ingredient>> ingredients) {
        if (width == 1) {
            return true;
        }
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width / 2; ++x) {
                Optional<Ingredient> left = ingredients.get(x + y * width);
                Optional<Ingredient> right = ingredients.get(width - x - 1 + y * width);
                if (!left.equals(right)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static NekoAggregatorRecipePattern of(Map<Character, Ingredient> key, String... pattern) {
        return of(key, List.of(pattern));
    }

    public static NekoAggregatorRecipePattern of(Map<Character, Ingredient> key, List<String> pattern) {
        NekoAggregatorRecipePattern.Data data = new NekoAggregatorRecipePattern.Data(key, pattern);
        return (NekoAggregatorRecipePattern)unpack(data).getOrThrow();
    }

    private static DataResult<NekoAggregatorRecipePattern> unpack(NekoAggregatorRecipePattern.Data data) {
        String[] strings = shrink(data.pattern);
        int i = strings[0].length();
        int j = strings.length;
        List<Optional<Ingredient>> list = new java.util.ArrayList<>(i * j);
        for(int slot = 0; slot < i * j; ++slot) {
            list.add(Optional.empty());
        }
        CharSet charSet = new CharArraySet(data.key.keySet());

        for(int k = 0; k < strings.length; ++k) {
            String string = strings[k];

            for(int l = 0; l < string.length(); ++l) {
                char c = string.charAt(l);
                Ingredient ingredient = c == ' ' ? null : (Ingredient)data.key.get(c);
                if (c != ' ' && ingredient == null) {
                    return DataResult.error(() -> "Pattern references symbol '" + c + "' but it's not defined in the key");
                }

                charSet.remove(c);
                list.set(l + i * k, Optional.ofNullable(ingredient));
            }
        }

        if (!charSet.isEmpty()) {
            return DataResult.error(() -> "Key defines symbols that aren't used in pattern: " + String.valueOf(charSet));
        } else {
            return DataResult.success(new NekoAggregatorRecipePattern(i, j, list, Optional.of(data)));
        }
    }

    @VisibleForTesting
    static String[] shrink(List<String> pattern) {
        int i = Integer.MAX_VALUE;
        int j = 0;
        int k = 0;
        int l = 0;

        for(int m = 0; m < pattern.size(); ++m) {
            String string = (String)pattern.get(m);
            i = Math.min(i, firstNonSpace(string));
            int n = lastNonSpace(string);
            j = Math.max(j, n);
            if (n < 0) {
                if (k == m) {
                    ++k;
                }

                ++l;
            } else {
                l = 0;
            }
        }

        if (pattern.size() == l) {
            return new String[0];
        } else {
            String[] strings = new String[pattern.size() - l - k];

            for(int o = 0; o < strings.length; ++o) {
                strings[o] = ((String)pattern.get(o + k)).substring(i, j + 1);
            }

            return strings;
        }
    }

    private static int firstNonSpace(String row) {
        int i;
        for(i = 0; i < row.length() && row.charAt(i) == ' '; ++i) {
        }

        return i;
    }

    private static int lastNonSpace(String row) {
        int i;
        for(i = row.length() - 1; i >= 0 && row.charAt(i) == ' '; --i) {
        }

        return i;
    }

    public boolean matches(NekoAggregatorInput input) {
        if (input.ingredientCount() == this.ingredientCount) {
            if (input.width() == this.width && input.height() == this.height) {
                if (!this.symmetrical && this.matches(input, true)) {
                    return true;
                }

                return this.matches(input, false);
            }

        }
        return false;
    }

    private boolean matches(NekoAggregatorInput input, boolean symmetrical) {
        for(int i = 0; i < this.height; ++i) {
            for(int j = 0; j < this.width; ++j) {
                Optional<Ingredient> optionalIngredient;
                if (symmetrical) {
                    optionalIngredient = this.ingredients.get(this.width - j - 1 + i * this.width);
                } else {
                    optionalIngredient = this.ingredients.get(j + i * this.width);
                }

                ItemStack itemStack = input.getItem(j, i);
                // 空槽位要求该位置无物品；非空槽位按普通配方成分匹配
                if (optionalIngredient.isEmpty() ? !itemStack.isEmpty() : !optionalIngredient.get().test(itemStack)) {
                    return false;
                }
            }
        }

        return true;
    }

    private void toNetwork(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(this.width);
        buffer.writeVarInt(this.height);

        for(Optional<Ingredient> optionalIngredient : this.ingredients) {
            Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.encode(buffer, optionalIngredient);
        }

    }

    private static NekoAggregatorRecipePattern fromNetwork(RegistryFriendlyByteBuf buffer) {
        int i = buffer.readVarInt();
        int j = buffer.readVarInt();
        List<Optional<Ingredient>> list = new java.util.ArrayList<>(i * j);
        for(int k = 0; k < i * j; ++k) {
            list.add((Optional<Ingredient>)Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.decode(buffer));
        }
        return new NekoAggregatorRecipePattern(i, j, list, Optional.empty());
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public List<Optional<Ingredient>> ingredients() {
        return this.ingredients;
    }

    static {
        MAP_CODEC = NekoAggregatorRecipePattern.Data.MAP_CODEC.flatXmap(NekoAggregatorRecipePattern::unpack, (shapedRecipePattern) -> (DataResult)shapedRecipePattern.data.map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Cannot encode unpacked recipe")));
        STREAM_CODEC = StreamCodec.ofMember(NekoAggregatorRecipePattern::toNetwork, NekoAggregatorRecipePattern::fromNetwork);
    }

    public static record Data(Map<Character, Ingredient> key, List<String> pattern) {
        private static final Codec<List<String>> PATTERN_CODEC;
        private static final Codec<Character> SYMBOL_CODEC;
        public static final MapCodec<NekoAggregatorRecipePattern.Data> MAP_CODEC;

        static {
            PATTERN_CODEC = Codec.STRING.listOf().comapFlatMap((list) -> {
                if (list.size() > 3) {
                    return DataResult.error(() -> "Invalid pattern: too many rows, 3 is maximum");
                } else if (list.isEmpty()) {
                    return DataResult.error(() -> "Invalid pattern: empty pattern not allowed");
                } else {
                    int i = ((String)list.getFirst()).length();

                    for(String string : list) {
                        if (string.length() > 3) {
                            return DataResult.error(() -> "Invalid pattern: too many columns, 3 is maximum");
                        }

                        if (i != string.length()) {
                            return DataResult.error(() -> "Invalid pattern: each row must be the same width");
                        }
                    }

                    return DataResult.success(list);
                }
            }, Function.identity());
            SYMBOL_CODEC = Codec.STRING.comapFlatMap((string) -> {
                if (string.length() != 1) {
                    return DataResult.error(() -> "Invalid key entry: '" + string + "' is an invalid symbol (must be 1 character only).");
                } else {
                    return " ".equals(string) ? DataResult.error(() -> "Invalid key entry: ' ' is a reserved symbol.") : DataResult.success(string.charAt(0));
                }
            }, String::valueOf);
            MAP_CODEC = RecordCodecBuilder.mapCodec((instance) -> instance.group(ExtraCodecs.strictUnboundedMap(SYMBOL_CODEC, Ingredient.CODEC).fieldOf("key").forGetter((data) -> data.key), PATTERN_CODEC.fieldOf("pattern").forGetter((data) -> data.pattern)).apply(instance, NekoAggregatorRecipePattern.Data::new));
        }
    }
}
