package org.cneko.toneko.common.mod.entities;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.cneko.toneko.common.mod.api.EntityPoseManager;
import sun.misc.Unsafe;

import java.util.UUID;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.RegistryAccess;
import com.google.common.collect.ImmutableList;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;

/** Exercise the real damage-attribution override against the target game's EntityReference. */
public final class MushroomNativeRegressionTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Unsafe fixtures = (Unsafe) field.get(null);
        // These fixtures call only attribution methods. Bypass world-bound constructors;
        // no partially constructed entities are ticked, rendered, or put in a world.
        DamageProbe neko = (DamageProbe) fixtures.allocateInstance(DamageProbe.class);
        ServerPlayer player = (ServerPlayer) fixtures.allocateInstance(ServerPlayer.class);
        player.setUUID(UUID.fromString("94053a09-d71e-44df-981b-57b9439d1e68"));

        boolean reproduced = false;
        try { EntityReference.getPlayer(EntityReference.of((UUID) null), null); }
        catch (NullPointerException expected) { reproduced = true; }
        check(reproduced, "Old UUID-null reference must reproduce the reported NPE");

        neko.setLastHurtByPlayer(player, 120);
        check(neko.reference() != null && neko.reference().matches(player), "Stranger damage keeps the responsible player");
        check(neko.memory() == 120, "Stranger damage keeps its attribution duration");
        neko.familiar = true;
        neko.setLastHurtByPlayer(player, 120);
        check(neko.reference() == null && neko.memory() == 0, "Friend damage clears the reference itself");
        check(EntityReference.getPlayer(neko.reference(), null) == null, "Vanilla can safely resolve cleared friend damage");

        neko.familiar = false;
        neko.setLastHurtByPlayer(player, 80);
        neko.setLastHurtByPlayer((Player) null, 80);
        check(neko.reference() == null && neko.memory() == 0, "Null player safely clears old attribution");
        check(EntityReference.getPlayer(neko.reference(), null) == null, "Vanilla can safely resolve cleared null-player damage");

        MushroomProbe mushroom = (MushroomProbe) fixtures.allocateInstance(MushroomProbe.class);
        mushroom.attributes = new AttributeMap(LivingEntity.createLivingAttributes().build());
        PlayerProbe carrier = (PlayerProbe) fixtures.allocateInstance(PlayerProbe.class);
        carrier.attributes = new AttributeMap(LivingEntity.createLivingAttributes().build());
        carrier.pose = Pose.STANDING;
        carrier.scale = 1;
        mushroom.mode = MushroomGirlEntity.CARRIED;
        for (float yaw : new float[]{0, 90, 180, 270}) {
            carrier.setYRot(yaw);
            Vec3 riding = carrier.getPassengerRidingPosition(mushroom)
                    .subtract(mushroom.getVehicleAttachmentPoint(carrier));
            double angle = Math.toRadians(yaw);
            Vec3 expected = carrier.position().add(Math.sin(angle) * 0.32, 0.55, -Math.cos(angle) * 0.32);
            check(riding.distanceToSqr(expected) < 1e-12, "Carry offset follows native riding math and player yaw");
        }
        carrier.scale = 0.5;
        carrier.attributes.getInstance(Attributes.SCALE).setBaseValue(0.5);
        mushroom.mode = MushroomGirlEntity.SITTING_ON_PLAYER;
        carrier.setYRot(0);
        Vec3 riding = carrier.getPassengerRidingPosition(mushroom).subtract(mushroom.getVehicleAttachmentPoint(carrier));
        check(riding.distanceToSqr(carrier.position().add(0.15, 0.11, 0.45)) < 1e-12, "Companion offset follows a scaled player and clears its foot-origin camera");
        for (float scale : new float[]{0.5f, 1, 2}) {
            carrier.scale = scale;
            carrier.attributes.getInstance(Attributes.SCALE).setBaseValue(scale);
            carrier.pose = Pose.SLEEPING;
            for (float yaw : new float[]{0, 90, 180, 270}) {
                carrier.setYRot(yaw);
                for (byte mode : new byte[]{MushroomGirlEntity.SITTING_ON_PLAYER, MushroomGirlEntity.LYING_ON_PLAYER}) {
                    mushroom.mode = mode;
                    Vec3 seat = carrier.getPassengerRidingPosition(mushroom).subtract(mushroom.getVehicleAttachmentPoint(carrier));
                    Vec3 camera = carrier.position().add(0, 0.2 * scale, 0);
                    check(seat.distanceTo(camera) > (mode == MushroomGirlEntity.LYING_ON_PLAYER ? 1.4 : 0.9) * scale,
                            "The ground companion's seat stays outside the foot-origin camera across yaw and player scales");
                }
            }
        }
        carrier.scale = 1; carrier.attributes.getInstance(Attributes.SCALE).setBaseValue(1); carrier.pose = Pose.STANDING;
        mushroom.mode = MushroomGirlEntity.SITTING_ON_PLAYER;
        check(Math.abs(mushroom.getScale() - 0.7) < 1e-6, "Native scale retains the requested 0.7 size");
        var seated = mushroom.getDimensions(Pose.STANDING);
        check(Math.abs(seated.width() - 0.63) < 1e-6 && Math.abs(seated.height() - 1.05) < 1e-6,
                "Mounted dimensions follow native entity scaling");
        mushroom.mode = MushroomGirlEntity.LYING_ON_PLAYER;
        check(mushroom.getDimensions(Pose.STANDING).height() < seated.height(), "Prone companion has a lower collision box");
        check(!MushroomGirlEntity.isPlayerLying(carrier), "Standing player is not lying");
        carrier.pose = Pose.SLEEPING;
        check(MushroomGirlEntity.isPlayerLying(carrier), "Beds and lie command allow companionship");
        carrier.pose = Pose.SWIMMING;
        check(!MushroomGirlEntity.isPlayerLying(carrier), "Ordinary swimming pose alone does not allow companionship");
        EntityPoseManager.setPose(carrier, Pose.SWIMMING);
        check(MushroomGirlEntity.isPlayerLying(carrier), "Pinned dry prone pose allows companionship");
        carrier.wet = true;
        check(!MushroomGirlEntity.isPlayerLying(carrier), "Swimming in water never triggers prone companionship");
        EntityPoseManager.remove(carrier);

        PacketSink sink = (PacketSink) fixtures.allocateInstance(PacketSink.class);
        sink.sent = new ArrayList<>(); carrier.connection = sink;
        carrier.setId(73); mushroom.setId(41);
        var passengers = Entity.class.getDeclaredField("passengers"); passengers.setAccessible(true);
        passengers.set(carrier, ImmutableList.of(mushroom));
        mushroom.mode = MushroomGirlEntity.SITTING_ON_PLAYER;
        mushroom.syncTo(carrier);
        check(sink.sent.size() == 2 && sink.sent.getFirst() instanceof ClientboundSetEntityDataPacket,
                "Carrier receives explicit companion metadata before the passenger relationship");
        ClientboundSetPassengersPacket mounted = (ClientboundSetPassengersPacket) sink.sent.get(1);
        check(mounted.getVehicle() == 73 && mounted.getPassengers().length == 1 && mounted.getPassengers()[0] == 41,
                "Carrier receives its own native passenger update");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer, (ClientboundSetEntityDataPacket) sink.sent.getFirst());
        var decoded = ClientboundSetEntityDataPacket.STREAM_CODEC.decode(buffer);
        check(decoded.id() == 41 && decoded.packedItems().getFirst().value().equals(MushroomGirlEntity.SITTING_ON_PLAYER),
                "Companion pose survives the actual native network codec");
        buffer.release();
        sink.sent.clear(); passengers.set(carrier, ImmutableList.of()); mushroom.mode = MushroomGirlEntity.NONE;
        mushroom.syncTo(carrier);
        check(((ClientboundSetEntityDataPacket) sink.sent.getFirst()).packedItems().getFirst().value().equals(MushroomGirlEntity.NONE),
                "Dismount sends the default NONE value explicitly");
        check(((ClientboundSetPassengersPacket) sink.sent.get(1)).getPassengers().length == 0,
                "Dismount clears the carrier's local passenger relationship");
        System.out.println("Mushroom native regression: " + checks + " checks passed.");
    }

    private static final class MushroomProbe extends MushroomGirlEntity {
        byte mode;
        AttributeMap attributes;
        private MushroomProbe() { super(null, null); }
        @Override public byte getCompanionPose() { return mode; }
        @Override public boolean isResting() { return false; }
        @Override public boolean isCowering() { return false; }
        @Override public boolean isHiding() { return false; }
        @Override public AttributeMap getAttributes() { return attributes; }
        void syncTo(ServerPlayer player) { syncCompanionTo(player); }
    }

    private static final class PacketSink extends ServerGamePacketListenerImpl {
        List<Packet<?>> sent;
        private PacketSink() { super(null, null, null, null); }
        @Override public void send(Packet<?> packet) { sent.add(packet); }
    }

    private static final class PlayerProbe extends ServerPlayer {
        Pose pose;
        boolean wet;
        double scale;
        AttributeMap attributes;
        private PlayerProbe() { super(null, null, null, null); }
        @Override public Vec3 position() { return new Vec3(12, 64, -9); }
        @Override public Vec3 getPassengerRidingPosition(Entity passenger) { return position().add(0, 1.6 * scale, 0); }
        @Override public AttributeMap getAttributes() { return attributes; }
        @Override public Pose getPose() { return pose; }
        @Override public boolean isInWater() { return wet; }
        @Override public Optional<BlockPos> getSleepingPos() { return Optional.empty(); }
    }

    private static final class DamageProbe extends NekoEntity {
        boolean familiar;
        private DamageProbe() { super(null, null); }
        @Override public boolean hasOwner(UUID player) { return familiar; }
        @Override public NekoEntity getBreedOffspring(net.minecraft.server.level.ServerLevel level, INeko other) { return null; }
        EntityReference<Player> reference() { return lastHurtByPlayer; }
        int memory() { return lastHurtByPlayerMemoryTime; }
    }
}
