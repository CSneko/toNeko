package org.cneko.toneko.fabric.items;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.cneko.toneko.common.api.NekoQuery;

import static org.cneko.toneko.fabric.util.TextUtil.translatable;
public class NekoPotionItem extends PotionItem {
    public static final String ID = "neko_potion";

    public NekoPotionItem() {
        super(new Settings().maxCount(1));
    }


    public void toneko(World world, PlayerEntity user, Hand hand) {
        // 如果食物被成功吃掉并且玩家还不是猫猫，则把玩家变成猫猫
        TypedActionResult<ItemStack> result = super.use(world, user, hand);
        NekoQuery.Neko neko = NekoQuery.getNeko(user.getUuid());
        if(result.getResult() == ActionResult.CONSUME && !neko.isNeko()){
            neko.setNeko(true);
            if(user instanceof ServerPlayerEntity player){
                //哼!哼!喵喵喵喵喵喵喵喵喵喵喵喵喵喵喵喵喵喵!
                // 向猫猫显示标题
                TitleS2CPacket title = new TitleS2CPacket(translatable("title.toneko.become"));
                SubtitleS2CPacket subtitle = new SubtitleS2CPacket(translatable("subtitle.toneko.become"));
                player.networkHandler.sendPacket(title);
                player.networkHandler.sendPacket(subtitle);

                // 让猫猫听到经验音效
                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);

            }
            neko.save();
        }
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        PlayerEntity playerEntity = user instanceof PlayerEntity ? (PlayerEntity)user : null;
        if (playerEntity instanceof ServerPlayerEntity) {
            Criteria.CONSUME_ITEM.trigger((ServerPlayerEntity)playerEntity, stack);
        }


        if (playerEntity != null) {
            playerEntity.incrementStat(Stats.USED.getOrCreateStat(this));
            if (!playerEntity.getAbilities().creativeMode) {
                stack.decrement(1);
            }
        }

        if (playerEntity == null || !playerEntity.getAbilities().creativeMode) {
            if (stack.isEmpty()) {
                return new ItemStack(Items.GLASS_BOTTLE);
            }

            if (playerEntity != null) {
                playerEntity.getInventory().insertStack(new ItemStack(Items.GLASS_BOTTLE));
            }

        }
        // 把玩家变成猫猫
        toneko(world, playerEntity, user.getActiveHand());
        user.emitGameEvent(GameEvent.DRINK);
        return stack;
    }
}
