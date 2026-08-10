package org.cneko.toneko.common.mod.ai;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.cneko.toneko.common.api.TickTasks;
import org.cneko.toneko.common.mod.api.EntityPoseManager;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.entities.ai.BehaviorPriority;
import org.cneko.toneko.common.mod.entities.ai.NekoBrain;
import org.cneko.toneko.common.mod.misc.Messaging;
import org.cneko.toneko.common.mod.util.TickTaskQueue;
import org.cneko.toneko.common.util.ConfigUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.cneko.toneko.common.mod.util.TextUtil.randomTranslatabledComponent;

/**
 * 触发型动作（事件驱动）：不依赖 AI 回复输出动作，玩家/世界事件直接触发猫娘行为反应。
 * 事件 → 条件检查（总开关/概率/萌属性）→ 行为序列（音效/粒子/短移动/本地化消息）。
 * 所有反应均为本地行为，不消耗 AI token。
 * <p>
 * 当前事件源：玩家空手摸头（{@link #onPlayerPet}）、猫娘受击（{@link #onNekoHurt}）。
 * 必须在服务器主线程调用。
 */
public final class NekoTriggerManager {
    /** 触发行为来源标记（与 NekoActionExecutor.ACTION_SOURCE 区分） */
    private static final Object TRIGGER_SOURCE = new Object();
    /** 摸头去重冷却（毫秒）：同一玩家对同一猫娘 3 秒内只触发一次反应，防交互双路径重复发包 */
    private static final long PET_COOLDOWN_MS = 3000;
    /** 摸头冷却：key = 玩家UUID + 猫娘UUID */
    private static final Map<String, Long> petCooldowns = new ConcurrentHashMap<>();

    private NekoTriggerManager() {}

    /**
     * 玩家空手摸头（右键）：猫娘按萌属性差异化反应——
     * 病娇低语 / 傲娇跑开 / 呆萌摔倒 / 文静害羞 / 默认呼噜撒娇。
     */
    public static void onPlayerPet(ServerLevel level, NekoEntity neko, ServerPlayer player) {
        if (!ConfigUtil.isTriggerEnabled()) return;
        // 同玩家+同猫娘 3 秒冷却：防交互双路径/双击导致重复消息
        long now = System.currentTimeMillis();
        String key = player.getUUID() + "|" + neko.getUUID();
        Long last = petCooldowns.get(key);
        if (last != null && now - last < PET_COOLDOWN_MS) return;
        petCooldowns.put(key, now);
        float chance = ConfigUtil.getTriggerPetChance();
        if (chance <= 0 || neko.getRandom().nextFloat() >= chance) return;

        List<String> tags = neko.getMoeTags();
        // 病娇/偏执：低语（不跑开，反而靠近） + 音效
        if (tags.contains("yandere") || tags.contains("paranoia")) {
            petMessage(neko, player, "message.toneko.trigger.pet.yandere", 3);
            neko.playExpressAnim("angry");
            playSound(neko, SoundEvents.CAT_HISS);
            return;
        }
        // 傲娇/小恶魔：傲娇跑开
        if (tags.contains("tsundere") || tags.contains("mesugaki")) {
            petMessage(neko, player, "message.toneko.trigger.pet.tsundere", 3);
            neko.playExpressAnim("angry");
            fleeFrom(neko, player, 3.0, 1.2);
            return;
        }
        // 呆萌/笨蛋：平地摔跤（扑通粒子）
        if (tags.contains("baka") || tags.contains("dojikko") || tags.contains("tennen_boke")) {
            petMessage(neko, player, "message.toneko.trigger.pet.baka", 3);
            neko.playExpressAnim("trip");
            level.sendParticles(ParticleTypes.POOF,
                    neko.getX(), neko.getY() + 0.5, neko.getZ(), 6, 0.3, 0.3, 0.3, 0.05);
            playSound(neko, SoundEvents.ITEM_PICKUP);
            return;
        }
        // 文静/软弱/温柔：害羞缩起来（蹲下 + 爱心粒子）
        if (tags.contains("shizukana") || tags.contains("yowaki") || tags.contains("gentleness")) {
            petMessage(neko, player, "message.toneko.trigger.pet.shy", 3);
            neko.playExpressAnim("shy");
            setPoseTemporarily(neko, Pose.CROUCHING, 40);
            level.sendParticles(ParticleTypes.HEART,
                    neko.getX(), neko.getY() + 1.2, neko.getZ(), 6, 0.4, 0.4, 0.4, 0.1);
            return;
        }
        // 默认：呼噜撒娇 + 爱心粒子
        petMessage(neko, player, "message.toneko.trigger.pet.default", 4);
        neko.playExpressAnim("purr");
        playSound(neko, SoundEvents.CAT_PURR);
        level.sendParticles(ParticleTypes.HEART,
                neko.getX(), neko.getY() + 1.2, neko.getZ(), 6, 0.4, 0.4, 0.4, 0.1);
    }

    /**
     * 猫娘受击后的行为反应（本地化消息已由 NekoEntity.on_hurt 按萌属性发送，这里只补行为）：
     * 胆小逃跑 / 黑化威胁 / 默认缩起来。
     */
    public static void onNekoHurt(NekoEntity neko, DamageSource source, float amount) {
        if (!ConfigUtil.isTriggerEnabled()) return;
        if (neko.level().isClientSide) return;
        float chance = ConfigUtil.getTriggerHurtChance();
        if (chance <= 0 || neko.getRandom().nextFloat() >= chance) return;

        List<String> tags = neko.getMoeTags();
        // 胆小/软弱/文静：短暂逃跑躲起来
        if (tags.contains("yowaki") || tags.contains("dojikko") || tags.contains("shizukana")) {
            LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
            neko.playExpressAnim("shy");
            fleeFrom(neko, attacker != null ? attacker : neko, 5.0, 1.2);
            return;
        }
        // 病娇/黑化：嘶吼威胁（不跑，靠近气势）
        if (tags.contains("yandere") || tags.contains("haraguro") || tags.contains("shoakuma")) {
            neko.playExpressAnim("angry");
            playSound(neko, SoundEvents.CAT_HISS);
            return;
        }
        // 默认/温柔/成熟：缩起来（蹲下害怕状）
        neko.playExpressAnim("shy");
        playSound(neko, SoundEvents.CAT_HISS);
        setPoseTemporarily(neko, Pose.CROUCHING, 40);
    }

    // ===== 行为原语 =====

    /** 发送摸头反应消息（本地化模板，走统一显示包：聊天栏/气泡） */
    private static void petMessage(NekoEntity neko, ServerPlayer player, String key, int range) {
        Component msg = randomTranslatabledComponent(neko.getRandom(), key, range);
        Messaging.sendNekoChat(player, neko, msg.getString());
    }

    /** 从目标反方向短距离跑开 */
    private static void fleeFrom(NekoEntity neko, LivingEntity from, double distance, double speed) {
        NekoBrain brain = neko.getNekoBrain();
        if (brain == null) return;
        double dx = neko.getX() - from.getX();
        double dz = neko.getZ() - from.getZ();
        double len = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));
        brain.submitMove(neko.getX() + dx / len * distance, neko.getY(), neko.getZ() + dz / len * distance,
                speed, BehaviorPriority.HIGH, TRIGGER_SOURCE);
    }

    /** 短暂切换姿势，ticks 后恢复 */
    private static void setPoseTemporarily(NekoEntity neko, Pose pose, int ticks) {
        EntityPoseManager.setPose(neko, pose);
        TickTaskQueue queue = new TickTaskQueue();
        queue.addTask(ticks, () -> {
            if (!neko.isRemoved()) EntityPoseManager.remove(neko);
        });
        TickTasks.add(queue);
    }

    /** 播放音效（周围可闻） */
    private static void playSound(NekoEntity neko, net.minecraft.sounds.SoundEvent sound) {
        neko.level().playSound(null, neko.getX(), neko.getY(), neko.getZ(),
                sound, neko.getSoundSource(), 1.0f, 1.0f);
    }
}
