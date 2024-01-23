package com.crystalneko.tonekofabric.entity.client;

import com.crystalneko.tonekofabric.entity.nekoEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class nekoModel extends GeoModel<nekoEntity>{
    private final Identifier modelResource = new Identifier("toneko", "geo/entity/neko.geo.json");
    private final Identifier textureResource = new Identifier("toneko", "textures/entity/neko.png");
    private final Identifier animationResource = new Identifier("toneko", "animations/entity/neko.animation.json");

    @Override
    public Identifier getModelResource(nekoEntity nekoEntity) {
        return this.modelResource;
    }

    @Override
    public Identifier getTextureResource(nekoEntity nekoEntity) {
        return this.textureResource;
    }

    @Override
    public Identifier getAnimationResource(nekoEntity nekoEntity) {
        return new Identifier("toneko", "animations/entity/neko.animation.json");
    }


    @Override
    public void setCustomAnimations(nekoEntity animatable, long instanceId, AnimationState<nekoEntity> animationState) {
        /*EntityModelData entityModelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        CoreGeoBone head = getAnimationProcessor().getBone("Head");
        if(head != null){
            head.setRotX(entityModelData.headPitch() * MathHelper.RADIANS_PER_DEGREE);
            head.setRotY(entityModelData.netHeadYaw() * MathHelper.RADIANS_PER_DEGREE);
        }*/
    }
}
