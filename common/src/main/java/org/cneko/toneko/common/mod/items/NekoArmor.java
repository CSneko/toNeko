package org.cneko.toneko.common.mod.items;

import org.cneko.toneko.common.mod.util.NekoIds;
import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.client.items.NekoArmorRenderer;
import org.cneko.toneko.common.mod.misc.ToNekoEnchantments;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.SingletonGeoAnimatable;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;


import com.geckolib.constant.DefaultAnimations;
import com.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/**
 * 猫娘套装（尾巴/耳朵/爪爪）。
 *
 * <h2>26.x 迁移说明</h2>
 * {@code ArmorItem} 已被移除，盔甲通过
 * {@code Item.Properties#humanArmor(ArmorMaterial, ArmorType)} 装配；
 * 右键穿戴由 Equippable 组件自动处理，本类仅在成功穿戴时扣除少量血量。
 */
public abstract class NekoArmor<N extends Item & GeoItem> extends Item implements GeoItem {
    public final AnimatableInstanceCache cache;

    protected NekoArmor(Properties settings) {
        super(settings);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
        this.cache = GeckoLibUtil.createInstanceCache(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("idle", 40, state -> {
            // 26.x/GeckoLib 5.5：渲染态数据票变化后，穿戴判定交由渲染层；
            // 盔甲渲染器只在穿戴时被调用，这里恒定播放 idle 即可。
            state.setAndContinue(DefaultAnimations.IDLE);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private com.geckolib.renderer.GeoArmorRenderer<?, ?> renderer;

            @Override
            public com.geckolib.renderer.GeoArmorRenderer<?, ?> getGeoArmorRenderer(ItemStack itemStack,
                                                                                    net.minecraft.world.entity.EquipmentSlot equipmentSlot) {
                if (this.renderer == null) // Important that we do this. If we just instantiate it directly in the field it can cause incompatibilities with some mods.
                    this.renderer = (com.geckolib.renderer.GeoArmorRenderer<?, ?>) NekoArmor.this.getRenderer();

                return this.renderer;
            }
        });
    }

    @Override
    public boolean canBeEnchantedWith(ItemStack stack, Holder<Enchantment> enchantment, EnchantingContext context) {
        if (enchantment.is(ToNekoEnchantments.REVERSION)){
            return true;
        }
        return super.canBeEnchantedWith(stack, enchantment, context);
    }

    // 这里返回Object的原因是它会导致服务器没法启动 T_T
    public abstract Object getRenderer();

    public static class NekoTailItem extends NekoArmor<NekoTailItem> {
        public static final String ID = "neko_tail";
        public NekoTailItem(net.minecraft.world.item.equipment.ArmorMaterial material) {
            super(NekoIds.itemProps(ID).humanoidArmor(material, net.minecraft.world.item.equipment.ArmorType.CHESTPLATE).stacksTo(1));
        }

        @Override
        public NekoArmorRenderer.NekoTailRenderer getRenderer() {
            return new NekoArmorRenderer.NekoTailRenderer();
        }

        /** 穿戴（含与身上装备互换）成功时扣 0.5 血。 */
        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            InteractionResult result = super.use(level, player, hand);
            if (result.consumesAction()) {
                org.cneko.toneko.common.mod.util.EntityHurtUtil.hurt(player, player.damageSources().generic(), 0.5f);
            }
            return result;
        }
    }

    public static class NekoEarsItem extends NekoArmor<NekoEarsItem> {
        public static final String ID = "neko_ears";
        public NekoEarsItem(net.minecraft.world.item.equipment.ArmorMaterial material) {
            super(NekoIds.itemProps(ID).humanoidArmor(material, net.minecraft.world.item.equipment.ArmorType.HELMET).stacksTo(1));
        }

        @Override
        public NekoArmorRenderer.NekoEarsRenderer getRenderer() {
            return new NekoArmorRenderer.NekoEarsRenderer();
        }
    }

    public static class NekoPawsItem extends NekoArmor<NekoPawsItem> {
        public static final String ID = "neko_paws";
        public NekoPawsItem(net.minecraft.world.item.equipment.ArmorMaterial material) {
            super(NekoIds.itemProps(ID).humanoidArmor(material, net.minecraft.world.item.equipment.ArmorType.BOOTS).stacksTo(1));
        }

        @Override
        public NekoArmorRenderer.NekoPawsRenderer getRenderer() {
            return new NekoArmorRenderer.NekoPawsRenderer();
        }
    }
}
