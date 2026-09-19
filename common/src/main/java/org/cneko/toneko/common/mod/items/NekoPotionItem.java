package org.cneko.toneko.common.mod.items;
import org.cneko.toneko.common.mod.util.NekoIds;
import org.cneko.toneko.common.mod.entities.INeko;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.advencements.ToNekoCriteria;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.common.mod.util.TextUtil.translatable;
public class NekoPotionItem extends PotionItem {
    public static final String ID = "neko_potion";

    /** 已经是猫娘时，喝一瓶恢复的能量 */
    private static final float ENERGY_PER_BOTTLE = 100f;

    public NekoPotionItem() {
        // 26.1.2：饮用行为由 CONSUMABLE 组件驱动（同原版药水），缺失则 use() 直接无效。
        // usingConvertsTo 让原版在喝完后自动返还玻璃瓶，不必再手写“扣 1 瓶 + 塞空瓶”。
        super(NekoIds.itemProps(ID).stacksTo(1)
                .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
                .usingConvertsTo(Items.GLASS_BOTTLE));
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack stack = new ItemStack(this);
        // 确保含有空的 PotionContents，避免其他代码直接取出时为 null
        stack.set(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return stack;
    }

    /**
     * 药水生效：还不是猫娘就变猫娘，已经是猫娘则回复能量。只在服务端调用。
     */
    public void applyNekoPotionEffect(@NotNull Player player) {
        INeko neko = (INeko) player;
        if (!neko.isNeko()) {
            neko.setNeko(true);
            if (player instanceof ServerPlayer serverPlayer) {
                //哼!哼!喵喵喵喵喵喵喵喵喵喵喵喵喵喵喵喵喵喵!
                // 向猫猫显示标题
                serverPlayer.connection.send(new ClientboundSetTitleTextPacket(translatable("title.toneko.become")));
                serverPlayer.connection.send(new ClientboundSetSubtitleTextPacket(translatable("subtitle.toneko.become")));

                // 让猫猫听到经验音效
                serverPlayer.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);

                // 触发成就：变身！猫娘！
                ToNekoCriteria.NEKO_BECOME.trigger(serverPlayer);
            }
        } else {
            // 恢复一些能量
            neko.setNekoEnergy(neko.getNekoEnergy() + ENERGY_PER_BOTTLE);
        }
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity user) {
        // 变身/回能必须放在 super 之前，且不能再去问“手上这瓶药水能不能喝”：
        // super 走的是原版 CONSUMABLE 流程，会把药水消耗掉。
        // 旧写法用 super.use(...) 的返回值是否等于 CONSUME 来判断，生存模式下此时手持槽已被清空，
        // 拿到的必然是 PASS，于是生存模式怎么喝都变不了猫娘（创造模式因为不消耗才碰巧正常）。
        if (user instanceof Player player && !level.isClientSide()) {
            applyNekoPotionEffect(player);
        }
        // 音效粒子、统计、CONSUME_ITEM 触发、扣掉 1 瓶、返还玻璃瓶全部交还原版 CONSUMABLE/USE_REMAINDER
        return super.finishUsingItem(stack, level, user);
    }
}
