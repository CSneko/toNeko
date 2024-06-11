package org.cneko.toneko.fabric.items;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.cneko.toneko.common.api.NekoQuery;
import static org.cneko.toneko.fabric.util.TextUtil.translatable;
public class NekoPotion extends Item {
    public static final String ID = "neko_potion";

    public NekoPotion() {
        super(new Settings());
    }

    @Override
    public boolean isFood() {
        return true;
    }
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        // 如果食物被成功吃掉并且玩家还不是猫猫，则把玩家变成猫猫
        TypedActionResult<ItemStack> result = super.use(world, user, hand);
        NekoQuery.Neko neko = NekoQuery.getNeko(user.getUuid());
        if(result.getResult() == ActionResult.CONSUME && !neko.isNeko()){
            neko.setNeko(true);
            if(user instanceof ServerPlayerEntity player){
                // 向猫猫显示标题
                TitleS2CPacket title = new TitleS2CPacket(translatable("title.toneko.become"));
                SubtitleS2CPacket subtitle = new SubtitleS2CPacket(translatable("subtitle.toneko.become"));
                player.networkHandler.sendPacket(title);
                player.networkHandler.sendPacket(subtitle);
                // 让猫猫听到经验音效
                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);

            }
        }
        return result;
    }

    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }
}
