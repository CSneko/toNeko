package com.crystalneko.tonekofabric.api;

import com.crystalneko.tonekofabric.entity.nekoEntity;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.Entity;
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

    /**
     * 猫娘实体被攻击时的回调<br>
     * 它会在执行原本的代码前被执行<br>
     * 如果返回true,则不会继续往下执行，攻击将不会生效<br>
     * 参数:<br>
     * - neko: 猫娘实体<br>
     * - attacker: 攻击者
     */
    public static Event<OnAttackEvent> ON_ATTACK = EventFactory.createArrayBacked(OnAttackEvent.class,
            (listeners) -> (neko,attacker) -> {
                for (OnAttackEvent listener : listeners) {
                    listener.onAttack(neko,attacker);
                    boolean result = listener.onAttack(neko,attacker);
                    if(result){
                       return true;
                    }
                }
                return false;
    });

    /**
     * 猫娘实体每tick的回调<br>
     * 参数:<br>
     * - neko: 猫娘实体
     */
    public static Event<OnTickEvent> TICK = EventFactory.createArrayBacked(OnTickEvent.class,
            (listeners) -> (neko) -> {
                for (OnTickEvent listener : listeners) {
                    listener.onTick(neko);
                }
    });

    public interface InteractEvent {
        ActionResult onInteract(nekoEntity entity, PlayerEntity player, Hand hand);
    }

    public interface GrowUpEvent {
        void onGrowUp(nekoEntity entity, int age, boolean overGrow);
    }
    public interface OnAttackEvent {
        boolean onAttack(nekoEntity entity, Entity attacker);
    }
    public interface OnTickEvent {
        void onTick(nekoEntity entity);
    }
}
