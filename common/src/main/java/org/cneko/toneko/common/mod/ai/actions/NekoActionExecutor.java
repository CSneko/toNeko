package org.cneko.toneko.common.mod.ai.actions;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import org.cneko.toneko.common.api.TickTasks;
import org.cneko.toneko.common.mod.ai.NekoDiary;
import org.cneko.toneko.common.mod.ai.NekoTalkManager;
import org.cneko.toneko.common.mod.ai.Prompts;
import org.cneko.toneko.common.mod.api.EntityPoseManager;
import org.cneko.toneko.common.mod.entities.NekoInventory;
import org.cneko.toneko.common.mod.misc.Messaging;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.cneko.toneko.common.mod.util.TickTaskQueue;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.entities.ai.BehaviorPriority;
import org.cneko.toneko.common.mod.entities.ai.NekoBrain;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.cneko.toneko.common.Bootstrap.LOGGER;

/**
 * AI 动作注册表与执行器。
 * 动作按 type 注册处理器（对齐 PromptRegistry 的模式），
 * 新动作只需 {@link #register(String, NekoActionHandler)} 即可接入，
 * 其 guide 说明也会自动出现在注入 prompt 的动作说明中。
 * 内置动作由 {@link #registerDefaults()} 在 ModBootstrap 注册。
 * 必须在服务器主线程调用 execute（调用方的 AI 回调已切主线程）。
 */
public class NekoActionExecutor {
    private static final Map<String, NekoActionHandler> HANDLERS = new LinkedHashMap<>();
    /** 动作来源标记：与其它行为（仇恨、跟随等）区分，防止误停 */
    private static final Object ACTION_SOURCE = new Object();
    /** 按名字查找目标实体时的搜索范围 */
    private static final double TARGET_SEARCH_RANGE = 64.0;

    private NekoActionExecutor() {}

    /** 注册动作处理器；type 已存在时覆盖 */
    public static void register(String type, NekoActionHandler handler) {
        HANDLERS.put(type, handler);
    }

    public static boolean hasAction(String type) {
        return HANDLERS.containsKey(type);
    }

    public static Collection<NekoActionHandler> getAll() {
        return HANDLERS.values();
    }

    public static java.util.Set<String> getIds() {
        return HANDLERS.keySet();
    }

    /** 注册内置动作（ModBootstrap 初始化时调用） */
    public static void registerDefaults() {
        register("move_to_player", new NekoActionHandler() {
            @Override
            public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
                NekoBrain brain = neko.getNekoBrain();
                if (brain == null) return false;
                brain.submitMove(target, 1.0, BehaviorPriority.HIGH, ACTION_SOURCE);
                return true;
            }
            @Override
            public String getGuideLine() {
                return "{\"action\": \"move_to_player\", \"target\": \"Steve\"} - "
                        + LanguageUtil.translatable("misc.toneko.ai.actions.guide.move_to_player");
            }
        });
        // 去找另一只猫娘说话（玩家指定的定向对话）：走向目标猫娘，走到面前后对方会回话，
        // 你回复的正文就是对她说的话。目标必须在 16 格内。
        register("talk_to_neko", new NekoActionHandler() {
            @Override
            public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
                // 目标必须是猫娘（resolveTarget 可能匹配到同名玩家）
                if (!(target instanceof NekoEntity other)) return false;
                NekoBrain brain = neko.getNekoBrain();
                if (brain == null) return false;
                // 走向目标猫娘
                brain.submitMove(other, 1.0, BehaviorPriority.HIGH, ACTION_SOURCE);
                // 记录定向对话目标：本次发言广播时由 NekoTalkManager 消费（走到面前后触发对方回话）
                NekoTalkManager.markDirectedTalk(neko, other);
                return true;
            }
            @Override
            public String getGuideLine() {
                return "{\"action\": \"talk_to_neko\", \"target\": \"小花\"} - "
                        + LanguageUtil.translatable("misc.toneko.ai.actions.guide.talk_to_neko");
            }
        });
        // 拥抱：走向目标 + 爱心粒子 + 呼噜音效（即时表达，不等走到）
        register("hug", new NekoActionHandler() {
            @Override
            public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
                if (target == null) return false;
                // 先走向目标，贴脸（≤3 格）后才执行拥抱表现（与右键 hug 交互共用）
                scheduleWalkToArrive(neko, target, neko::playHugEffect);
                return true;
            }
            @Override
            public String getGuideLine() {
                return "{\"action\": \"hug\", \"target\": \"Steve\"} - "
                        + LanguageUtil.translatable("misc.toneko.ai.actions.guide.hug");
            }
        });
        // 求摸头：蹲下仰头（姿势 2 秒）+ 撒娇消息 + 音效
        register("pet_request", new NekoActionHandler() {
            @Override
            public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
                EntityPoseManager.setPose(neko, Pose.CROUCHING);
                TickTaskQueue queue = new TickTaskQueue();
                queue.addTask(40, () -> {
                    if (!neko.isRemoved()) EntityPoseManager.remove(neko);
                });
                TickTasks.add(queue);
                neko.playExpressAnim("pet_request");
                String msg = LanguageUtil.translatable("misc.toneko.ai.actions.pet_request.message");
                Messaging.sendNekoChat(speaker, neko, msg);
                neko.level().playSound(null, neko.getX(), neko.getY(), neko.getZ(),
                        SoundEvents.CAT_PURR, neko.getSoundSource(), 1.0f, 1.0f);
                return true;
            }
            @Override
            public String getGuideLine() {
                return "{\"action\": \"pet_request\"} - "
                        + LanguageUtil.translatable("misc.toneko.ai.actions.guide.pet_request");
            }
        });
        // 呼噜：呼噜音效 + 开心的本地消息
        register("purr", new NekoActionHandler() {
            @Override
            public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
                neko.playExpressAnim("purr");
                neko.level().playSound(null, neko.getX(), neko.getY(), neko.getZ(),
                        SoundEvents.CAT_PURR, neko.getSoundSource(), 1.0f, 1.0f);
                String msg = LanguageUtil.translatable("misc.toneko.ai.actions.purr.message");
                Messaging.sendNekoChat(speaker, neko, msg);
                return true;
            }
            @Override
            public String getGuideLine() {
                return "{\"action\": \"purr\"} - "
                        + LanguageUtil.translatable("misc.toneko.ai.actions.guide.purr");
            }
        });
        // 蹭蹭：先走向目标，贴脸后才蹭（音符粒子 + 音效）
        register("nuzzle", new NekoActionHandler() {
            @Override
            public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
                if (target == null) return false;
                scheduleWalkToArrive(neko, target, () -> {
                    neko.playExpressAnim("nuzzle");
                    ServerLevel level = (ServerLevel) neko.level();
                    level.sendParticles(ParticleTypes.NOTE,
                            neko.getX(), neko.getY() + 1.0, neko.getZ(), 6, 0.4, 0.4, 0.4, 0.05);
                    level.playSound(null, neko.getX(), neko.getY(), neko.getZ(),
                            SoundEvents.CAT_AMBIENT, neko.getSoundSource(), 1.0f, 1.0f);
                });
                return true;
            }
            @Override
            public String getGuideLine() {
                return "{\"action\": \"nuzzle\", \"target\": \"Steve\"} - "
                        + LanguageUtil.translatable("misc.toneko.ai.actions.guide.nuzzle");
            }
        });
        // 从玩家背包拿物品（只能拿对方背包里有的；背包满则放回）
        register("take_item", new NekoActionHandler() {
            @Override
            public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
                if (!(target instanceof Player player)) return false;
                Item item = parseItem(action.item());
                if (item == null) return false;
                int count = Math.max(1, action.count());
                Inventory inv = player.getInventory();
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack stack = inv.getItem(i);
                    if (stack.isEmpty() || stack.getItem() != item) continue;
                    ItemStack taken = inv.removeItem(i, count);
                    if (!neko.addItem(taken)) {
                        inv.setItem(i, taken); // 背包满：原位放回
                        return false;
                    }
                    return true;
                }
                return false; // 对方背包没有
            }
            @Override
            public String getGuideLine() {
                return "{\"action\": \"take_item\", \"item\": \"minecraft:apple\", \"count\": 1} - "
                        + LanguageUtil.translatable("misc.toneko.ai.actions.guide.take_item");
            }
        });
        // 拾取附近掉落物（item 可指定，留空拾取最近的心仪掉落物）：走过去捡起来
        register("pickup_item", new NekoActionHandler() {
            @Override
            public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
                if (!neko.getInventory().canAdd()) return false;
                Item filter = parseItem(action.item());
                ItemEntity found = null;
                for (ItemEntity e : EntityUtil.getItemEntitiesInRange(neko, neko.level(), 12)) {
                    if (filter == null) {
                        if (neko.isLikedItem(e.getItem())) { found = e; break; }
                    } else if (e.getItem().getItem() == filter) {
                        found = e; break;
                    }
                }
                if (found == null) return false;
                final ItemEntity itemEntity = found;
                NekoBrain brain = neko.getNekoBrain();
                if (brain == null) return false;
                brain.submitMove(itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), 1.0,
                        BehaviorPriority.HIGH, ACTION_SOURCE);
                // 轮询：靠近 1.2 格内拾取（超时 10 秒取消）
                TickTaskQueue queue = new TickTaskQueue();
                java.util.concurrent.atomic.AtomicInteger waited = new java.util.concurrent.atomic.AtomicInteger(0);
                Runnable poll = new Runnable() {
                    @Override
                    public void run() {
                        if (neko.isRemoved() || itemEntity.isRemoved() || !itemEntity.isAlive()) return;
                        if (neko.distanceTo(itemEntity) < 1.2) {
                            if (neko.addItem(itemEntity.getItem())) {
                                itemEntity.remove(Entity.RemovalReason.DISCARDED);
                            }
                            return;
                        }
                        if (waited.addAndGet(10) >= 200) return; // 10 秒没走到：放弃
                        queue.addTask(10, this);
                    }
                };
                queue.addTask(10, poll);
                TickTasks.add(queue);
                return true;
            }
            @Override
            public String getGuideLine() {
                return "{\"action\": \"pickup_item\", \"item\": \"minecraft:apple\"} - "
                        + LanguageUtil.translatable("misc.toneko.ai.actions.guide.pickup_item");
            }
        });
        // 找另一只猫娘玩耍：先走向对方，贴脸后跳跳 + 邀请消息
        register("play_with", new NekoActionHandler() {
            @Override
            public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
                if (!(target instanceof NekoEntity other)) return false;
                scheduleWalkToArrive(neko, other, () -> {
                    neko.playExpressAnim("happy_jump");
                    ServerLevel level = (ServerLevel) neko.level();
                    level.sendParticles(ParticleTypes.NOTE,
                            neko.getX(), neko.getY() + 1.0, neko.getZ(), 8, 0.4, 0.4, 0.4, 0.05);
                    level.playSound(null, neko.getX(), neko.getY(), neko.getZ(),
                            SoundEvents.CAT_AMBIENT, neko.getSoundSource(), 1.0f, 1.0f);
                    Messaging.sendNekoChat(speaker, neko,
                            LanguageUtil.translatable("misc.toneko.ai.actions.play.message"));
                });
                return true;
            }
            @Override
            public String getGuideLine() {
                return "{\"action\": \"play_with\", \"target\": \"小花\"} - "
                        + LanguageUtil.translatable("misc.toneko.ai.actions.guide.play_with");
            }
        });
        // 给其他猫娘/玩家梳毛（groom）：先走向目标，贴脸后梳毛表现 + 温柔消息
        register("groom", new NekoActionHandler() {
            @Override
            public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
                if (target == null) return false;
                scheduleWalkToArrive(neko, target, () -> {
                    neko.playExpressAnim("purr");
                    ServerLevel level = (ServerLevel) neko.level();
                    level.sendParticles(ParticleTypes.HEART,
                            neko.getX(), neko.getY() + 1.0, neko.getZ(), 6, 0.4, 0.4, 0.4, 0.1);
                    level.playSound(null, neko.getX(), neko.getY(), neko.getZ(),
                            SoundEvents.CAT_PURR, neko.getSoundSource(), 1.0f, 1.0f);
                    Messaging.sendNekoChat(speaker, neko,
                            LanguageUtil.translatable("misc.toneko.ai.actions.groom.message"));
                });
                return true;
            }
            @Override
            public String getGuideLine() {
                return "{\"action\": \"groom\", \"target\": \"小花\"} - "
                        + LanguageUtil.translatable("misc.toneko.ai.actions.guide.groom");
            }
        });
        // 分享食物：从背包拿一个食物给目标（玩家或猫娘）
        register("share_food", new NekoActionHandler() {
            @Override
            public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
                if (target == null) return false;
                for (ItemStack stack : neko.getInventory().items) {
                    if (!stack.isEmpty() && stack.has(DataComponents.FOOD)) {
                        giveItem(neko, target,
                                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), 1);
                        return true;
                    }
                }
                return false; // 背包没有食物
            }
            @Override
            public String getGuideLine() {
                return "{\"action\": \"share_food\", \"target\": \"Steve\"} - "
                        + LanguageUtil.translatable("misc.toneko.ai.actions.guide.share_food");
            }
        });
        // 晒太阳：白天+露天时回复精力（+10）并呼噜
        register("sunbathe", new NekoActionHandler() {
            @Override
            public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
                net.minecraft.world.level.Level level = neko.level();
                if (!level.isDay() || !level.canSeeSkyFromBelowWater(neko.blockPosition())) return false;
                neko.setNekoEnergy(neko.getNekoEnergy() + 10);
                neko.playExpressAnim("purr");
                level.playSound(null, neko.getX(), neko.getY(), neko.getZ(),
                        SoundEvents.CAT_PURR, neko.getSoundSource(), 1.0f, 1.0f);
                ((ServerLevel) level).sendParticles(ParticleTypes.NOTE,
                        neko.getX(), neko.getY() + 1.0, neko.getZ(), 6, 0.4, 0.4, 0.4, 0.05);
                Messaging.sendNekoChat(speaker, neko,
                        LanguageUtil.translatable("misc.toneko.ai.actions.sunbathe.message"));
                return true;
            }
            @Override
            public String getGuideLine() {
                return "{\"action\": \"sunbathe\"} - "
                        + LanguageUtil.translatable("misc.toneko.ai.actions.guide.sunbathe");
            }
        });
        // 放火把：背包有火把时在附近空气方块放置并消耗一根
        register("light_place", new NekoActionHandler() {
            @Override
            public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
                net.minecraft.world.level.Level level = neko.level();
                NekoInventory inv = neko.getInventory();
                int slot = -1;
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack stack = inv.getItem(i);
                    if (!stack.isEmpty() && stack.getItem() == net.minecraft.world.item.Items.TORCH) { slot = i; break; }
                }
                if (slot < 0) return false;
                net.minecraft.core.BlockPos pos = neko.blockPosition();
                for (net.minecraft.core.BlockPos candidate : new net.minecraft.core.BlockPos[]{
                        pos.above(), pos.east(), pos.west(), pos.north(), pos.south()}) {
                    if (level.getBlockState(candidate).isAir()) {
                        level.setBlock(candidate, net.minecraft.world.level.block.Blocks.TORCH.defaultBlockState(), 3);
                        inv.removeItem(slot, 1);
                        return true;
                    }
                }
                return false; // 周围没有可放置的空气方块
            }
            @Override
            public String getGuideLine() {
                return "{\"action\": \"light_place\"} - "
                        + LanguageUtil.translatable("misc.toneko.ai.actions.guide.light_place");
            }
        });
        // 种地：背包有小麦种子且脚下是耕地时种下
        register("plant", new NekoActionHandler() {
            @Override
            public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
                net.minecraft.world.level.Level level = neko.level();
                NekoInventory inv = neko.getInventory();
                int slot = -1;
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack stack = inv.getItem(i);
                    if (!stack.isEmpty() && stack.getItem() == net.minecraft.world.item.Items.WHEAT_SEEDS) { slot = i; break; }
                }
                if (slot < 0) return false;
                net.minecraft.core.BlockPos below = neko.blockPosition().below();
                net.minecraft.core.BlockPos here = neko.blockPosition();
                if (!level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.FARMLAND)) return false;
                if (!level.getBlockState(here).isAir()) return false;
                level.setBlock(here, net.minecraft.world.level.block.Blocks.WHEAT.defaultBlockState(), 3);
                inv.removeItem(slot, 1);
                return true;
            }
            @Override
            public String getGuideLine() {
                return "{\"action\": \"plant\"} - "
                        + LanguageUtil.translatable("misc.toneko.ai.actions.guide.plant");
            }
        });
        // 把背包里的物品放进附近箱子（8 格内最近的箱子；箱子满则放回）
        register("store_item", new NekoActionHandler() {
            @Override
            public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
                Item item = parseItem(action.item());
                if (item == null) return false;
                NekoInventory inv = neko.getInventory();
                int slot = -1;
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack stack = inv.getItem(i);
                    if (!stack.isEmpty() && stack.getItem() == item) { slot = i; break; }
                }
                if (slot < 0) return false;
                ChestBlockEntity chest = findNearbyChest((ServerLevel) neko.level(), neko.blockPosition(), 8);
                if (chest == null) return false;
                ItemStack toStore = inv.removeItem(slot, Math.max(1, action.count()));
                ItemStack leftover = putItemInContainer(chest, toStore);
                if (!leftover.isEmpty()) neko.addItem(leftover); // 箱子满：剩余放回背包
                return true;
            }
            @Override
            public String getGuideLine() {
                return "{\"action\": \"store_item\", \"item\": \"minecraft:apple\", \"count\": 1} - "
                        + LanguageUtil.translatable("misc.toneko.ai.actions.guide.store_item");
            }
        });
        // 从背包拿物品装备/手持（盔甲穿对应槽位，其他拿在手上；换下的旧装备放回背包）
        register("equip", new NekoActionHandler() {
            @Override
            public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
                Item item = parseItem(action.item());
                if (item == null) return false;
                NekoInventory inv = neko.getInventory();
                int slot = -1;
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack stack = inv.getItem(i);
                    if (!stack.isEmpty() && stack.getItem() == item) { slot = i; break; }
                }
                if (slot < 0) return false;
                ItemStack stack = inv.removeItem(slot, 1);
                // 盔甲穿对应部位，其他物品拿在主手
                EquipmentSlot equipSlot = stack.getItem() instanceof net.minecraft.world.item.ArmorItem armor
                        ? armor.getEquipmentSlot() : EquipmentSlot.MAINHAND;
                if (equipSlot == null || equipSlot == EquipmentSlot.MAINHAND || equipSlot == EquipmentSlot.OFFHAND) {
                    // 手持（或非装备类物品）：拿在主手
                    ItemStack old = neko.getItemInHand(InteractionHand.MAIN_HAND);
                    neko.setItemInHand(InteractionHand.MAIN_HAND, stack);
                    if (!old.isEmpty() && !neko.addItem(old)) {
                        neko.spawnAtLocation(old); // 背包满：旧装备掉落
                    }
                } else {
                    ItemStack old = neko.getItemBySlot(equipSlot);
                    neko.setItemSlot(equipSlot, stack);
                    if (!old.isEmpty() && !neko.addItem(old)) {
                        neko.spawnAtLocation(old);
                    }
                }
                return true;
            }
            @Override
            public String getGuideLine() {
                return "{\"action\": \"equip\", \"item\": \"minecraft:iron_sword\"} - "
                        + LanguageUtil.translatable("misc.toneko.ai.actions.guide.equip");
            }
        });
        register("give_item", new NekoActionHandler() {
            @Override
            public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
                giveItem(neko, target, action.item(), action.count());
                return true;
            }
            @Override
            public String getGuideLine() {
                return "{\"action\": \"give_item\", \"item\": \"minecraft:apple\", \"count\": 1, \"target\": \"Steve\"} - "
                        + LanguageUtil.translatable("misc.toneko.ai.actions.guide.give_item");
            }
        });
        register("follow", new NekoActionHandler() {
            @Override
            public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
                if (target instanceof net.minecraft.world.entity.player.Player followTarget) {
                    neko.followOwner(followTarget);
                    return true;
                }
                return false;
            }
            @Override
            public String getGuideLine() {
                return "{\"action\": \"follow\", \"target\": \"Steve\"} - "
                        + LanguageUtil.translatable("misc.toneko.ai.actions.guide.follow");
            }
        });
        register("stop_follow", new NekoActionHandler() {
            @Override
            public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
                var goal = neko.getFollowingOwner();
                if (goal != null) {
                    goal.stop();
                    return true;
                }
                return false;
            }
            @Override
            public String getGuideLine() {
                return "{\"action\": \"stop_follow\"} - "
                        + LanguageUtil.translatable("misc.toneko.ai.actions.guide.stop_follow");
            }
        });
        register("lie", new NekoActionHandler() {
            @Override
            public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
                // 躺下/恢复站立（toggle）
                if (EntityPoseManager.contains(neko)) {
                    EntityPoseManager.remove(neko);
                } else {
                    EntityPoseManager.setPose(neko, Pose.SLEEPING);
                }
                return true;
            }
            @Override
            public String getGuideLine() {
                return "{\"action\": \"lie\"} - "
                        + LanguageUtil.translatable("misc.toneko.ai.actions.guide.lie");
            }
        });
        register("stand", new NekoActionHandler() {
            @Override
            public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
                EntityPoseManager.remove(neko);
                return true;
            }
            @Override
            public String getGuideLine() {
                return "{\"action\": \"stand\"} - "
                        + LanguageUtil.translatable("misc.toneko.ai.actions.guide.stand");
            }
        });
        register("jump", new NekoActionHandler() {
            @Override
            public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
                neko.jumpFromGround();
                return true;
            }
            @Override
            public String getGuideLine() {
                return "{\"action\": \"jump\"} - "
                        + LanguageUtil.translatable("misc.toneko.ai.actions.guide.jump");
            }
        });
        register("eat", new NekoActionHandler() {
            @Override
            public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
                // 从背包找食物吃掉回血（eatOrStoreFood：血满且无效果时自动存回）
                for (int i = 0; i < neko.getInventory().getContainerSize(); i++) {
                    ItemStack stack = neko.getInventory().getItem(i);
                    if (stack.isEmpty() || !stack.has(net.minecraft.core.component.DataComponents.FOOD)) continue;
                    neko.getInventory().removeItem(i, 1);
                    neko.eatOrStoreFood(stack.copy());
                    return true;
                }
                return false;
            }
            @Override
            public String getGuideLine() {
                return "{\"action\": \"eat\"} - "
                        + LanguageUtil.translatable("misc.toneko.ai.actions.guide.eat");
            }
        });
        register("mate", new NekoActionHandler() {
            @Override
            public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
                // 猫娘主动发起交配（目标可为玩家或其他猫娘，canMate 会校验双方条件）
                if (target instanceof INeko mateTarget) {
                    neko.tryMating((ServerLevel) neko.level(), mateTarget);
                    return true;
                }
                return false;
            }
            @Override
            public String getGuideLine() {
                return "{\"action\": \"mate\", \"target\": \"Steve\"} - "
                        + LanguageUtil.translatable("misc.toneko.ai.actions.guide.mate");
            }
        });
        register("give_diary", new NekoActionHandler() {
            @Override
            public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
                // 给目标一本猫娘日记成书（有 AI 写的日记数据时用数据，无数据回退随机）
                giveToPlayer(target, neko.createNekoDiary());
                return true;
            }
            @Override
            public String getGuideLine() {
                return "{\"action\": \"give_diary\"} - "
                        + LanguageUtil.translatable("misc.toneko.ai.actions.guide.give_diary");
            }
        });
        register("write_diary", new NekoActionHandler() {
            @Override
            public boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action) {
                // 写日记冷却（游戏 tick，配置秒数×20；last<=0 表示从未写过，放行；
                // 同一次回复连发多个 write_diary 会自然被冷却拦截）
                int cooldownSec = ConfigUtil.getAIActionsDiaryCooldown();
                long now = neko.level().getGameTime();
                long last = neko.getLastDiaryWriteTime();
                if (cooldownSec > 0 && last > 0 && now - last < cooldownSec * 20L) {
                    LOGGER.info("[AI-ACTION] write_diary skipped (cooldown) for {}", neko.getName().getString());
                    return false;
                }
                // 正文清洗：去 § 格式码、trim、限长（保留换行）；空正文拒绝
                String text = action.text();
                if (text == null || text.isBlank()) return false;
                text = text.replaceAll("§[0-9a-fk-orK-OR]", "").trim();
                if (text.length() > NekoDiary.MAX_BODY_LENGTH) {
                    text = text.substring(0, NekoDiary.MAX_BODY_LENGTH);
                }

                // 环境元数据：复用 Prompts 的翻译工厂（other 传 null 安全）
                String weather = Prompts.WORLD_WEATHER.getPrompt(neko, null);
                String mood = Prompts.NEKO_MOOD.getPrompt(neko, null);
                String biome = Prompts.WORLD_BIOME.getPrompt(neko, null);
                String dimension = Prompts.WORLD_DIMENSION.getPrompt(neko, null);

                // 无日记：先用随机条目 + 当前真实环境初始化（用户要求：没有日记就随机）
                if (neko.getDiaryEntries().isEmpty()) {
                    String nekoName = neko.getCustomName() != null
                            ? neko.getCustomName().getString()
                            : neko.getName().getString();
                    for (String seed : NekoDiary.seedEntries(neko.getRandom(), nekoName,
                            weather, mood, biome, dimension)) {
                        neko.appendDiaryEntry(seed);
                    }
                }
                neko.appendDiaryEntry(NekoDiary.composeEntry(weather, mood, biome, dimension, text));
                neko.setLastDiaryWriteTime(now);
                LOGGER.info("[AI-ACTION] {} wrote a diary entry", neko.getName().getString());
                return true;
            }
            @Override
            public String getGuideLine() {
                return "{\"action\": \"write_diary\", \"text\": \"今天...\"} - "
                        + LanguageUtil.translatable("misc.toneko.ai.actions.guide.write_diary");
            }
        });
    }

    /**
     * 便捷入口：解析 AI 回复中的动作并执行，返回清理掉 JSON 代码块后的显示文本。
     * 未启用 AI 动作时原样返回文本。必须在服务器主线程调用。
     */
    public static String process(NekoEntity neko, ServerPlayer player, String responseText) {
        if (!ConfigUtil.isAIActionsEnabled()) return responseText;
        NekoActionParser.ParseResult result = NekoActionParser.parse(responseText);
        execute(neko, player, result.actions());
        return result.cleanedText();
    }

    /** 执行动作列表：按 type 分发到注册的处理器，单个动作失败不影响其它 */
    public static void execute(NekoEntity neko, ServerPlayer player, List<NekoAction> actions) {
        if (actions == null || actions.isEmpty()) return;
        for (NekoAction action : actions) {
            NekoActionHandler handler = HANDLERS.get(action.type());
            if (handler == null) {
                LOGGER.warn("[AI-ACTION] unknown action type: {}", action.type());
                continue;
            }
            try {
                // 解析目标：缺省为说话者，指定名字时查找玩家或范围内实体
                LivingEntity target = resolveTarget(neko, player, action);
                if (target == null) {
                    LOGGER.warn("[AI-ACTION] target not found for {}: {}", action.type(), action.target());
                    continue;
                }
                handler.handle(neko, player, target, action);
            } catch (Exception e) {
                LOGGER.warn("[AI-ACTION] failed to execute {}: {}", action.type(), e.toString());
            }
        }
    }

    /**
     * 解析动作目标：target 为空或缺省 → 说话者；
     * 指定名字 → 在线玩家（忽略大小写）→ 同维度 64 格内实体按昵称/实体名/显示名匹配（支持指向其他猫娘）；
     * 找不到返回 null（动作跳过，不误执行）。
     */
    private static LivingEntity resolveTarget(NekoEntity neko, ServerPlayer speaker, NekoAction action) {
        String targetName = action.target();
        if (targetName == null || targetName.isBlank()) return speaker;

        // 1. 在线玩家按名字（忽略大小写）
        for (ServerPlayer p : neko.level().getServer().getPlayerList().getPlayers()) {
            if (p.getName().getString().equalsIgnoreCase(targetName)) return p;
        }
        // 2. 同维度 64 格内实体：昵称（猫娘优先）→ 实体名 → 显示名
        AABB box = neko.getBoundingBox().inflate(TARGET_SEARCH_RANGE);
        for (Entity entity : neko.level().getEntities(neko, box)) {
            if (entity == neko) continue;
            if (entity instanceof NekoEntity n) {
                String nick = n.getNickName();
                if (nick != null && !nick.isEmpty() && nick.equalsIgnoreCase(targetName)) {
                    return n;
                }
            }
            if (entity.getName().getString().equalsIgnoreCase(targetName)) {
                return entity instanceof LivingEntity living ? living : null;
            }
            String displayName = entity.getDisplayName().getString();
            if (displayName.equalsIgnoreCase(targetName)) return entity instanceof LivingEntity living ? living : null;
        }
        return null;
    }

    /**
     * 生成注入 prompt 的动作说明文本（由所有已注册动作的 guide 行组成）。
     */
    public static String actionGuide() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(LanguageUtil.translatable("misc.toneko.ai.actions.guide.header"));
        for (NekoActionHandler handler : HANDLERS.values()) {
            String line = handler.getGuideLine();
            if (line != null && !line.isEmpty()) {
                sb.append("\n").append(line);
            }
        }
        String footer = LanguageUtil.translatable("misc.toneko.ai.actions.guide.footer");
        if (!footer.isEmpty() && !footer.equals("misc.toneko.ai.actions.guide.footer")) {
            sb.append("\n").append(footer);
        }
        return sb.toString();
    }

    /** 给予物品：背包优先，背包没有且配置允许时虚拟生成（消耗猫娘能量） */
    private static void giveItem(NekoEntity neko, LivingEntity target, String itemId, int count) {
        if (itemId == null || itemId.isEmpty()) return;
        Item item = parseItem(itemId);
        if (item == null) {
            LOGGER.warn("[AI-ACTION] unknown item id: {}", itemId);
            return;
        }

        // 1. 背包优先
        int slot = neko.getInventory().findSlotMatchingItem(new ItemStack(item));
        if (slot >= 0) {
            ItemStack stack = neko.getInventory().removeItem(slot, count);
            if (!stack.isEmpty()) {
                giveToPlayer(target, stack);
                return;
            }
        }

        // 2. 虚拟生成（配置开启且能量足够）
        if (ConfigUtil.isAIActionsVirtualItems()) {
            int cost = Math.max(1, ConfigUtil.getAIActionsEnergyCost());
            if (neko.getNekoEnergy() >= cost) {
                neko.setNekoEnergy(neko.getNekoEnergy() - cost);
                giveToPlayer(target, new ItemStack(item, count));
                return;
            }
        }
        LOGGER.info("[AI-ACTION] neko has no {} in inventory and cannot generate (virtual={} energy={})",
                itemId, ConfigUtil.isAIActionsVirtualItems(), neko.getNekoEnergy());
    }

    /** 给目标物品：玩家背包放不下时掉落到目标位置 */
    private static void giveToPlayer(LivingEntity target, ItemStack stack) {
        if (target instanceof ServerPlayer player) {
            if (!player.addItem(stack)) {
                player.drop(stack, false);
            }
        } else {
            // 非玩家实体：掉落到其位置
            target.spawnAtLocation(stack);
        }
    }

    private static Item parseItem(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) return null;
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == null || item == net.minecraft.world.item.Items.AIR ? null : item;
    }

    /** 走到目标贴脸判定距离（≤1 格，与猫娘间对话一致） */
    private static final double FACE_TO_FACE_SQ = 1.0 * 1.0;
    /** 走向目标再执行表现的超时（15 秒），走不到则放弃 */
    private static final int WALK_TO_ARRIVE_TIMEOUT = 300;

    /**
     * 走向目标，贴脸（≤3 格）后才执行 onArrive 表现（动画/粒子/音效等）。
     * 已在范围内直接执行；目标中途失效或 15 秒走不到则放弃。
     * 与 {@link NekoTalkManager} 的"走到再说话"、pickup_item 的"走到再拾取"同一模式。
     */
    private static void scheduleWalkToArrive(NekoEntity neko, LivingEntity target, Runnable onArrive) {
        if (neko.distanceToSqr(target) <= FACE_TO_FACE_SQ) {
            onArrive.run(); // 已经贴脸：直接表现
            return;
        }
        NekoBrain brain = neko.getNekoBrain();
        if (brain == null) {
            onArrive.run(); // 无移动能力：原地表现
            return;
        }
        brain.submitMove(target, 1.0, BehaviorPriority.HIGH, ACTION_SOURCE);
        TickTaskQueue queue = new TickTaskQueue();
        java.util.concurrent.atomic.AtomicInteger waited = new java.util.concurrent.atomic.AtomicInteger(0);
        Runnable poll = new Runnable() {
            @Override
            public void run() {
                if (neko.isRemoved() || target.isRemoved() || !target.isAlive()) return; // 中途失效：放弃
                if (neko.distanceToSqr(target) <= FACE_TO_FACE_SQ) {
                    onArrive.run(); // 走到了：执行表现
                    return;
                }
                if (waited.addAndGet(10) >= WALK_TO_ARRIVE_TIMEOUT) return; // 走不到：放弃
                queue.addTask(10, this);
            }
        };
        queue.addTask(10, poll);
        TickTasks.add(queue);
    }

    /**
     * 把物品放入容器：优先合并到同物品堆叠，其次空槽；返回未能放入的剩余（容器满）。
     */
    private static ItemStack putItemInContainer(net.minecraft.world.Container container, ItemStack stack) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slot = container.getItem(i);
            if (slot.isEmpty()) {
                container.setItem(i, stack);
                return ItemStack.EMPTY;
            }
            if (ItemStack.isSameItemSameComponents(slot, stack) && slot.getCount() < slot.getMaxStackSize()) {
                int move = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
                slot.grow(move);
                stack.shrink(move);
                if (stack.isEmpty()) return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    /** 在指定范围内搜索最近的箱子（ChestBlockEntity），与 NekoCropGatheringGoal 语义一致 */
    private static ChestBlockEntity findNearbyChest(ServerLevel level, net.minecraft.core.BlockPos center, int radius) {
        ChestBlockEntity closest = null;
        double minDistSq = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    net.minecraft.core.BlockPos pos = center.offset(dx, dy, dz);
                    net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof ChestBlockEntity chest) {
                        double distSq = center.distSqr(pos);
                        if (distSq < minDistSq) {
                            minDistSq = distSq;
                            closest = chest;
                        }
                    }
                }
            }
        }
        return closest;
    }

    /**
     * 动作处理器：处理指定类型的动作，并贡献 prompt 中的动作说明行。
     */
    public interface NekoActionHandler {
        /**
         * 执行动作。
         * @param neko   执行动作的猫娘
         * @param speaker 正在与猫娘对话的玩家
         * @param target 动作目标（action.target 解析结果，缺省为 speaker；找不到时不会调用）
         * @return 是否成功执行（用于日志/调试）
         */
        boolean handle(NekoEntity neko, ServerPlayer speaker, LivingEntity target, NekoAction action);

        /**
         * 该动作的 guide 行（JSON 示例 + 说明，注入 prompt 用）。
         * 说明文本建议使用 {@link LanguageUtil#translatable(String)} 支持多语言。
         * 返回空字符串表示不显示在 guide 中。
         */
        default String getGuideLine() {
            return "";
        }
    }
}
