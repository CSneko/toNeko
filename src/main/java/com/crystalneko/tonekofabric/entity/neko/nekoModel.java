package com.crystalneko.tonekofabric.entity.neko;

import com.crystalneko.tonekofabric.entity.neko.nekoEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;

public class nekoModel extends EntityModel<nekoEntity> {

    private final ModelPart Head;
    private final ModelPart Body;
    private final ModelPart RightArm;
    private final ModelPart LeftArm;
    private final ModelPart RightLeg;
    private final ModelPart LeftLeg;
    private final ModelPart Tail;
    private ModelPart TailHead;
    private ModelPart TH2_r1;
    private ModelPart TH1_r1;
    private ModelPart TailMedium;
    private ModelPart TM4_r1;
    private ModelPart TM3_r1;
    private ModelPart TM2_r1;
    private ModelPart TM1_r1;

    public nekoModel(ModelPart root) {
        this.Head = root.getChild("Head");
        this.Body = root.getChild("Body");
        this.RightArm = root.getChild("RightArm");
        this.LeftArm = root.getChild("LeftArm");
        this.RightLeg = root.getChild("RightLeg");
        this.LeftLeg = root.getChild("LeftLeg");
        this.Tail = root.getChild("Tail");
    }
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData Head = modelPartData.addChild("Head", ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new Dilation(0.0F))
                .uv(32, 0).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new Dilation(0.5F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData Body = modelPartData.addChild("Body", ModelPartBuilder.create().uv(16, 16).cuboid(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new Dilation(0.0F))
                .uv(16, 32).cuboid(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new Dilation(0.25F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData RightArm = modelPartData.addChild("RightArm", ModelPartBuilder.create().uv(40, 16).cuboid(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new Dilation(0.0F))
                .uv(40, 32).cuboid(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new Dilation(0.25F)), ModelTransform.pivot(-5.0F, 2.0F, 0.0F));

        ModelPartData LeftArm = modelPartData.addChild("LeftArm", ModelPartBuilder.create().uv(32, 48).cuboid(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new Dilation(0.0F))
                .uv(48, 48).cuboid(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new Dilation(0.25F)), ModelTransform.pivot(5.0F, 2.0F, 0.0F));

        ModelPartData RightLeg = modelPartData.addChild("RightLeg", ModelPartBuilder.create().uv(0, 16).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new Dilation(0.0F))
                .uv(0, 32).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new Dilation(0.25F)), ModelTransform.pivot(-1.9F, 12.0F, 0.0F));

        ModelPartData LeftLeg = modelPartData.addChild("LeftLeg", ModelPartBuilder.create().uv(16, 48).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new Dilation(0.0F))
                .uv(0, 48).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new Dilation(0.25F)), ModelTransform.pivot(1.9F, 12.0F, 0.0F));

        ModelPartData Tail = modelPartData.addChild("Tail", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 12.0F, 3.0F));

        ModelPartData TailHead = Tail.addChild("TailHead", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData TH2_r1 = TailHead.addChild("TH2_r1", ModelPartBuilder.create().uv(55, 32).cuboid(-1.0F, -0.4163F, -0.6037F, 2.0F, 2.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -2.7121F, 1.8558F, -0.6981F, 0.0F, 0.0F));

        ModelPartData TH1_r1 = TailHead.addChild("TH1_r1", ModelPartBuilder.create().uv(54, 32).cuboid(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.3054F, 0.0F, 0.0F));

        ModelPartData TailMedium = TailHead.addChild("TailMedium", ModelPartBuilder.create().uv(56, 32).cuboid(-1.0F, -8.4497F, 1.0853F, 2.0F, 2.0F, 2.0F, new Dilation(0.0F))
                .uv(56, 35).cuboid(-1.0F, -10.4497F, 1.0853F, 2.0F, 2.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, -4.0F, 3.0F));

        ModelPartData TM4_r1 = TailMedium.addChild("TM4_r1", ModelPartBuilder.create().uv(56, 34).cuboid(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -3.5483F, 1.8276F, -0.0873F, 0.0F, 0.0F));

        ModelPartData TM3_r1 = TailMedium.addChild("TM3_r1", ModelPartBuilder.create().uv(56, 33).cuboid(-1.0F, 0.0323F, -0.5257F, 2.0F, 1.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -3.7489F, 1.3775F, -0.1745F, 0.0F, 0.0F));

        ModelPartData TM2_r1 = TailMedium.addChild("TM2_r1", ModelPartBuilder.create().uv(56, 33).cuboid(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -1.0863F, 0.8986F, -0.4363F, 0.0F, 0.0F));

        ModelPartData TM1_r1 = TailMedium.addChild("TM1_r1", ModelPartBuilder.create().uv(54, 37).cuboid(-1.0F, -1.1421F, 0.9183F, 2.0F, 2.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 2.18F, -1.0239F, 1.0036F, 0.0F, 0.0F));
        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setAngles(nekoEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {

    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
        Head.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
        Body.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
        RightArm.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
        LeftArm.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
        RightLeg.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
        LeftLeg.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
        Tail.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
    }
}
