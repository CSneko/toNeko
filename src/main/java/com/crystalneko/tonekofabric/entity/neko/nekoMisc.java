package com.crystalneko.tonekofabric.entity.neko;

import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

public class nekoMisc {
    public static VoxelShape makeShape(){
        VoxelShape shape = VoxelShapes.empty();
        shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.25, 1.5, 0.25, 0.75, 2, 0.75));
        shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.25, 1.5, 0.25, 0.75, 2, 0.75));
        shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.25, 0.75, 0.375, 0.75, 1.5, 0.625));
        shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.25, 0.75, 0.375, 0.75, 1.5, 0.625));
        shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.75, 0.75, 0.375, 1, 1.5, 0.625));
        shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.75, 0.75, 0.375, 1, 1.5, 0.625));
        shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0, 0.75, 0.375, 0.25, 1.5, 0.625));
        shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0, 0.75, 0.375, 0.25, 1.5, 0.625));
        shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.49375, 0, 0.375, 0.74375, 0.75, 0.625));
        shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.49375, 0, 0.375, 0.74375, 0.75, 0.625));
        shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.25625, 0, 0.375, 0.50625, 0.75, 0.625));
        shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.25625, 0, 0.375, 0.50625, 0.75, 0.625));
        shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.4375, 0.75, 0.625, 0.5625, 0.875, 0.8125));
        shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.4375, 0.8205251108237632, 0.7657555090687476, 0.5625, 0.9455251108237632, 0.8907555090687476));
        shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.4375, 0.8101294791643654, 0.8683954615654204, 0.5625, 0.9351294791643654, 1.0558954615654204));
        shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.4375, 1.067893316349747, 0.8686625087322994, 0.5625, 1.192893316349747, 0.9936625087322994));
        shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.4375, 1.1697882653938747, 0.9282374025641313, 0.5625, 1.2322882653938747, 1.0532374025641313));
        shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.4375, 1.2217658698363405, 0.9267248711387245, 0.5625, 1.4092658698363405, 1.0517248711387244));
        shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.4375, 1.4031051418068141, 0.9428287415346445, 0.5625, 1.5281051418068141, 1.0678287415346444));
        shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.4375, 1.5281051418068141, 0.9428287415346445, 0.5625, 1.6531051418068141, 1.0678287415346444));

        return shape;
    }

}
