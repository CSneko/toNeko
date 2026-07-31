package org.cneko.toneko.common.mod.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.WoolCarpetBlock;
import org.cneko.toneko.common.mod.api.NekoLevelRegistry;
import org.cneko.toneko.common.mod.entities.INeko;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockPlaceMixin {
    @Inject(method = "place", at = @At("RETURN"))
    private void toneko$onPlace(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        // 仅在放置成功时给予经验
        if (!cir.getReturnValue().consumesAction()) return;

        Player player = context.getPlayer();
        if (!(player instanceof INeko neko) || !neko.isNeko()) return;

        Block block = ((BlockItem) (Object) this).getBlock();
        double xp = getHomesteadXp(block);
        if (xp > 0) {
            NekoLevelRegistry.homestead().addRaw(neko, xp);
        }
    }

    /**
     * 根据放置的方块类型返回家园经验值。
     */
    private static double getHomesteadXp(Block block) {
        // 猫娘模组方块：统一 30 XP
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if ("toneko".equals(id.getNamespace())) {
            return 30.0;
        }
        // 羊毛地毯 / 普通地毯：2 XP（猫喜欢软软的地方）
        if (block instanceof WoolCarpetBlock || block instanceof CarpetBlock) {
            return 2.0;
        }
        // 火把 / 灯笼：3 XP（温暖的光）
        if (block instanceof BaseTorchBlock || block instanceof LanternBlock) {
            return 3.0;
        }
        return 0;
    }
}
