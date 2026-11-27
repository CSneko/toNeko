package org.cneko.toneko.common.mod.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.cneko.toneko.common.mod.items.CatnipBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 允许把已经酿好的猫薄荷水重新放回酿造台的药水槽。
 *
 * <p>原版 {@code canPlaceItem} 只认 {@code Items.POTION/SPLASH_POTION/LINGERING_POTION}，
 * 自定义物品会被拒绝（漏斗、发射器、以及某些快捷移动路径都会走到这里）。
 * 菜单里的 {@code PotionSlot} 只检查 {@code PotionContents#potion()} 是否存在，不需要额外处理。
 */
@Mixin(BrewingStandBlockEntity.class)
public class BrewingStandBlockEntityMixin {

    @Inject(method = "canPlaceItem", at = @At("HEAD"), cancellable = true)
    private void toneko$allowCatnipPotions(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (slot >= 0 && slot < 3 && CatnipBrewing.isCatnipPotion(stack)) {
            // 与原版一致：槽位为空才允许放入
            cir.setReturnValue(((BrewingStandBlockEntity) (Object) this).getItem(slot).isEmpty());
        }
    }
}
