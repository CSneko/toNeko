package org.cneko.toneko.fabric.items;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import dev.emi.trinkets.api.SlotAttributes;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.Trinket;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Consumer;

public abstract class NekoArmor<N extends Item & GeoItem> extends ArmorItem implements GeoItem, Trinket {
    public final AnimatableInstanceCache cache;
    public NekoArmor(RegistryEntry<ArmorMaterial> material, Type type, Settings settings) {
        super(material, type, settings);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
        this.cache = GeckoLibUtil.createInstanceCache(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, 50, state -> {
            Entity e = state.getData(DataTickets.ENTITY);
            if (e.getMovement().lengthSquared() > 0) {
                // 行走动画
                if (e.getVelocity().lengthSquared() <= 0.025)
                    state.getController().setAnimation(DefaultAnimations.WALK);
                    // 跑动动画
                else state.getController().setAnimation(DefaultAnimations.RUN);
            }else state.getController().setAnimation(DefaultAnimations.IDLE);
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
        /*
        controllers.add(new AnimationController<>(this, 40, state -> {
            Entity e = state.getData(DataTickets.ENTITY);
            // 正在移动
            if (! (e instanceof LivingEntity entity)) return PlayState.STOP;
            for (ItemStack stack : entity.getArmorItems()) {
                // 只要有任意一件穿了就播放
                if (!stack.isEmpty())
                    return PlayState.CONTINUE;
            }
            return PlayState.STOP;
        }));
         */

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
            public <T extends LivingEntity> BipedEntityModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot, @Nullable BipedEntityModel<T> original) {
                if(this.renderer == null) // Important that we do this. If we just instantiate  it directly in the field it can cause incompatibilities with some mods.
                    this.renderer = NekoArmor.this.getRenderer();

                return this.renderer;
            }
        });
    }

    // --------------------- Trinket ---------------------
    @Override
    public boolean canEquipFromUse(ItemStack stack, LivingEntity entity) {
        return true;
    }
    @Override
    public boolean canUnequip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        return true;
    }
    @Override
    public Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> getModifiers(ItemStack stack, SlotReference slot, LivingEntity entity, Identifier slotIdentifier) {
        Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> modifiers = Multimaps.newMultimap(Maps.newLinkedHashMap(), ArrayList::new);
        SlotAttributes.addSlotModifier(modifiers, "chest/back", slotIdentifier, 1, EntityAttributeModifier.Operation.ADD_VALUE);
        return modifiers;
    }
    public abstract GeoArmorRenderer<N> getRenderer();
}
