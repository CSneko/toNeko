package org.cneko.toneko.neoforge.fabric.events;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.neoforge.fabric.util.TextUtil;

import static org.cneko.toneko.common.quirks.Quirks.CARESS;
import static org.cneko.toneko.neoforge.fabric.util.TextUtil.translatable;

public class PlayerInteractionEvent {
    public static void init(){
        UseEntityCallback.EVENT.register(PlayerInteractionEvent::useEntity);
    }

    public static InteractionResult useEntity(Player player, Level world, InteractionHand hand, Entity entity, EntityHitResult hitResult) {
        // 目标实体为玩家
        if(entity instanceof ServerPlayer nekoPlayer && player instanceof ServerPlayer sp){
            NekoQuery.Neko neko = NekoQuery.getNeko(nekoPlayer.getUUID());
            // 如果是猫娘且玩家是主人
            if(neko.isNeko() && neko.hasOwner(player.getUUID())){
                // 如果有抚摸癖好且为玩家空手
                if(neko.hasQuirk(CARESS) && player.getMainHandItem().isEmpty()){
                    // 播放爱心粒子
                    nekoPlayer.level().addParticle(ParticleTypes.HEART,nekoPlayer.getX()+1.8, nekoPlayer.getY(), nekoPlayer.getZ(),1,1,1);
                    // 发送给客户端
                    ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(ParticleTypes.HEART,true,player.getX()+1.8, player.getY(), player.getZ(),2,2,2,1,1);
                    sp.connection.send(packet);
                    nekoPlayer.connection.send(packet);
                    // 增加互动值
                    //neko.addXp(player.getUuid(), CARESS.getInteractionValue());
                    // 发送消息文本
                    player.displayClientMessage(translatable("quirk.toneko.caress.use", TextUtil.getPlayerName(player)), true);
                    nekoPlayer.displayClientMessage(translatable("quirk.toneko.caress.be_used", TextUtil.getPlayerName(player)),true);
                    return InteractionResult.SUCCESS;
                }
            }
            //neko.save();
        }
        return InteractionResult.PASS;
    }
}
