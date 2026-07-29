package org.cneko.toneko.common.mod.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static org.cneko.toneko.common.mod.util.EnchantmentUtil.getEnchantmentLevel;

public class NekoMultiToolItem extends Item {
    public static final String ID = "neko_multi_tool";

    // ---- 模式常量（偶数=正常速度，奇数=慢速×0.4） ----
    public static final int DIG_DEFAULT = 0;
    public static final int DIG_DEFAULT_SLOW = 1;
    public static final int DIG_SMELTING = 2;
    public static final int DIG_SMELTING_SLOW = 3;
    public static final int DIG_SILK_TOUCH = 4;
    public static final int DIG_SILK_TOUCH_SLOW = 5;
    public static final int DIG_MODE_COUNT = 6;

    /** 慢速模式时的速度除数 */
    public static final float SLOW_SPEED_DIVISOR = 2.5f;

    // ---- 速度/伤害上下限 ----
    public static final float SPEED_CAP = 36.0f;
    public static final float DAMAGE_CAP = 25.0f;
    public static final float NON_NEKO_SPEED = 4.0f;
    public static final float NON_NEKO_DAMAGE = 3.0f;
    public static final float BASE_SPEED = 2.0f;
    public static final float BASE_DAMAGE = 3.0f;
    public static final float SPEED_PER_LEVEL = 0.17f;
    public static final float DAMAGE_PER_LEVEL = 0.05f;

    // ---- 模式判断 ----
    public static boolean isSlowMode(int mode) { return mode % 2 == 1; }
    public static boolean isSmeltingMode(int mode) { return mode == DIG_SMELTING || mode == DIG_SMELTING_SLOW; }
    public static boolean isSilkTouchMode(int mode) { return mode == DIG_SILK_TOUCH || mode == DIG_SILK_TOUCH_SLOW; }

    public NekoMultiToolItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    // ========================
    //  CustomData 读写
    // ========================

    public static int getRangeMode(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.copyTag().contains("clawRange", CompoundTag.TAG_INT)) {
            return data.copyTag().getInt("clawRange");
        }
        return 1;
    }

    public static void setRangeMode(ItemStack stack, int range) {
        CompoundTag tag = getOrCreateTag(stack);
        tag.putInt("clawRange", range);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static int getDigMode(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.copyTag().contains("clawMode", CompoundTag.TAG_INT)) {
            return data.copyTag().getInt("clawMode");
        }
        return DIG_DEFAULT;
    }

    public static void setDigMode(ItemStack stack, int mode) {
        CompoundTag tag = getOrCreateTag(stack);
        tag.putInt("clawMode", mode);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static CompoundTag getOrCreateTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null ? data.copyTag() : new CompoundTag();
    }

    /** 循环范围模式：1→3→5(Lv15)→1 */
    public static void cycleRangeMode(ItemStack stack, Player player) {
        int current = getRangeMode(stack);
        int next;
        if (current == 1) next = 3;
        else if (current == 3) next = (player.isNeko() && player.getNekoLevel() >= 15) ? 5 : 1;
        else next = 1;
        setRangeMode(stack, next);
    }

    /** 循环挖掘模式：默认→默认慢速→熔炼→熔炼慢速→精准→精准慢速→默认 */
    public static void cycleDigMode(ItemStack stack) {
        int current = getDigMode(stack);
        int next = (current + 1) % DIG_MODE_COUNT;
        setDigMode(stack, next);
    }

    // ========================
    //  速度 / 伤害公式
    // ========================

    public static float getMiningSpeed(Player player) {
        if (!player.isNeko()) return NON_NEKO_SPEED;
        float speed = BASE_SPEED + player.getNekoLevel() * SPEED_PER_LEVEL;
        return Math.min(speed, SPEED_CAP);
    }

    public static float getAttackDamage(Player player) {
        if (!player.isNeko()) return NON_NEKO_DAMAGE;
        float damage = BASE_DAMAGE + player.getNekoLevel() * DAMAGE_PER_LEVEL;
        return Math.min(damage, DAMAGE_CAP);
    }

    /** 每方块基础能量消耗 */
    public static float getEnergyCostPerBlock(Player player, ItemStack stack) {
        if (!player.isNeko()) return 0;
        int unbreaking = getEnchantmentLevel(Enchantments.UNBREAKING, stack, player.level());
        float base = 0.5f;
        return Math.max(0.1f, base * (1.0f - unbreaking * 0.10f));
    }

    // ========================
    //  工具覆写
    // ========================

    @Override
    public float getDestroySpeed(@NotNull ItemStack stack, BlockState state) {
        float cachedLevel = getCachedNekoLevel(stack);
        float baseSpeed;
        if (cachedLevel > 0) {
            baseSpeed = BASE_SPEED + cachedLevel * SPEED_PER_LEVEL;
            baseSpeed = Math.min(baseSpeed, SPEED_CAP);
        } else {
            baseSpeed = NON_NEKO_SPEED;
        }
        int mode = getDigMode(stack);
        return isSlowMode(mode) ? baseSpeed / SLOW_SPEED_DIVISOR : baseSpeed;
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity,
                               int slotId, boolean isSelected) {
        if (!level.isClientSide && entity instanceof Player player && isSelected) {
            // 每 20 tick (~1秒) 同步猫娘等级到物品 NBT，供 getDestroySpeed 使用
            if (level.getGameTime() % 20 == 0) {
                CompoundTag tag = getOrCreateTag(stack);
                tag.putBoolean("isNeko", player.isNeko());
                tag.putFloat("nekoLevel", player.isNeko() ? player.getNekoLevel() : 0);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
        }
    }

    /** 从物品 NBT 读取缓存的猫娘等级 */
    private static float getCachedNekoLevel(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.copyTag().contains("isNeko")) {
            CompoundTag tag = data.copyTag();
            if (tag.getBoolean("isNeko")) {
                return tag.getFloat("nekoLevel");
            }
        }
        return 0;
    }

    @Override
    public boolean isCorrectToolForDrops(@NotNull ItemStack stack, BlockState state) {
        // 万能工具：所有可挖掘方块都算作正确工具
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                || state.is(BlockTags.MINEABLE_WITH_AXE)
                || state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                || state.is(BlockTags.MINEABLE_WITH_HOE)
                || state.is(BlockTags.SWORD_EFFICIENT);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand usedHand) {
        if (player.isShiftKeyDown()) {
            // Shift+右键：循环挖掘模式
            ItemStack stack = player.getItemInHand(usedHand);
            if (!level.isClientSide) {
                cycleDigMode(stack);
                int mode = getDigMode(stack);
                player.displayClientMessage(Component.translatable(getModeLangKey(mode)), true);
            }
            return InteractionResultHolder.success(player.getItemInHand(usedHand));
        }
        return super.use(level, player, usedHand);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        // Shift+右键交给 use() 处理模式切换
        if (player.isShiftKeyDown()) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos center = context.getClickedPos();
        Direction face = context.getClickedFace();

        // 先检查中心方块是否可锄
        if (getTilledState(level.getBlockState(center)) == null) return InteractionResult.PASS;

        // 获取范围
        ItemStack stack = context.getItemInHand();
        int range = getRangeMode(stack);
        if (range == 5 && (!player.isNeko() || player.getNekoLevel() < 15)) range = 3;

        // 收集范围内可锄方块
        List<BlockPos> targets = new ArrayList<>();
        targets.add(center);
        if (range > 1) {
            int half = range / 2;
            Direction a1, a2;
            if (face.getAxis() == Direction.Axis.Y) {
                a1 = Direction.NORTH; a2 = Direction.EAST;
            } else {
                a1 = Direction.UP; a2 = face.getClockWise();
            }
            for (int x = -half; x <= half; x++) {
                for (int z = -half; z <= half; z++) {
                    if (x == 0 && z == 0) continue;
                    BlockPos p = center.relative(a1, x).relative(a2, z);
                    if (getTilledState(level.getBlockState(p)) != null) {
                        targets.add(p);
                    }
                }
            }
        }

        if (level.isClientSide) {
            // 客户端只播声音
            level.playSound(player, center, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.sidedSuccess(true);
        }

        // 服务端：计算能量
        float totalCost = 0;
        for (BlockPos ignored : targets) {
            totalCost += getEnergyCostPerBlock(player, stack);
        }
        if (player.isNeko() && player.getNekoEnergy() < totalCost) {
            player.displayClientMessage(Component.translatable("item.toneko.neko_multi_tool.no_energy"), true);
            return InteractionResult.FAIL;
        }

        // 执行锄地
        for (BlockPos p : targets) {
            BlockState tilled = getTilledState(level.getBlockState(p));
            if (tilled != null) {
                level.setBlock(p, tilled, 11);
                level.levelEvent(2001, p, net.minecraft.world.level.block.Block.getId(tilled));
            }
        }
        level.playSound(null, center, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);

        // 扣除能量
        if (player.isNeko()) {
            player.setNekoEnergy(player.getNekoEnergy() - totalCost);
        }

        return InteractionResult.sidedSuccess(false);
    }

    /** 锄地映射：输入方块 → 耕地 */
    private static BlockState getTilledState(BlockState state) {
        if (state.is(Blocks.GRASS_BLOCK)) return Blocks.FARMLAND.defaultBlockState();
        if (state.is(Blocks.DIRT_PATH)) return Blocks.FARMLAND.defaultBlockState();
        if (state.is(Blocks.DIRT)) return Blocks.FARMLAND.defaultBlockState();
        if (state.is(Blocks.COARSE_DIRT)) return Blocks.DIRT.defaultBlockState();
        if (state.is(Blocks.ROOTED_DIRT)) return Blocks.DIRT.defaultBlockState();
        return null;
    }

    // ========================
    //  附魔
    // ========================

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public boolean canBeEnchantedWith(ItemStack stack, Holder<Enchantment> enchantment, net.fabricmc.fabric.api.item.v1.EnchantingContext context) {
        // 工具附魔
        if (enchantment.is(Enchantments.EFFICIENCY)
                || enchantment.is(Enchantments.UNBREAKING) // 改造为减能耗
                || enchantment.is(Enchantments.FORTUNE)) {
            return true;
        }
        // 武器附魔
        if (enchantment.is(Enchantments.SHARPNESS)
                || enchantment.is(Enchantments.SMITE)
                || enchantment.is(Enchantments.BANE_OF_ARTHROPODS)
                || enchantment.is(Enchantments.KNOCKBACK)
                || enchantment.is(Enchantments.SWEEPING_EDGE)
                || enchantment.is(Enchantments.LOOTING)) {
            return true;
        }
        return super.canBeEnchantedWith(stack, enchantment, context);
    }

    // ========================
    //  攻击处理
    // ========================

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        if (attacker instanceof Player player && player.isNeko()) {
            float energyCost = getEnergyCostPerBlock(player, stack);
            if (player.getNekoEnergy() >= energyCost) {
                player.setNekoEnergy(player.getNekoEnergy() - energyCost);
            }
        }
        // 工具不会因攻击损失耐久（无耐久条）
        return true;
    }

    @Override
    public void postHurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        // 火焰附加处理由原版逻辑通过 enchantment 自动完成
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
    }

    // ========================
    //  Tooltip
    // ========================

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        int range = getRangeMode(stack);
        int digMode = getDigMode(stack);

        tooltipComponents.add(Component.translatable("item.toneko.neko_multi_tool.tip"));
        tooltipComponents.add(Component.translatable("item.toneko.neko_multi_tool.mode.range." + range));
        tooltipComponents.add(Component.translatable(getModeLangKey(digMode)));
        tooltipComponents.add(Component.translatable("item.toneko.neko_multi_tool.tip.modes"));
        tooltipComponents.add(Component.translatable("item.toneko.neko_multi_tool.tip.range_locked"));
    }

    /** 根据模式编号返回对应的翻译键 */
    public static String getModeLangKey(int mode) {
        return switch (mode) {
            case DIG_DEFAULT_SLOW -> "item.toneko.neko_multi_tool.mode.default_slow";
            case DIG_SMELTING -> "item.toneko.neko_multi_tool.mode.smelting";
            case DIG_SMELTING_SLOW -> "item.toneko.neko_multi_tool.mode.smelting_slow";
            case DIG_SILK_TOUCH -> "item.toneko.neko_multi_tool.mode.silk_touch";
            case DIG_SILK_TOUCH_SLOW -> "item.toneko.neko_multi_tool.mode.silk_touch_slow";
            default -> "item.toneko.neko_multi_tool.mode.default";
        };
    }
}
