package org.cneko.toneko.fabric.events;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.fabric.util.TextUtil;

import static org.cneko.toneko.common.quirks.Quirks.CARESS;
import static org.cneko.toneko.fabric.util.TextUtil.translatable;

public class PlayerInteractionEvent {
    public static void init(){
        UseEntityCallback.EVENT.register(PlayerInteractionEvent::useEntity);
    }

    public static ActionResult useEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        // 目标实体为玩家
        if(entity instanceof PlayerEntity nekoPlayer){
            NekoQuery.Neko neko = NekoQuery.getNeko(nekoPlayer.getUuid());
            // 如果是猫娘且玩家是主人
            if(neko.isNeko() && neko.hasOwner(player.getUuid())){
                // 如果有抚摸癖好且为玩家空手
                if(neko.hasQuirk(CARESS) && player.getMainHandStack().isEmpty()){
                    // 播放爱心粒子
                    nekoPlayer.getWorld().addParticle(ParticleTypes.HEART,nekoPlayer.getX()+1.8, nekoPlayer.getY(), nekoPlayer.getZ(),1,1,1);
                    // 增加互动值
                    neko.addXp(player.getUuid(), CARESS.getInteractionValue());
                    // 发送消息文本
                    player.sendMessage(translatable("quirk.toneko.caress.use", TextUtil.getPlayerName(player)), true);
                    nekoPlayer.sendMessage(translatable("quirk.toneko.caress.be_used", TextUtil.getPlayerName(player)),true);
                    return ActionResult.SUCCESS;
                }
            }
            neko.save();
        }
        return ActionResult.PASS;
    }
}
