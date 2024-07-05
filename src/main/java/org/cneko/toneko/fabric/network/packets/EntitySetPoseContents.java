package org.cneko.toneko.fabric.network.packets;

import net.minecraft.entity.EntityPose;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.util.Identifier;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class EntitySetPoseContents /*implements Packet<PacketListener>*/ {
    public static final Identifier ID = Identifier.of(MODID, "entity_set_pose");
    private EntityPose pose;
    public boolean status;

    public EntitySetPoseContents(EntityPose pose, boolean status) {
        this.pose = pose;
        this.status = status;
    }


    public void write(PacketByteBuf buf) {
        buf.writeEnumConstant(pose);
        buf.writeBoolean(status);
    }
/*
    @Override
    public PacketType<? extends Packet<PacketListener>> getPacketId() {
        return EntityPosePayload.ID;
    }

    @Override
    public void apply(PacketListener listener) {
    }
    */

}
