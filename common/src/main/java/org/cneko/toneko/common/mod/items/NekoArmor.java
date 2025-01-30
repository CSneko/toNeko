package org.cneko.toneko.common.mod.items;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.client.items.NekoArmorRenderer;
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

import java.util.function.Consumer;

public abstract class NekoArmor<N extends Item & GeoItem> extends ArmorItem implements GeoItem {
    public final AnimatableInstanceCache cache;
    public NekoArmor(Holder<ArmorMaterial> material, Type type, Properties settings) {
        super(material, type, settings);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
        this.cache = GeckoLibUtil.createInstanceCache(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, 40, state -> {
            Entity e = state.getData(DataTickets.ENTITY);
            /*if (e.getMovement().lengthSquared() > 0) {
                // 行走动画
                if (e.getVelocity().lengthSquared() <= 0.025)
                    state.getController().setAnimation(DefaultAnimations.WALK);
                    // 跑动动画
                else state.getController().setAnimation(DefaultAnimations.RUN);
            }else*/
            state.getController().setAnimation(DefaultAnimations.IDLE);
            if (! (e instanceof LivingEntity entity)) return PlayState.STOP;
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
                if(this.renderer == null) // Important that we do this. If we just instantiate  it directly in the field it can cause incompatibilities with some mods.
                    this.renderer = (GeoArmorRenderer<N>)NekoArmor.this.getRenderer();

                return this.renderer;
            }
        });
    }

    // 这里返回Object的原因是它会导致服务器没法启动 T_T
    public abstract Object getRenderer();

    public static class NekoTailItem extends NekoArmor<NekoTailItem> {
        public static final String ID = "neko_tail";
        public NekoTailItem(Holder<ArmorMaterial> material) {
            super(material,Type.CHESTPLATE,new Properties().stacksTo(1));
        }

        @Override
        public NekoArmorRenderer.NekoTailRenderer getRenderer() {
            return new NekoArmorRenderer.NekoTailRenderer();
        }

        @Override
        public InteractionResultHolder<ItemStack> swapWithEquipmentSlot(Item item, Level world, Player user, InteractionHand hand) {
            InteractionResultHolder<ItemStack> result = super.swapWithEquipmentSlot(item, world, user, hand);
            // 如果成功，则扣除玩家0.5血量
            if (result.getResult().consumesAction()) {
                user.hurt(user.damageSources().generic(), 0.5f);
            }
            return result;
        }

    }

    public static class NekoEarsItem extends NekoArmor<NekoEarsItem> {
        public static final String ID = "neko_ears";
        public NekoEarsItem(Holder<ArmorMaterial> material) {
            super(material,Type.HELMET,new Properties().stacksTo(1));
        }

        @Override
        public NekoArmorRenderer.NekoEarsRenderer getRenderer() {
            return new NekoArmorRenderer.NekoEarsRenderer();
        }

    }

    public static class NekoPawsItem extends NekoArmor<NekoPawsItem> {
        public static final String ID = "neko_paws";
        public NekoPawsItem(Holder<ArmorMaterial> material) {
            super(material,Type.BOOTS,new Properties().stacksTo(1));
        }

        @Override
        public NekoArmorRenderer.NekoPawsRenderer getRenderer() {
            return new NekoArmorRenderer.NekoPawsRenderer();
        }
    }


}
