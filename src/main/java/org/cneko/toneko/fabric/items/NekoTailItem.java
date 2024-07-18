package org.cneko.toneko.fabric.items;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import org.cneko.toneko.fabric.client.items.NekoTailRenderer;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.client.render.entity.model.BipedEntityModel;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class NekoTailItem extends ArmorItem implements GeoItem {
    public static final String ID = "neko_tail";
    public AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public NekoTailItem() {
        super(ToNekoArmorMaterials.NEKO,Type.LEGGINGS,new Settings().maxCount(1));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoArmorRenderer<?> renderer;

            @Override
            public <T extends LivingEntity> BipedEntityModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot, @Nullable BipedEntityModel<T> original) {
                if(this.renderer == null) // Important that we do this. If we just instantiate  it directly in the field it can cause incompatibilities with some mods.
                    this.renderer = new NekoTailRenderer();

                return this.renderer;
            }
        });
    }
}
