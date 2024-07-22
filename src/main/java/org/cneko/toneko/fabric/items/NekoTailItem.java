package org.cneko.toneko.fabric.items;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.cneko.toneko.fabric.client.items.NekoTailRenderer;
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
import net.minecraft.client.render.entity.model.BipedEntityModel;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.function.Consumer;

public class NekoTailItem extends ArmorItem implements GeoItem {
    public static final String ID = "neko_tail";
    public final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public NekoTailItem() {
        super(ToNekoArmorMaterials.NEKO,Type.CHESTPLATE,new Settings().maxCount(1));
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 待机动画
        controllers.add(new AnimationController<>(this, 60, state -> {
            state.getController().setAnimation(DefaultAnimations.IDLE);
            if (state.isMoving())
                return PlayState.STOP;
            Entity e = state.getData(DataTickets.ENTITY);
            if (! (e instanceof LivingEntity entity)) return PlayState.STOP;
            if (entity instanceof ArmorStandEntity)
                return PlayState.CONTINUE;
            for (ItemStack stack : entity.getArmorItems()) {
                // 只要有任意一件穿了就播放
                if (!stack.isEmpty())
                    return PlayState.CONTINUE;
            }
            return PlayState.STOP;
        }));

        // 走路动画
        controllers.add(new AnimationController<>(this, 40, state -> {
            state.getController().setAnimation(DefaultAnimations.WALK);
            Entity e = state.getData(DataTickets.ENTITY);
             // 如果没有移动或速度过快就停止播放
             if (!state.isMoving() || e.getVelocity().lengthSquared() > 1)
                 return PlayState.STOP;
             if (! (e instanceof LivingEntity entity)) return PlayState.STOP;
             if (entity instanceof ArmorStandEntity)
                 return PlayState.CONTINUE;
             for (ItemStack stack : entity.getArmorItems()) {
                 // 只要有任意一件穿了就播放
                 if (!stack.isEmpty())
                     return PlayState.CONTINUE;
             }
             return PlayState.STOP;

        }));

        // 跑步动画
        controllers.add(new AnimationController<>(this, 40, state -> {
            state.getController().setAnimation(DefaultAnimations.RUN);
            Entity e = state.getData(DataTickets.ENTITY);
            // 如果没有移动或速度过慢就停止播放
            if (!state.isMoving() || e.getVelocity().lengthSquared() <= 1)
                return PlayState.STOP;
            if (! (e instanceof LivingEntity entity)) return PlayState.STOP;
            if (entity instanceof ArmorStandEntity)
                return PlayState.CONTINUE;
            for (ItemStack stack : entity.getArmorItems()) {
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
            private GeoArmorRenderer<NekoTailItem> renderer;

            @Override
            public <T extends LivingEntity> BipedEntityModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot, @Nullable BipedEntityModel<T> original) {
                if(this.renderer == null) // Important that we do this. If we just instantiate  it directly in the field it can cause incompatibilities with some mods.
                    this.renderer = new NekoTailRenderer();

                return this.renderer;
            }
        });
    }
}
