package org.cneko.toneko.common.mod.client.renderers.layers;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.client.renderers.NekoRenderer;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.ItemArmorGeoLayer;

/**
 * 猫娘实体盔甲渲染层：让猫娘身上（捡起/被赠送/AI 穿戴）的盔甲显示出来。
 * 骨骼 → 槽位映射只覆盖有实际部件意义的 4 根骨骼（"chest" 胸骨不映射，避免胸甲双渲染）。
 * 丝袜（LegwearItem）的 legwear 模型使用 GeckoLib 标准 armorLeftLeg/armorRightLeg 骨骼名，
 * applyBoneVisibilityByPart 天然正确；玩家穿戴路径（HumanoidArmorLayerMixin）不走本层，互不影响。
 */
public class NekoArmorLayer<T extends NekoEntity> extends ItemArmorGeoLayer<T> {

    public NekoArmorLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    protected ItemStack getArmorItemForBone(GeoBone bone, T animatable) {
        if (!NekoRenderer.isArmorDisplayEnabled()) return null;
        ItemStack stack = switch (bone.getName()) {
            case "Head" -> animatable.getItemBySlot(EquipmentSlot.HEAD);
            case "Body" -> animatable.getItemBySlot(EquipmentSlot.CHEST);
            case "RightLeg", "LeftLeg" -> animatable.getItemBySlot(EquipmentSlot.LEGS);
            default -> null;
        };
        // renderForBone 只在返回 null 时跳过（空槽返回 EMPTY 会继续渲染流程）
        return stack != null && !stack.isEmpty() ? stack : null;
    }

    @Override
    protected ModelPart getModelPartForBone(GeoBone bone, EquipmentSlot slot, ItemStack stack,
                                            T animatable, HumanoidModel<?> baseModel) {
        return switch (bone.getName()) {
            case "Head" -> baseModel.head;
            case "Body" -> baseModel.body;
            case "RightLeg" -> baseModel.rightLeg;
            case "LeftLeg" -> baseModel.leftLeg;
            default -> baseModel.body;
        };
    }
}
