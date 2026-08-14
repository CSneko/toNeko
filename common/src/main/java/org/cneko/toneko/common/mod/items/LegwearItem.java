package org.cneko.toneko.common.mod.items;

import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.enchantment.Enchantment;
import org.cneko.toneko.common.mod.client.items.LegwearRenderer;
import org.cneko.toneko.common.mod.misc.ToNekoComponents;
import org.cneko.toneko.common.mod.misc.ToNekoEnchantments;
import org.cneko.toneko.common.mod.misc.ZettaiRyouiki;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

/**
 * 腿部服饰（丝袜/未来裙装）基类。
 * 与 NekoArmor 相互独立：这是独立的类体系，未来裙装将作为
 * 本类的兄弟实现接入，绝对领域等级由 {@link ZettaiRyouiki} 统一计算。
 */
public abstract class LegwearItem<N extends Item & GeoItem> extends ArmorItem implements GeoItem {
    public final AnimatableInstanceCache cache;

    public LegwearItem(int defaultDenier, float defaultLength, Holder<ArmorMaterial> material) {
        super(material, Type.LEGGINGS, new Properties().stacksTo(1)
                .component(ToNekoComponents.LEGWEAR_DENIER_COMPONENT, defaultDenier)
                .component(ToNekoComponents.LEGWEAR_LENGTH_COMPONENT, defaultLength));
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
        this.cache = GeckoLibUtil.createInstanceCache(this);
    }

    // === 组件读写（renderer / tooltip / 音效共用） ===

    /** 丹尼尔值（D 值），默认 40D */
    public static int getDenier(ItemStack stack) {
        return stack.getOrDefault(ToNekoComponents.LEGWEAR_DENIER_COMPONENT, 40);
    }

    /** 袜口高度（0~1，1 = 髋部），默认连裤袜顶 */
    public static float getStockingTopHeight(ItemStack stack) {
        return stack.getOrDefault(ToNekoComponents.LEGWEAR_LENGTH_COMPONENT, 1f);
    }

    /** 是否是本体系的腿部服饰 */
    public static boolean isLegwear(ItemStack stack) {
        return stack.getItem() instanceof LegwearItem<?>;
    }

    /** 左腿独立染色 RGB（0~16777215），未设置返回 -1 */
    public static int getLeftDye(ItemStack stack) {
        DyedItemColor dye = stack.get(ToNekoComponents.LEGWEAR_DYE_LEFT_COMPONENT);
        return dye == null ? -1 : dye.rgb();
    }

    /** 右腿独立染色 RGB（0~16777215），未设置返回 -1 */
    public static int getRightDye(ItemStack stack) {
        DyedItemColor dye = stack.get(ToNekoComponents.LEGWEAR_DYE_RIGHT_COMPONENT);
        return dye == null ? -1 : dye.rgb();
    }

    /** 左腿渲染色：独立染色 → 整体染色 → 纯白 */
    public static int getLeftRenderColor(ItemStack stack) {
        int dye = getLeftDye(stack);
        if (dye >= 0) return dye;
        return DyedItemColor.getOrDefault(stack, 0xFFFFFF);
    }

    /** 右腿渲染色：独立染色 → 整体染色 → 纯白 */
    public static int getRightRenderColor(ItemStack stack) {
        int dye = getRightDye(stack);
        if (dye >= 0) return dye;
        return DyedItemColor.getOrDefault(stack, 0xFFFFFF);
    }

    // === GeoItem 三件套 ===

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, 40, state -> {
            Entity e = state.getData(DataTickets.ENTITY);
            state.getController().setAnimation(DefaultAnimations.IDLE);
            if (!(e instanceof LivingEntity entity)) return PlayState.STOP;
            if (entity instanceof ArmorStand)
                return PlayState.CONTINUE;
            for (ItemStack stack : entity.getArmorSlots()) {
                // 只要有任意一件穿了就播放
                if (!stack.isEmpty())
                    return PlayState.CONTINUE;
            }
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoArmorRenderer<N> renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot, @Nullable HumanoidModel<T> original) {
                if (this.renderer == null) // 懒加载，避免与其他 mod 不兼容
                    this.renderer = (GeoArmorRenderer<N>) LegwearItem.this.getRenderer();

                return this.renderer;
            }
        });
    }

    // === 附魔 ===

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public boolean canBeEnchantedWith(ItemStack stack, Holder<Enchantment> enchantment, EnchantingContext context) {
        if (enchantment.is(ToNekoEnchantments.REVERSION)) {
            return true;
        }
        return super.canBeEnchantedWith(stack, enchantment, context);
    }

    // 这里返回Object的原因是它会导致服务器没法启动 T_T
    public abstract Object getRenderer();

    // === tooltip ===

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.toneko.legwear.tip.denier", getDenier(stack)));
        tooltip.add(Component.translatable("item.toneko.legwear.tip.length", Math.round(getStockingTopHeight(stack) * 100)));
        String grade = ZettaiRyouiki.compute(stack);
        Component gradeText = Component.translatable("item.toneko.legwear.zettai_ryouiki." + grade);
        if ("full".equals(grade)) {
            // 全覆盖：不显示百分比
            tooltip.add(Component.translatable("item.toneko.legwear.tip.zettai_ryouiki_full", gradeText));
        } else {
            // 连续百分比 + 等级（百分比随袜口高度无级变化）
            tooltip.add(Component.translatable("item.toneko.legwear.tip.zettai_ryouiki",
                    Math.round(ZettaiRyouiki.computeTerritory(stack) * 100), gradeText));
        }
        tooltip.add(Component.translatable("item.toneko.legwear.tip.dyeable"));
    }

    // === 4 款预设 ===

    public static class Pantyhose40DItem extends LegwearItem<Pantyhose40DItem> {
        public static final String ID = "legwear_pantyhose_40d";
        public Pantyhose40DItem(Holder<ArmorMaterial> material) {
            super(40, 1.0f, material);
        }

        @Override
        public LegwearRenderer getRenderer() {
            return new LegwearRenderer();
        }
    }

    public static class Pantyhose20DItem extends LegwearItem<Pantyhose20DItem> {
        public static final String ID = "legwear_pantyhose_20d";
        public Pantyhose20DItem(Holder<ArmorMaterial> material) {
            super(20, 1.0f, material);
        }

        @Override
        public LegwearRenderer getRenderer() {
            return new LegwearRenderer();
        }
    }

    public static class Pantyhose5DItem extends LegwearItem<Pantyhose5DItem> {
        public static final String ID = "legwear_pantyhose_5d";
        public Pantyhose5DItem(Holder<ArmorMaterial> material) {
            super(5, 1.0f, material);
        }

        @Override
        public LegwearRenderer getRenderer() {
            return new LegwearRenderer();
        }
    }

    public static class OverKneeSockItem extends LegwearItem<OverKneeSockItem> {
        public static final String ID = "legwear_over_knee";
        public OverKneeSockItem(Holder<ArmorMaterial> material) {
            super(40, 0.7f, material);
        }

        @Override
        public LegwearRenderer getRenderer() {
            return new LegwearRenderer();
        }
    }
}
