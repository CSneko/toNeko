package org.cneko.toneko.common.mod.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.cneko.toneko.common.mod.items.CatnipBrewing;
import org.cneko.toneko.common.mod.items.ToNekoItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 把猫薄荷水的三条酿造配方接进原版酿造系统。
 *
 * <p>原版 {@link PotionBrewing} 只处理 {@code Items.POTION/SPLASH_POTION/LINGERING_POTION}
 * 之间的转换，自定义物品进不了它的容器列表，所以直接在这三个入口上做判断：
 * <ul>
 *   <li>{@code isIngredient}：让猫薄荷可以作为酿造材料放进材料槽；</li>
 *   <li>{@code hasMix}：参数顺序是 {@code (potion, ingredient)}；</li>
 *   <li>{@code mix}：参数顺序是 {@code (ingredient, potion)}。</li>
 * </ul>
 */
@Mixin(PotionBrewing.class)
public class PotionBrewingMixin {

    @Inject(method = "isIngredient", at = @At("HEAD"), cancellable = true)
    private void toneko$isCatnipIngredient(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (ToNekoItems.CATNIP != null && stack.is(ToNekoItems.CATNIP)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "hasMix", at = @At("HEAD"), cancellable = true)
    private void toneko$hasCatnipMix(ItemStack potion, ItemStack ingredient, CallbackInfoReturnable<Boolean> cir) {
        if (CatnipBrewing.resultFor(ingredient, potion) != null) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mix", at = @At("HEAD"), cancellable = true)
    private void toneko$mixCatnip(ItemStack ingredient, ItemStack potion, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack result = CatnipBrewing.resultFor(ingredient, potion);
        if (result != null) {
            cir.setReturnValue(result);
        }
    }
}
