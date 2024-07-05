package org.cneko.toneko.fabric.network.packets;

import net.minecraft.entity.EntityPose;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.util.Identifier;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class EntitySetPoseS2CPacket implements Packet<PacketListener> {
    public static final Identifier ID = new Identifier(MODID, "entity_set_pose");
    private EntityPose pose;
    public boolean status;

    public EntitySetPoseS2CPacket(EntityPose pose,boolean status) {
        this.pose = pose;
        this.status = status;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeEnumConstant(pose);
        buf.writeBoolean(status);
    }

    @Override
    public void apply(PacketListener listener) {
    }
}
