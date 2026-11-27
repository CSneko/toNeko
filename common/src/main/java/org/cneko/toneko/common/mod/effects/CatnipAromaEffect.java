package org.cneko.toneko.common.mod.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

import static org.cneko.toneko.common.Bootstrap.MODID;

/**
 * 猫薄荷香气：喷溅型 / 滞留型猫薄荷水给玩家挂的标记效果。
 *
 * <p>效果本身不做任何事，只是给 {@link org.cneko.toneko.common.mod.abilities.CatnipAromaHandler}
 * 提供一个「谁身上有香气」的判定；真正的吸引逻辑在那里统一处理（需要跨实体仲裁，放在效果 tick 里会重复执行）。
 *
 * <p>图标暂时是透明的占位图，等以后再补。
 */
public class CatnipAromaEffect extends MobEffect {
    public static final String ID = "catnip_aroma";
    public static final Identifier LOCATION = Identifier.fromNamespaceAndPath(MODID, ID);

    public CatnipAromaEffect() {
        super(MobEffectCategory.NEUTRAL, 0x88B008);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }
}
