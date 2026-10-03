package org.cneko.toneko.common.mod.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import org.cneko.toneko.common.mod.util.NekoIds;

public class WildCoffeeBlock extends BushBlock {
    public static final MapCodec<BushBlock> CODEC = simpleCodec(p -> new WildCoffeeBlock());

    public WildCoffeeBlock() {
        super(NekoIds.blockProps("wild_coffee").mapColor(MapColor.PLANT).noCollision()
                .instabreak().sound(SoundType.GRASS));
    }

    @Override public MapCodec<BushBlock> codec() { return CODEC; }
}
