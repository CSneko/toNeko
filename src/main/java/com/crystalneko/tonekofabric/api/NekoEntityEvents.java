package com.crystalneko.tonekofabric.api;

import com.crystalneko.tonekofabric.entity.nekoEntity;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
public class NekoEntityEvents {

    /**
     * 玩家右键猫娘实体时的回调<br>
     * 它会在执行原本的代码前执行<br>
     * 参数:<br>
     * - entity: 实体<br>
     * - player: 玩家<br>
     * - hand: 玩家使用的手
     */
    public static Event<InteractEvent> ON_INTERACT = EventFactory.createArrayBacked(InteractEvent.class,
            (listeners) -> (entity, player, hand) -> {
                for (InteractEvent listener : listeners) {
                    ActionResult result = listener.onInteract(entity, player, hand);
                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    /**
     * 猫娘实体成长时的回调<br>
     * 它会在执行原本的代码后执行<br>
     * 参数:<br>
     * - entity: 实体<br>
     * - age: 猫娘的年龄<br>
     * - overGrow: 是否过度生长
     */
    public static Event<GrowUpEvent> GROW_UP = EventFactory.createArrayBacked(GrowUpEvent.class,
            (listeners) -> (entity, age, overGrow) -> {
                for (GrowUpEvent listener : listeners) {
                    listener.onGrowUp(entity, age, overGrow);
                }
            });

    public interface InteractEvent {
        ActionResult onInteract(nekoEntity entity, PlayerEntity player, Hand hand);
    }

    public interface GrowUpEvent {
        void onGrowUp(nekoEntity entity, int age, boolean overGrow);
    }
}
