package org.cneko.toneko.common.mod.items;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.common.mod.util.TextUtil.translatable;
public class NekoPotionItem extends PotionItem {
    public static final String ID = "neko_potion";

    public NekoPotionItem() {
        super(new Properties().stacksTo(1));
    }


    public void toneko(Level world, Player neko, InteractionHand hand) {
        // 如果食物被成功吃掉并且玩家还不是猫猫，则把玩家变成猫猫
        InteractionResultHolder<ItemStack> result = super.use(world, neko, hand);
        if (world.isClientSide()) return;
        if(result.getResult() == InteractionResult.CONSUME && !neko.isNeko()){
            neko.setNeko(true);
            if(neko instanceof ServerPlayer player){
                //哼!哼!喵喵喵喵喵喵喵喵喵喵喵喵喵喵喵喵喵喵!
                // 向猫猫显示标题
                ClientboundSetTitleTextPacket title = new ClientboundSetTitleTextPacket(translatable("title.toneko.become"));
                ClientboundSetSubtitleTextPacket subtitle = new ClientboundSetSubtitleTextPacket(translatable("subtitle.toneko.become"));
                player.connection.send(title);
                player.connection.send(subtitle);

                // 让猫猫听到经验音效
                player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
            }
        }else if (result.getResult() == InteractionResult.CONSUME &&neko.isNeko()){
            // 恢复一些能量
            neko.setNekoEnergy(neko.getNekoEnergy() + 100);
        }
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level world, @NotNull LivingEntity user) {
        Player playerEntity = user instanceof Player ? (Player)user : null;
        if (playerEntity instanceof ServerPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayer)playerEntity, stack);
        }


        if (playerEntity != null) {
            playerEntity.awardStat(Stats.ITEM_USED.get(this));
            if (!playerEntity.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        if (playerEntity == null || !playerEntity.getAbilities().instabuild) {
            // 把玩家变成猫猫
            if (!world.isClientSide) {
                toneko(world, playerEntity, user.getUsedItemHand());
            }
            if (stack.isEmpty()) {
                return new ItemStack(Items.GLASS_BOTTLE);
            }

            if (playerEntity != null) {
                playerEntity.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
            }

        }
        // 把玩家变成猫猫
        if (!world.isClientSide) {
            toneko(world, playerEntity, user.getUsedItemHand());
        }
        user.gameEvent(GameEvent.DRINK);
        return stack;
    }
}
