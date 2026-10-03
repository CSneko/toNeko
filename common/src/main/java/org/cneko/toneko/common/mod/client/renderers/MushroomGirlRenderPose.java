package org.cneko.toneko.common.mod.client.renderers;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.BoneSnapshots;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.api.MushroomBedRest;
import org.cneko.toneko.common.mod.entities.MushroomGirlEntity;

/** Companion facing and gaze shared by all render passes. */
final class MushroomGirlRenderPose {
    private MushroomGirlRenderPose() {}

    static void alignCompanion(MushroomGirlEntity entity, TonekoLivingEntityGeoState state) {
        if (entity.getCompanionPose() != MushroomGirlEntity.CARRIED && entity.getVehicle() instanceof Player player) {
            var facing = MushroomBedRest.direction(player);
            state.bodyRot = facing != null ? facing.toYRot() : player.getYRot() + 180;
            state.yRot = 0;
            state.addGeckolibData(DataTickets.ENTITY_BODY_YAW, state.bodyRot);
            state.addGeckolibData(DataTickets.ENTITY_YAW, 0f);
        }
    }

    static void applyLookDirection(BoneSnapshots snapshots, float headYaw, float headPitch, boolean bedCompanion) {
        float yaw = Mth.clamp(Mth.wrapDegrees(headYaw), -25, 25);
        float pitch = Mth.clamp(headPitch, -15, bedCompanion ? 35 : 15);
        // Gecko's face points along -Z: Minecraft's positive downward pitch
        // needs a negative bone X rotation, as in DefaultAnimations.
        snapshots.ifPresent("head", bone -> bone.setRotY(bone.getRotY() - yaw * Mth.DEG_TO_RAD)
                .setRotX(bone.getRotX() - pitch * Mth.DEG_TO_RAD));
        for (String side : new String[]{"left", "right"}) {
            snapshots.ifPresent("iris_" + side, bone -> bone.setTranslateX(bone.getTranslateX() + yaw * 0.0015f)
                    .setTranslateY(bone.getTranslateY() - pitch * 0.002f));
        }
    }
}
