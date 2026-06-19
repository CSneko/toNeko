package org.cneko.toneko.common.mod.items;

import lombok.Getter;
import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.misc.ToNekoDamageTypes;
import org.cneko.toneko.common.mod.misc.ToNekoEnchantments;
import org.cneko.toneko.common.mod.util.EnchantmentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.cneko.toneko.common.mod.effects.ToNekoEffects.HISS_INTIMIDATION_EFFECT;

public class NekoEnergyBurstItem extends Item {
    @Getter
    private final float damage;
    @Getter
    private final float radius;
    @Getter
    private final float energyCost;

    // ========== 连击系统 ==========
    private static final Map<UUID, ComboState> COMBO_MAP = new ConcurrentHashMap<>();
    private static final long COMBO_WINDOW_MS = 5000; // 基础5秒连击窗口（附魔可延长）

    // ========== BossBar 连击HUD ==========
    private static final Map<UUID, ActiveCombo> ACTIVE_COMBOS = new ConcurrentHashMap<>();

    private record ComboState(int count, long lastUseTime) {}

    private static class ActiveCombo {
        final ServerBossEvent bossBar;
        long expirationTime;
        long totalWindowMs;
        int comboCount;
        boolean evil; // 邪恶版样式

        ActiveCombo(ServerBossEvent bossBar, long expirationTime, long totalWindowMs, int comboCount, boolean evil) {
            this.bossBar = bossBar;
            this.expirationTime = expirationTime;
            this.totalWindowMs = totalWindowMs;
            this.comboCount = comboCount;
            this.evil = evil;
        }
    }

    public NekoEnergyBurstItem(float damage, float radius, float energyCost) {
        super(new Properties().stacksTo(1).durability(200));
        this.damage = damage;
        this.radius = radius;
        this.energyCost = energyCost;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand usedHand) {
        if (level.isClientSide) return super.use(level, player, usedHand);
        if (!player.isNeko()){
            player.displayClientMessage(Component.translatable(getBroadcastKeyPrefix() + ".not_neko"),true);
            return InteractionResultHolder.fail(player.getItemInHand(usedHand));
        }
        if (player.getNekoEnergy() < energyCost){
            player.displayClientMessage(Component.translatable(getBroadcastKeyPrefix() + ".not_enough_energy"),true);
            return InteractionResultHolder.fail(player.getItemInHand(usedHand));
        }

        // ---- 读取附魔等级 ----
        ItemStack stack = player.getItemInHand(usedHand);
        int powerLevel = EnchantmentUtil.getEnchantmentLevel(ToNekoEnchantments.HISS_POWER, stack, level);
        int spreadLevel = EnchantmentUtil.getEnchantmentLevel(ToNekoEnchantments.HISS_SPREAD, stack, level);
        int efficiencyLevel = EnchantmentUtil.getEnchantmentLevel(ToNekoEnchantments.HISS_EFFICIENCY, stack, level);
        int extendLevel = EnchantmentUtil.getEnchantmentLevel(ToNekoEnchantments.COMBO_EXTEND, stack, level);

        // 附魔加成系数
        float enchDamageMult = 1.0f + powerLevel * 0.20f;       // 哈气强化：每级+20%伤害
        float enchRadiusMult = 1.0f + spreadLevel * 0.15f;       // 哈气扩散：每级+15%范围
        float enchEnergyMult = 1.0f - efficiencyLevel * 0.15f;   // 节能哈气：每级-15%能量消耗
        long extendedWindow = COMBO_WINDOW_MS + extendLevel * 5000L; // 连击延续：每级+5秒窗口

        // ---- 读取原版附魔等级 ----
        int sharpnessLevel = EnchantmentUtil.getEnchantmentLevel(Enchantments.SHARPNESS, stack, level);
        int smiteLevel = EnchantmentUtil.getEnchantmentLevel(Enchantments.SMITE, stack, level);
        int baneLevel = EnchantmentUtil.getEnchantmentLevel(Enchantments.BANE_OF_ARTHROPODS, stack, level);
        int fireAspectLevel = EnchantmentUtil.getEnchantmentLevel(Enchantments.FIRE_ASPECT, stack, level);
        int knockbackLevel = EnchantmentUtil.getEnchantmentLevel(Enchantments.KNOCKBACK, stack, level);
        int hissRootLevel = EnchantmentUtil.getEnchantmentLevel(ToNekoEnchantments.HISS_ROOT, stack, level);
        int demolishLevel = EnchantmentUtil.getEnchantmentLevel(ToNekoEnchantments.HISS_DEMOLISH, stack, level);
        // 锋利基础加成（与其他两者互斥，原版1.21自动处理）
        float sharpnessBonus = sharpnessLevel > 0 ? 0.5f + 0.5f * sharpnessLevel : 0;

        // ---- 连击计算（仅当命中生物时才累计连击） ----
        long now = System.currentTimeMillis();
        ComboState oldState = COMBO_MAP.get(player.getUUID());
        int previousHits = (oldState != null) ? oldState.count() : 0;
        boolean withinWindow = oldState != null && (now - oldState.lastUseTime() < extendedWindow);
        int effectiveCombo = withinWindow ? previousHits : 0; // 本次的有效连击等级

        // 对数伤害倍率：无上限，越往后增长越慢
        float comboMultiplier = effectiveCombo > 0
            ? 1.0f + (float) Math.log(effectiveCombo + 1) * 0.25f
            : 1.0f;
        int comboBonusLevel = effectiveCombo / 3; // 每3连击+1级威慑

        // ---- 彩蛋判定：圆头耄耋降临（5%概率） ----
        boolean isEasterEgg = level.getRandom().nextFloat() < 0.05f;
        if (isEasterEgg) {
            comboMultiplier = 2.0f; // 伤害范围翻倍！
        }

        // ---- 计算最终数值（基础×等级×连击×附魔） ----
        float finalDamage = (damage + damage * player.getNekoLevel() * 0.02f) * comboMultiplier * enchDamageMult;
        float finalRadius = (radius + player.getNekoLevel() * 0.02f) * comboMultiplier * enchRadiusMult;

        // 扣除能量（附魔减免后）
        float actualEnergyCost = energyCost * enchEnergyMult;
        player.setNekoEnergy(player.getNekoEnergy() - actualEnergyCost);

        // ---- 音效 ----
        playHissSound(level, player, effectiveCombo, isEasterEgg);

        // ---- 粒子大秀 ----
        spawnHissSpray((ServerLevel) level, player);           // Layer 1: 哈气喷射
        spawnEnergyShockRing((ServerLevel) level, player, finalRadius); // Layer 2: 能量冲击环
        spawnRoundHeadHalo((ServerLevel) level, player);       // Layer 4: 圆头光环
        spawnLingeringAura((ServerLevel) level, player);       // Layer 3: 威慑气场
        if (isEasterEgg) {
            spawnEasterEggParticles((ServerLevel) level, player); // Layer 5: 彩蛋特效
        }

        // ---- 统计目标 ----
        AtomicInteger hitCount = new AtomicInteger(0);
        AtomicInteger healCount = new AtomicInteger(0);
        AtomicReference<String> firstTargetName = new AtomicReference<>("");
        AtomicReference<Float> levelUp = new AtomicReference<>((float) 0);

        // ---- 范围效果 ----
        player.level().getEntities(player,
                player.getBoundingBox().inflate(finalRadius),
                getEntityFilter(player))
            .forEach(entity -> {
                if (entity instanceof LivingEntity livingEntity) {
                    if (isFriendlyTarget(livingEntity)) {
                        // 治疗猫娘同伴
                        livingEntity.heal(finalDamage * 0.5f);
                        levelUp.updateAndGet(v -> v + 0.01f);
                        healCount.incrementAndGet();
                        // 爱心粒子
                        for (int i = 0; i < 10; i++) {
                            double offsetX = (entity.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
                            double offsetY = entity.getRandom().nextDouble() * entity.getBbHeight();
                            double offsetZ = (entity.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
                            entity.level().addParticle(ParticleTypes.HEART,
                                entity.getX() + offsetX, entity.getY() + offsetY, entity.getZ() + offsetZ,
                                0, 0.1, 0);
                        }
                    } else {
                        // ---- 原版附魔伤害加成 ----
                        float vanillaBonus = sharpnessBonus;
                        if (smiteLevel > 0 && livingEntity.getType().is(EntityTypeTags.SENSITIVE_TO_SMITE)) {
                            vanillaBonus += 2.5f * smiteLevel;
                        }
                        if (baneLevel > 0 && livingEntity.getType().is(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)) {
                            vanillaBonus += 2.5f * baneLevel;
                        }

                        // 哈气伤害（基础伤害 + 原版附魔加成）
                        float entityDamage = finalDamage + vanillaBonus;
                        livingEntity.hurt(ToNekoDamageTypes.hissDamage(player), entityDamage);
                        levelUp.updateAndGet(v -> v + 0.005f);
                        hitCount.incrementAndGet();
                        if (firstTargetName.get().isEmpty()) {
                            firstTargetName.set(livingEntity.getName().getString());
                        }

                        // 火焰附加：点燃目标
                        if (fireAspectLevel > 0) {
                            livingEntity.setRemainingFireTicks(fireAspectLevel * 80); // 每级4秒
                        }

                        // 哈气威慑效果
                        if (livingEntity instanceof Mob mob) {
                            int duration = 60 + comboBonusLevel * 40; // 基础3秒 + combo加成
                            mob.addEffect(new MobEffectInstance(
                                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(HISS_INTIMIDATION_EFFECT), duration, 0));

                            if (hissRootLevel > 0) {
                                // 定身哈气：取消击退，改为强效缓慢（钉在原地）
                                int slowAmp = 4 + hissRootLevel; // I级=缓慢V(75%), II级=缓慢VI(87%)
                                int slowDuration = 60 + hissRootLevel * 20; // I级=3秒, II级=4秒
                                mob.addEffect(new MobEffectInstance(
                                    MobEffects.MOVEMENT_SLOWDOWN, slowDuration, slowAmp));
                            } else {
                                // 正常击退（基础 + combo + 原版击退附魔）
                                float kbPower = 2.5f + comboBonusLevel * 0.5f + knockbackLevel * 1.5f;
                                Vec3 knockback = mob.position()
                                    .subtract(player.position())
                                    .normalize()
                                    .scale(kbPower);
                                mob.push(knockback.x, 0.35, knockback.z);
                            }
                        }

                        // 受伤粒子
                        for (int i = 0; i < 10; i++) {
                            double offsetX = (entity.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
                            double offsetY = entity.getRandom().nextDouble() * entity.getBbHeight();
                            double offsetZ = (entity.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
                            entity.level().addParticle(ParticleTypes.DAMAGE_INDICATOR,
                                entity.getX() + offsetX, entity.getY() + offsetY, entity.getZ() + offsetZ,
                                0, 0.1, 0);
                        }
                    }
                }
            });

        // ---- 破坏哈气：范围内方块销毁 ----
        if (demolishLevel > 0) {
            // 硬度上限随等级提升（不上不下的石头刚好卡住）
            //   Lv1 (1.5): 泥土/沙子/玻璃/地狱岩/石头
            //   Lv2 (2.0): +圆石/木板/原木/混凝土
            //   Lv3 (3.0): +各类矿石/末地石/金块  （无法破坏铁块/钻石块/刷怪笼/黑曜石）
            float threshold = switch (demolishLevel) {
                case 1 -> 1.5f;
                case 2 -> 2.0f;
                default -> 3.0f;
            };
            // 破坏半径独立于伤害半径，有上限（每级+1格，最高5格）
            float demolishRadius = Math.min(finalRadius, 2.0f + demolishLevel * 1.0f);
            // 每级可破坏方块数上限
            int maxBlocks = 8 + demolishLevel * 8;

            BlockPos playerPos = player.blockPosition();
            int r = (int) Math.ceil(demolishRadius);
            int broken = 0;

            // 收集->随机打乱->限制数量，避免总是破坏同一侧的方块
            java.util.List<BlockPos> candidates = new java.util.ArrayList<>();
            BlockPos.betweenClosed(playerPos.offset(-r, -r, -r), playerPos.offset(r, r, r))
                .forEach(pos -> {
                    if (pos.distSqr(playerPos) <= demolishRadius * demolishRadius) {
                        BlockState state = level.getBlockState(pos);
                        float hardness = state.getDestroySpeed(level, pos);
                        if (hardness >= 0 && hardness <= threshold && !state.isAir()) {
                            candidates.add(pos.immutable());
                        }
                    }
                });

            // 随机打乱，保证每次破坏的方块不同
            java.util.Collections.shuffle(candidates, new java.util.Random());

            for (BlockPos pos : candidates) {
                if (broken >= maxBlocks) break;
                level.destroyBlock(pos, true);
                broken++;
            }
        }

        // ---- 邪恶版反噬：使用者自身受伤 ----
        if (isEvil()) {
            player.hurt(ToNekoDamageTypes.hissDamage(player), finalDamage);
        }

        // ---- 更新连击状态（仅命中时累计，未命中则重置） ----
        int finalHits = hitCount.get();
        int newCombo;
        if (finalHits > 0) {
            newCombo = effectiveCombo + 1; // 无上限递增
            COMBO_MAP.put(player.getUUID(), new ComboState(newCombo, now));

            // 创建/更新 BossBar 连击HUD
            updateComboBossBar((ServerPlayer) player, newCombo, comboMultiplier, extendedWindow, now, isEvil());
        } else {
            newCombo = 0;
            COMBO_MAP.remove(player.getUUID());
            // 打空了，移除 BossBar
            removeComboBossBar(player.getUUID());
        }

        // ---- 聊天栏广播 ----
        broadcastHissMessage(player, finalHits, healCount.get(), firstTargetName.get(), newCombo, isEasterEgg);

        // ---- 消耗耐久 ----
        player.getItemInHand(usedHand).hurtAndBreak(1, player,
            usedHand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);

        // ---- 冷却 ----
        player.getCooldowns().addCooldown(this, 10);

        // ---- 升级 ----
        org.cneko.toneko.common.mod.api.NekoLevelRegistry.combat().addRaw(player, levelUp.get());

        return InteractionResultHolder.success(player.getItemInHand(usedHand));
    }

    // ========================
    //  音效
    // ========================
    private void playHissSound(Level level, Player player, int combo, boolean isEasterEgg) {
        // 使用原版豹猫哈气声，通过 pitch 变调实现不同层次
        // 注意：level.playSound 的第一个参数是"排除的玩家"（except），传入 null 才能让所有玩家（包括使用者）都听到
        if (isEasterEgg || combo >= 7) {
            // 终极哈气：低沉震撼（彩蛋 或 连击7+）
            level.playSound(null, player.blockPosition(),
                SoundEvents.CAT_HISS,
                SoundSource.NEUTRAL, 1.2f, 0.55f);
        } else {
            // 普通/连击哈气：combo越高音调越尖锐刺耳
            float pitch = 1.1f + combo * 0.15f;
            float volume = Math.min(0.7f + combo * 0.05f, 1.1f);
            level.playSound(null, player.blockPosition(),
                SoundEvents.CAT_HISS,
                SoundSource.NEUTRAL, volume, pitch);
        }
    }

    // ========================
    //  Layer 1: 哈气喷射（锥形喷雾）
    // ========================
    private void spawnHissSpray(ServerLevel level, Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = right.cross(look).normalize();
        double px = player.getX() + look.x * 0.5;
        double py = player.getY() + player.getEyeHeight() - 0.2;
        double pz = player.getZ() + look.z * 0.5;

        // 锥形喷射：从嘴部位置向前方30°锥角扩散
        for (int i = 0; i < 25; i++) {
            double spreadAngle = (player.getRandom().nextDouble() - 0.5) * Math.PI / 3; // ±30°
            double elevAngle = (player.getRandom().nextDouble() - 0.5) * Math.PI / 6;   // ±15°
            double speed = 0.15 + player.getRandom().nextDouble() * 0.3;

            // 在look方向的基础上施加锥形偏离
            Vec3 sprayDir = look.add(
                right.scale(Math.sin(spreadAngle) * 0.6))
                .add(up.scale(Math.sin(elevAngle) * 0.4))
                .normalize();

            level.sendParticles(ParticleTypes.CLOUD,
                px, py, pz,
                1,
                sprayDir.x * speed, sprayDir.y * speed + 0.02, sprayDir.z * speed,
                0.02);

            // 混入少量SNEEZE粒子模拟口水飞溅（更有"哈气"味）
            if (player.getRandom().nextFloat() < 0.3f) {
                level.sendParticles(ParticleTypes.SNEEZE,
                    px, py, pz,
                    1,
                    sprayDir.x * speed * 0.8, sprayDir.y * speed * 0.8, sprayDir.z * speed * 0.8,
                    0.01);
            }
        }
    }

    // ========================
    //  Layer 2: 能量冲击环
    // ========================
    private void spawnEnergyShockRing(ServerLevel level, Player player, float radius) {
        double px = player.getX();
        double py = player.getY() + 0.1;
        double pz = player.getZ();
        int particleCount = 40;

        // 外层：END_ROD 能量环
        for (int i = 0; i < particleCount; i++) {
            double angle = (i * Math.PI * 2) / particleCount;
            double dx = Math.cos(angle) * radius;
            double dz = Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.END_ROD,
                px + dx, py, pz + dz,
                1, 0, 0.05, 0, 0.01);
        }

        // 内层：WITCH 魔法环
        for (int i = 0; i < particleCount; i++) {
            double angle = (i * Math.PI * 2) / particleCount;
            double dx = Math.cos(angle) * radius * 0.5;
            double dz = Math.sin(angle) * radius * 0.5;
            level.sendParticles(ParticleTypes.WITCH,
                px + dx, py, pz + dz,
                1, 0, 0.1, 0, 0.02);
        }

        // 地面冲击波：SONIC_BOOM（监守者音爆粒子）沿地面扩散
        for (int i = 0; i < 16; i++) {
            double angle = (i * Math.PI * 2) / 16;
            double dx = Math.cos(angle) * radius * 1.2;
            double dz = Math.sin(angle) * radius * 1.2;
            level.sendParticles(ParticleTypes.SONIC_BOOM,
                px + dx, py, pz + dz,
                1, 0, 0, 0, 0);
        }
    }

    // ========================
    //  Layer 3: 威慑气场（地面残留粒子）
    // ========================
    private void spawnLingeringAura(ServerLevel level, Player player) {
        double px = player.getX();
        double py = player.getY() + 0.05;
        double pz = player.getZ();

        // 在玩家周围地面生成缓慢上升的白色粒子
        for (int i = 0; i < 30; i++) {
            double angle = player.getRandom().nextDouble() * Math.PI * 2;
            double dist = player.getRandom().nextDouble() * 1.5;
            double ox = Math.cos(angle) * dist;
            double oz = Math.sin(angle) * dist;
            level.sendParticles(ParticleTypes.CLOUD,
                px + ox, py, pz + oz,
                1,
                (player.getRandom().nextDouble() - 0.5) * 0.03,
                0.01 + player.getRandom().nextDouble() * 0.04,
                (player.getRandom().nextDouble() - 0.5) * 0.03,
                0.04);
        }
    }

    // ========================
    //  Layer 4: 圆头光环（头顶粒子圆环）
    // ========================
    private void spawnRoundHeadHalo(ServerLevel level, Player player) {
        double px = player.getX();
        double py = player.getY() + 2.3; // 头顶上方
        double pz = player.getZ();
        int particleCount = 16;
        double haloRadius = 0.5;

        for (int i = 0; i < particleCount; i++) {
            double angle = (i * Math.PI * 2) / particleCount;
            double dx = Math.cos(angle) * haloRadius;
            double dz = Math.sin(angle) * haloRadius;
            level.sendParticles(ParticleTypes.WITCH,
                px + dx, py, pz + dz,
                1, 0, 0.02, 0, 0.01);
        }
    }

    // ========================
    //  Layer 5: 彩蛋粒子（圆头耄耋降临）
    // ========================
    private void spawnEasterEggParticles(ServerLevel level, Player player) {
        double px = player.getX();
        double py = player.getY() + 1.0;
        double pz = player.getZ();
        int particleCount = 50;
        double bigRadius = 2.5;

        // 巨大的圆头轮廓
        for (int i = 0; i < particleCount; i++) {
            double angle = (i * Math.PI * 2) / particleCount;
            // 椭圆形的"圆头"轮廓
            double dx = Math.cos(angle) * bigRadius;
            double dy = Math.sin(angle) * bigRadius * 0.7;
            level.sendParticles(ParticleTypes.WITCH,
                px + dx, py + dy + 1.0, pz,
                1, 0, 0, 0, 0.01);

            // 内层填充
            double innerDx = Math.cos(angle) * bigRadius * 0.6;
            double innerDy = Math.sin(angle) * bigRadius * 0.4;
            level.sendParticles(ParticleTypes.END_ROD,
                px + innerDx, py + innerDy + 1.0, pz,
                1, 0, 0, 0, 0.02);
        }

        // "眼睛"粒子——两个集中的粒子团
        for (int eye = -1; eye <= 1; eye += 2) {
            for (int i = 0; i < 8; i++) {
                double ex = eye * 0.8 + (player.getRandom().nextDouble() - 0.5) * 0.3;
                double ey = 0.2 + (player.getRandom().nextDouble() - 0.5) * 0.2;
                level.sendParticles(ParticleTypes.GLOW,
                    px + ex, py + ey + 1.8, pz - 0.3,
                    1, 0, 0.01, 0, 0.01);
            }
        }

        // 地面冲击扩散
        for (int i = 0; i < 30; i++) {
            double angle = (i * Math.PI * 2) / 30;
            for (int r = 1; r <= 3; r++) {
                double dx = Math.cos(angle) * r * 1.5;
                double dz = Math.sin(angle) * r * 1.5;
                level.sendParticles(ParticleTypes.CLOUD,
                    px + dx, player.getY() + 0.05, pz + dz,
                    1, 0, 0.01, 0, 0.02);
            }
        }
    }

    // ========================
    //  实体分类（子类可覆写以实现不同行为）
    // ========================

    /** 目标过滤：默认排除自己 */
    protected java.util.function.Predicate<net.minecraft.world.entity.Entity> getEntityFilter(Player player) {
        return entity -> entity.isAlive() && entity != player;
    }

    /** 是否对目标造成伤害 */
    protected boolean isHostileTarget(LivingEntity entity) {
        return !(entity instanceof INeko neko && neko.isNeko());
    }

    /** 是否对目标进行治疗 */
    protected boolean isFriendlyTarget(LivingEntity entity) {
        return entity instanceof INeko neko && neko.isNeko();
    }

    /** 广播消息的翻译键前缀（子类覆写以换台词） */
    protected String getBroadcastKeyPrefix() {
        return "item.toneko.neko_energy_burst";
    }

    /** 是否为邪恶版（影响 BossBar 标题样式） */
    protected boolean isEvil() {
        return false;
    }

    // ========================
    //  聊天栏广播
    // ========================
    private void broadcastHissMessage(Player player, int hitCount, int healCount,
                                       String firstTargetName, int combo,
                                       boolean isEasterEgg) {
        String playerName = player.getName().getString();

        // 彩蛋消息优先
        if (isEasterEgg) {
            String msg1 = Component.translatable(getBroadcastKeyPrefix() + ".easter_egg.line1", playerName).getString();
            String msg2 = Component.translatable(getBroadcastKeyPrefix() + ".easter_egg.line2", playerName).getString();
            broadcastRaw(player, msg1);
            broadcastRaw(player, msg2);
            return;
        }

        // 高连击特殊播报（逐级解锁，随机变体）
        if (combo >= 25) {
            String key = randomKey(player, getBroadcastKeyPrefix() + ".combo_transcend", 2);
            String msg = Component.translatable(key, playerName, combo).getString();
            broadcastRaw(player, msg);
            return;
        }
        if (combo >= 15) {
            String key = randomKey(player, getBroadcastKeyPrefix() + ".combo_myth", 3);
            String msg = Component.translatable(key, playerName, combo).getString();
            broadcastRaw(player, msg);
            return;
        }
        if (combo >= 10) {
            String key = randomKey(player, getBroadcastKeyPrefix() + ".combo_god", 3);
            String msg = Component.translatable(key, playerName, combo).getString();
            broadcastRaw(player, msg);
            return;
        }
        if (combo >= 7) {
            String key = randomKey(player, getBroadcastKeyPrefix() + ".combo_max", 2);
            String msg = Component.translatable(key, playerName, combo).getString();
            broadcastRaw(player, msg);
            return;
        }

        // 根据命中数量选择消息（随机变体）
        String msgKey;
        Object[] args;
        if (hitCount == 0) {
            msgKey = randomKey(player, getBroadcastKeyPrefix() + ".broadcast.zero", 5);
            args = new Object[]{playerName};
        } else if (hitCount == 1) {
            msgKey = randomKey(player, getBroadcastKeyPrefix() + ".broadcast.one", 4);
            args = new Object[]{playerName, firstTargetName};
        } else if (hitCount <= 3) {
            msgKey = randomKey(player, getBroadcastKeyPrefix() + ".broadcast.few", 3);
            args = new Object[]{playerName, hitCount};
        } else if (hitCount <= 6) {
            msgKey = randomKey(player, getBroadcastKeyPrefix() + ".broadcast.many", 3);
            args = new Object[]{playerName, hitCount};
        } else {
            msgKey = randomKey(player, getBroadcastKeyPrefix() + ".broadcast.massive", 2);
            args = new Object[]{playerName, hitCount};
        }

        String msg = Component.translatable(msgKey, args).getString();

        // 追加治疗信息
        if (healCount > 0) {
            String healMsg = Component.translatable(getBroadcastKeyPrefix() + ".broadcast.heal", healCount).getString();
            msg += healMsg;
        }

        broadcastRaw(player, msg);
    }

    /** 从翻译键的变体中随机选取一个 */
    private static String randomKey(Player player, String baseKey, int variants) {
        return baseKey + "." + player.getRandom().nextInt(variants);
    }

    private void broadcastRaw(Player player, String message) {
        // 向全服玩家广播
        player.level().players().forEach(p ->
            p.sendSystemMessage(Component.literal(message))
        );
    }

    // ========================
    //  BossBar 连击HUD（格斗游戏风格）
    // ========================

    /** 为玩家创建或更新连击 BossBar */
    private static void updateComboBossBar(ServerPlayer player, int combo, float multiplier,
                                            long totalWindowMs, long now, boolean evil) {
        long expiration = now + totalWindowMs;

        ActiveCombo existing = ACTIVE_COMBOS.get(player.getUUID());
        if (existing != null) {
            // 更新已有的 BossBar
            existing.expirationTime = expiration; // 重置过期时间
            existing.totalWindowMs = totalWindowMs;
            existing.comboCount = combo;
            existing.evil = evil;
            updateBossBarAppearance(existing);
            return;
        }

        // 新建 BossBar
        ServerBossEvent bar = new ServerBossEvent(
            Component.empty(),
            BossEvent.BossBarColor.BLUE,
            BossEvent.BossBarOverlay.PROGRESS
        );
        bar.setProgress(1.0f);
        bar.setVisible(true);
        bar.addPlayer(player);

        ActiveCombo active = new ActiveCombo(bar, expiration, totalWindowMs, combo, evil);
        updateBossBarAppearance(active);
        ACTIVE_COMBOS.put(player.getUUID(), active);
    }

    /** 设置 BossBar 的外观：颜色、标题 */
    private static void updateBossBarAppearance(ActiveCombo combo) {
        ServerBossEvent bar = combo.bossBar;
        int count = combo.comboCount;
        float secondsLeft = (combo.expirationTime - System.currentTimeMillis()) / 1000.0f;

        bar.setName(Component.literal(getBossBarTitle(count, secondsLeft, combo.evil)));
        bar.setColor(getBossBarColor(count, combo.evil));
    }

    /** BossBar 标题 */
    protected static String getBossBarTitle(int combo, float secondsLeft, boolean evil) {
        float mult = getComboMultiplier(combo);
        String prefix = evil ? "§4§l" : "";
        if (combo >= 25) {
            return String.format(prefix + "§5§k!§r§5§l✦ %s x%d §5§k!§r §7| §fx%.2f §8[%.1fs]",
                evil ? "邪神耄耋" : "超越耄耋", combo, mult, secondsLeft);
        } else if (combo >= 15) {
            return String.format(prefix + "§d§l✦ %s x%d §7| §fx%.2f §8[%.1fs]",
                evil ? "堕天哈气" : "哈气神话", combo, mult, secondsLeft);
        } else if (combo >= 10) {
            return String.format(prefix + "§5§l✦ %s x%d §7| §fx%.2f §8[%.1fs]",
                evil ? "罪恶耄耋" : "圆头耄耋", combo, mult, secondsLeft);
        } else if (combo >= 7) {
            return String.format(prefix + "§c✦ %s x%d §7| §fx%.2f §8[%.1fs]",
                evil ? "邪气暴走" : "哈气暴走", combo, mult, secondsLeft);
        } else if (combo >= 5) {
            return String.format(prefix + "§e✦ %s x%d §7| §fx%.2f §8[%.1fs]",
                evil ? "暗之猛攻" : "哈气猛攻", combo, mult, secondsLeft);
        } else if (combo >= 3) {
            return String.format(prefix + "§a✦ %s x%d §7| §fx%.2f §8[%.1fs]",
                evil ? "邪气连击" : "哈气连击", combo, mult, secondsLeft);
        } else {
            return String.format(prefix + "§b✦ %s x%d §7| §fx%.2f §8[%.1fs]",
                evil ? "暗之连击" : "连击", combo, mult, secondsLeft);
        }
    }

    /** BossBar 颜色 */
    protected static BossEvent.BossBarColor getBossBarColor(int combo, boolean evil) {
        if (evil && combo >= 10) return BossEvent.BossBarColor.RED;
        if (combo >= 25) return BossEvent.BossBarColor.PURPLE;
        if (combo >= 15) return BossEvent.BossBarColor.PINK;
        if (combo >= 10) return BossEvent.BossBarColor.PURPLE;
        if (combo >= 7) return BossEvent.BossBarColor.RED;
        if (combo >= 5) return BossEvent.BossBarColor.YELLOW;
        if (combo >= 3) return BossEvent.BossBarColor.GREEN;
        return BossEvent.BossBarColor.BLUE;
    }

    /** 获取指定连击数的对数倍率（用于显示） */
    private static float getComboMultiplier(int combo) {
        if (combo <= 0) return 1.0f;
        return 1.0f + (float) Math.log(combo + 1) * 0.25f;
    }

    /** 移除玩家的连击 BossBar */
    static void removeComboBossBar(UUID playerId) {
        ActiveCombo active = ACTIVE_COMBOS.remove(playerId);
        if (active != null) {
            active.bossBar.removeAllPlayers();
            active.bossBar.setVisible(false);
        }
    }

    /**
     * 每 tick 更新所有活跃连击 BossBar 的进度条。
     * 由 ToNekoEvents 中的 ServerTickEvents 调用。
     */
    public static void tickComboBossBars(MinecraftServer server) {
        long now = System.currentTimeMillis();
        var iterator = ACTIVE_COMBOS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ActiveCombo combo = entry.getValue();
            long remaining = combo.expirationTime - now;

            if (remaining <= 0) {
                // 连击超时，移除
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player != null) {
                    combo.bossBar.removePlayer(player);
                }
                combo.bossBar.removeAllPlayers();
                combo.bossBar.setVisible(false);
                iterator.remove();
            } else {
                // 更新进度条
                float progress = (float) remaining / combo.totalWindowMs;
                combo.bossBar.setProgress(Math.clamp(progress, 0.0f, 1.0f));

                // 每 10 tick 更新一次标题中的时间显示
                if (server.getTickCount() % 10 == 0) {
                    updateBossBarAppearance(combo);
                }
            }
        }
    }

    // ========================
    //  附魔接口
    // ========================
    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 15; // 中等附魔能力
    }

    @Override
    public boolean canBeEnchantedWith(ItemStack stack, Holder<Enchantment> enchantment, EnchantingContext context) {
        // 猫能爆哈器专属附魔
        if (enchantment.is(ToNekoEnchantments.HISS_POWER)
            || enchantment.is(ToNekoEnchantments.HISS_SPREAD)
            || enchantment.is(ToNekoEnchantments.HISS_EFFICIENCY)
            || enchantment.is(ToNekoEnchantments.COMBO_EXTEND)
            || enchantment.is(ToNekoEnchantments.HISS_ROOT)
            || enchantment.is(ToNekoEnchantments.HISS_DEMOLISH)) {
            return true;
        }
        // 原版剑类附魔
        if (enchantment.is(Enchantments.SHARPNESS)
            || enchantment.is(Enchantments.SMITE)
            || enchantment.is(Enchantments.BANE_OF_ARTHROPODS)
            || enchantment.is(Enchantments.FIRE_ASPECT)
            || enchantment.is(Enchantments.KNOCKBACK)) {
            return true;
        }
        // 通用附魔（耐久、经验修补等由 super 处理）
        return super.canBeEnchantedWith(stack, enchantment, context);
    }

    // ========================
    //  Tooltip
    // ========================
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                 @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable(getBroadcastKeyPrefix() + ".tip.flavor"));
        tooltipComponents.add(Component.translatable(getBroadcastKeyPrefix() + ".tip"));
        tooltipComponents.add(Component.translatable(getBroadcastKeyPrefix() + ".tip.damage", damage));
        tooltipComponents.add(Component.translatable(getBroadcastKeyPrefix() + ".tip.radius", radius));
        tooltipComponents.add(Component.translatable(getBroadcastKeyPrefix() + ".tip.energy_cost", energyCost));
        tooltipComponents.add(Component.translatable(getBroadcastKeyPrefix() + ".tip.combo_hint"));
    }
}
