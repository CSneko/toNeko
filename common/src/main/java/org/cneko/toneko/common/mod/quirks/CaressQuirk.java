package org.cneko.toneko.common.mod.quirks;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.util.TextUtil;

import static org.cneko.toneko.common.mod.quirks.ToNekoQuirks.CARESS;
import static org.cneko.toneko.common.mod.util.TextUtil.translatable;

public class CaressQuirk extends Quirk {
    public CaressQuirk(String id) {
        super(id);
    }

    @Override
    public int getInteractionValue() {
        return 1;
    }

    @Override
    public Component getTooltip() {
        // 添加描述
        return Component.translatable("quirk.toneko.caress.des");
    }

    @Override
    public InteractionResult onNekoInteraction(Player owner, Level world, InteractionHand hand, INeko neko, EntityHitResult hitResult) {
        super.onNekoInteraction(owner, world, hand, neko, hitResult);
        if (owner.getMainHandItem().isEmpty()){
            LivingEntity nekoPlayer = neko.getEntity();
            // 播放爱心粒子
            nekoPlayer.level().addParticle(ParticleTypes.HEART,nekoPlayer.getX()+1.8, nekoPlayer.getY(), nekoPlayer.getZ(),1,1,1);
            if (owner instanceof ServerPlayer so && nekoPlayer instanceof ServerPlayer snp) {
                // 发送给客户端
                ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(ParticleTypes.HEART, true, owner.getX() + 1.8, owner.getY(), owner.getZ(), 2, 2, 2, 1, 1);
                so.connection.send(packet);
                snp.connection.send(packet);
            }
            // 增加互动值
            neko.setXpWithOwner(owner.getUUID(), CARESS.getInteractionValue()+ neko.getXpWithOwner(owner.getUUID()));
            // 发送消息文本
            if (neko instanceof Player player) {
                owner.displayClientMessage(translatable("quirk.toneko.caress.use", TextUtil.getPlayerName(player)), true);
                player.displayClientMessage(translatable("quirk.toneko.caress.be_used", TextUtil.getPlayerName(owner)), true);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
