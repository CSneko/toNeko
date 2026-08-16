package org.cneko.toneko.common.mod.items;

import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import org.cneko.toneko.common.mod.client.items.LegwearItemRenderer;
import org.cneko.toneko.common.mod.client.items.LegwearRenderer;
import org.cneko.toneko.common.mod.codecs.Scent;
import org.cneko.toneko.common.mod.misc.Scentable;
import org.cneko.toneko.common.mod.misc.ScentUtil;
import org.cneko.toneko.common.mod.misc.ToNekoComponents;
import org.cneko.toneko.common.mod.misc.ToNekoEnchantments;
import org.cneko.toneko.common.mod.misc.WetnessUtil;
import org.cneko.toneko.common.mod.misc.ZettaiRyouiki;
import org.cneko.toneko.common.util.ConfigUtil;
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
public abstract class LegwearItem<N extends Item & GeoItem> extends ArmorItem implements GeoItem, Scentable {
    public final AnimatableInstanceCache cache;

    public LegwearItem(int defaultDenier, float defaultLength, Holder<ArmorMaterial> material) {
        super(material, Type.LEGGINGS, new Properties().stacksTo(1)
                .component(ToNekoComponents.LEGWEAR_DENIER_COMPONENT, defaultDenier)
                .component(ToNekoComponents.LEGWEAR_LENGTH_COMPONENT, defaultLength)
                .component(ToNekoComponents.LEGWEAR_SCENT_COMPONENT, Scent.EMPTY)
                .component(ToNekoComponents.LEGWEAR_WET_COMPONENT, 0));
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

    /** 出品人（首写即署名；null = 未署名） */
    public static String getMaker(ItemStack stack) {
        return stack.get(ToNekoComponents.LEGWEAR_MAKER_COMPONENT);
    }

    /** 气味强度（0~100） */
    public static int getScentIntensity(ItemStack stack) {
        return ScentUtil.getIntensity(stack);
    }

    /** 湿度（0~100） */
    public static int getWetness(ItemStack stack) {
        return WetnessUtil.get(stack);
    }

    /** 气味积累系数：厚袜（>=40D）更快、薄袜（<=20D）更慢 */
    @Override
    public float scentAccumulationFactor(ItemStack stack) {
        int denier = getDenier(stack);
        if (denier >= 40) return ConfigUtil.getScentThickFactor();
        if (denier <= 20) return ConfigUtil.getScentThinFactor();
        return 1.0f;
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
            private LegwearItemRenderer itemRenderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot, @Nullable HumanoidModel<T> original) {
                if (this.renderer == null) // 懒加载，避免与其他 mod 不兼容
                    this.renderer = (GeoArmorRenderer<N>) LegwearItem.this.getRenderer();

                return this.renderer;
            }

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.itemRenderer == null)
                    this.itemRenderer = new LegwearItemRenderer();

                return this.itemRenderer;
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
        String maker = getMaker(stack);
        if (maker != null) {
            tooltip.add(Component.translatable("item.toneko.legwear.tip.maker", maker));
        }
        int scent = ScentUtil.getIntensity(stack);
        tooltip.add(Component.translatable("item.toneko.legwear.tip.scent",
                Component.translatable("item.toneko.legwear.scent." + ScentUtil.grade(scent))));
        if (scent > 0) {
            String wearer = ScentUtil.getWearer(stack);
            if (wearer != null && !wearer.isEmpty()) {
                tooltip.add(Component.translatable("item.toneko.legwear.tip.scent_wearer", wearer));
            }
        }
        int wetness = WetnessUtil.get(stack);
        if (wetness > 0) {
            tooltip.add(Component.translatable("item.toneko.legwear.tip.wetness",
                    Component.translatable("item.toneko.legwear.wetness." + WetnessUtil.grade(wetness))));
        }
    }

    // === 手持闻 / 水缸洗 ===

    /** 潜行 + 右键：凑近闻（纯本地，零 token）；普通右键仍是穿袜子 */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isSniffing(player)) {
            if (level.isClientSide) {
                doSniff(player, stack);
            }
            return InteractionResultHolder.consume(stack);
        }
        return super.use(level, player, hand);
    }

    /** 右键水缸：洗掉一半气味 + 沾湿到湿透，消耗一格水 */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        // 潜行 + 右键：闻（无论点空气还是点方块，优先于穿/洗）
        if (player != null && isSniffing(player)) {
            if (level.isClientSide) {
                doSniff(player, context.getItemInHand());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof LayeredCauldronBlock && state.getValue(LayeredCauldronBlock.LEVEL) > 0) {
            if (!level.isClientSide) {
                ItemStack stack = context.getItemInHand();
                stack.set(ToNekoComponents.LEGWEAR_SCENT_COMPONENT, ScentUtil.wash(stack));
                stack.set(ToNekoComponents.LEGWEAR_WET_COMPONENT, WetnessUtil.MAX_WETNESS);
                LayeredCauldronBlock.lowerFillLevel(state, level, pos);
                level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useOn(context);
    }

    /** 是否处于「闻」的交互意图：蹲下（潜行）状态；姿势为准，shift 标志兜底 */
    private static boolean isSniffing(Player player) {
        return player.isCrouching() || player.isShiftKeyDown();
    }

    /** 客户端闻：按气味等级弹暧昧文案，高浓度撒爱心粒子 */
    private static void doSniff(Player player, ItemStack stack) {
        int intensity = ScentUtil.getIntensity(stack);
        Component msg;
        if (intensity <= 0) {
            msg = Component.translatable("item.toneko.legwear.sniff.clean");
        } else {
            String wearer = ScentUtil.getWearer(stack);
            msg = Component.translatable("item.toneko.legwear.sniff." + ScentUtil.grade(intensity),
                    wearer == null || wearer.isEmpty() ? "???" : wearer);
        }
        player.displayClientMessage(msg, true);
        if (intensity >= 60) {
            for (int i = 0; i < 5; i++) {
                player.level().addParticle(ParticleTypes.HEART,
                        player.getX() + (player.getRandom().nextDouble() - 0.5) * 0.6,
                        player.getEyeY() + 0.2 + player.getRandom().nextDouble() * 0.4,
                        player.getZ() + (player.getRandom().nextDouble() - 0.5) * 0.6,
                        0, 0.05, 0);
            }
        }
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
        /** 过膝袜自然袜口高度（滑落回弹目标 / 提袜复位值） */
        public static final float NATURAL_TOP = 0.7f;
        public OverKneeSockItem(Holder<ArmorMaterial> material) {
            super(40, NATURAL_TOP, material);
        }

        @Override
        public LegwearRenderer getRenderer() {
            return new LegwearRenderer();
        }
    }
}
