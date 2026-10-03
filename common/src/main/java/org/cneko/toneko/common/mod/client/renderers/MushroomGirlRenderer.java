package org.cneko.toneko.common.mod.client.renderers;

import com.geckolib.model.GeoModel;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.cneko.toneko.common.mod.entities.MushroomGirlEntity;
import org.cneko.toneko.common.mod.api.MushroomBedRest;
import net.minecraft.world.entity.player.Player;

public class MushroomGirlRenderer extends GeoEntityRenderer<MushroomGirlEntity, TonekoLivingEntityGeoState> {
    private static final String[] MOUTHS = {"mouth_smile", "mouth_soft", "mouth_open", "mouth_o", "mouth_pout"};
    public MushroomGirlRenderer(EntityRendererProvider.Context context) { super(context, new Model()); }

    @Override public TonekoLivingEntityGeoState createRenderState(MushroomGirlEntity entity, Void unused) {
        return new TonekoLivingEntityGeoState();
    }

    @Override public void extractRenderState(MushroomGirlEntity entity, TonekoLivingEntityGeoState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        // Living passengers inherit the vehicle's body yaw. calculateYRot is only used
        // for non-living Gecko render states, so correct the actual living state here.
        MushroomGirlRenderPose.alignCompanion(entity, state);
    }

    @Override public void captureDefaultRenderState(MushroomGirlEntity entity, Void unused,
                                                   TonekoLivingEntityGeoState state, float partialTick) {
        super.captureDefaultRenderState(entity, unused, state, partialTick);
        state.addGeckolibData(NekoRenderer.TONEKO_ENTITY, entity);
    }

    @Override public void applyAnimationControllers(RenderPassInfo<TonekoLivingEntityGeoState> info, BoneSnapshots snapshots) {
        super.applyAnimationControllers(info, snapshots);
        var entity = info.renderState().getGeckolibData(NekoRenderer.TONEKO_ENTITY);
        boolean rest = entity instanceof MushroomGirlEntity mushroom && mushroom.isResting();
        // The cap covers the curled body; omit covered geometry so hair and clothes never poke through.
        snapshots.ifPresent("hips", bone -> bone.skipRender(rest).skipChildrenRender(rest));
        snapshots.ifPresent("sleep_cap", bone -> bone.skipRender(!rest).skipChildrenRender(!rest));

        // Hidden facial alternatives use visibility, avoiding singular matrices and overlapping mouth layers.
        String mouth = "mouth_smile";
        float largest = -1;
        for (String variant : MOUTHS) {
            var snapshot = snapshots.get(variant);
            if (snapshot.isPresent() && snapshot.get().getScaleX() > largest) {
                largest = snapshot.get().getScaleX();
                mouth = variant;
            }
        }
        for (String variant : MOUTHS) {
            boolean hidden = !variant.equals(mouth);
            snapshots.ifPresent(variant, bone -> bone.skipRender(hidden));
        }
        for (String side : new String[]{"left", "right"}) {
            var eye = snapshots.get("eye_" + side);
            boolean closed = eye.isPresent() && eye.get().getScaleY() < 0.15f;
            snapshots.ifPresent("eye_" + side, bone -> bone.skipRender(closed).skipChildrenRender(closed));
            snapshots.ifPresent("eye_lid_" + side, bone -> bone.skipRender(!closed || bone.getScaleX() < 0.5f));
            snapshots.ifPresent("eye_happy_" + side, bone -> bone.skipRender(bone.getScaleX() < 0.5f));
            snapshots.ifPresent("eye_sparkle_" + side, bone -> bone.skipRender(bone.getScaleX() < 0.5f));
            snapshots.ifPresent("eye_displeased_" + side, bone -> bone.skipRender(bone.getScaleX() < 0.5f));
        }
        snapshots.ifPresent("blush_shy", bone -> bone.skipRender(bone.getScaleX() < 0.5f));

        var manager = info.renderState().getGeckolibData(DataTickets.ANIMATABLE_MANAGER);
        var express = manager == null ? null : manager.getAnimationControllers().get("express");
        boolean bedCompanion = entity instanceof MushroomGirlEntity mushroom
                && mushroom.getCompanionPose() == MushroomGirlEntity.SITTING_ON_PLAYER
                && mushroom.getVehicle() instanceof Player player && MushroomBedRest.bed(player).isPresent();
        if (!rest && entity != null && !(entity instanceof MushroomGirlEntity mushroom && (mushroom.isCowering() || mushroom.isHiding()))
                && (!entity.isPassenger() || bedCompanion)
                && (bedCompanion || express == null || !express.isPlayingTriggeredAnimation())) {
            MushroomGirlRenderPose.applyLookDirection(snapshots, info.renderState().yRot, info.renderState().xRot, bedCompanion);
        }
    }

    private static class Model extends GeoModel<MushroomGirlEntity> {
        private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("toneko", path); }
        @Override public Identifier getModelResource(GeoRenderState state) { return id("entity/purwhite"); }
        @Override public Identifier getAnimationResource(MushroomGirlEntity entity) { return id("entity/purwhite"); }
        @Override public Identifier getTextureResource(GeoRenderState state) { return id("textures/entity/purwhite.png"); }
    }
}
