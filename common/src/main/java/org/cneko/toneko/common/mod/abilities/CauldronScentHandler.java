package org.cneko.toneko.common.mod.abilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.cneko.toneko.common.mod.advencements.ToNekoCriteria;
import org.cneko.toneko.common.mod.items.SpoiledWaterBucketItem;
import org.cneko.toneko.common.mod.items.ToNekoItems;
import org.cneko.toneko.common.mod.misc.CauldronSpoilageData;
import org.cneko.toneko.common.mod.misc.LegwearUtil;
import org.cneko.toneko.common.mod.misc.Scentable;
import org.cneko.toneko.common.mod.misc.ScentUtil;
import org.cneko.toneko.common.mod.misc.ScentedWaterUtil;
import org.cneko.toneko.common.mod.misc.ToNekoComponents;
import org.cneko.toneko.common.mod.misc.WetnessUtil;
import org.cneko.toneko.common.util.ConfigUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 丝袜水缸：玩家穿着有气味的丝袜站在装满水的炼药锅里时，
 * 气味按当前浓度比例洗入水中（水质变质程度 0~100），同时丝袜气味被洗掉。
 * 用桶/玻璃瓶/火药/龙息/糖 可以分别舀出不同形态的变质水。
 */
public class CauldronScentHandler {
    private static final Map<UUID, Float> WASH_BUFFER = new ConcurrentHashMap<>();
    private static boolean cauldronInteractionsRegistered = false;

    public static void onServerTick(MinecraftServer server) {
        if (!ConfigUtil.isScentEnabled() || !ConfigUtil.isScentCauldronEnabled()) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isRemoved()) continue;

            ItemStack legwear = LegwearUtil.getWornLegwear(player);
            if (!Scentable.isScentable(legwear)) {
                WASH_BUFFER.remove(player.getUUID());
                continue;
            }

            int scent = ScentUtil.getIntensity(legwear);
            if (scent <= 0) {
                WASH_BUFFER.remove(player.getUUID());
                continue;
            }

            BlockPos pos = player.blockPosition();
            BlockState state = player.level().getBlockState(pos);
            if (!isWaterCauldron(state)) {
                WASH_BUFFER.remove(player.getUUID());
                continue;
            }

            float rate = ConfigUtil.getScentCauldronWashRate();
            if (rate <= 0f) {
                WASH_BUFFER.remove(player.getUUID());
                continue;
            }

            float delta = scent * rate;
            float buf = WASH_BUFFER.getOrDefault(player.getUUID(), 0f) + delta;
            int whole = (int) buf;
            if (whole == 0) {
                WASH_BUFFER.put(player.getUUID(), buf);
                continue;
            }

            // 洗掉的点数不会超过当前气味强度
            int washed = Math.min(whole, scent);
            WASH_BUFFER.put(player.getUUID(), buf - washed);

            boolean dirty = false;
            if (washed > 0) {
                legwear.set(ToNekoComponents.LEGWEAR_SCENT_COMPONENT,
                        ScentUtil.withClampedIntensity(legwear, scent - washed));
                dirty = true;
            }
            if (ConfigUtil.isWetnessEnabled() && WetnessUtil.get(legwear) < WetnessUtil.MAX_WETNESS) {
                legwear.set(ToNekoComponents.LEGWEAR_WET_COMPONENT, WetnessUtil.MAX_WETNESS);
                dirty = true;
            }
            if (dirty) {
                LegwearUtil.markLegwearDirty(player);
            }

            if (washed > 0) {
                ServerLevel serverLevel = player.serverLevel();
                CauldronSpoilageData data = CauldronSpoilageData.get(serverLevel);
                data.setSpoilage(pos, data.getSpoilage(pos) + washed, player.getName().getString());
            }
        }
    }

    /** 注册炼药锅交互：空桶/玻璃瓶/火药/龙息/糖 舀出变质水；普通水桶重新装满时清掉旧水质。 */
    public static void registerCauldronInteractions() {
        if (cauldronInteractionsRegistered) return;
        cauldronInteractionsRegistered = true;

        // 空桶从水缸舀水：有变质记录时给变质水桶，否则走原版
        CauldronInteraction vanillaScoop = CauldronInteraction.WATER.map().get(Items.BUCKET);
        if (vanillaScoop != null) {
            CauldronInteraction.WATER.map().put(Items.BUCKET, (state, level, pos, player, hand, stack) -> {
                if (level.isClientSide) {
                    return vanillaScoop.interact(state, level, pos, player, hand, stack);
                }
                if (isFullWaterCauldron(state) && level instanceof ServerLevel serverLevel) {
                    CauldronSpoilageData data = CauldronSpoilageData.get(serverLevel);
                    int spoilage = data.getSpoilage(pos);
                    if (spoilage > 0) {
                        String wearer = data.getWearer(pos);
                        data.clearSpoilage(pos);
                        ItemStack filled = SpoiledWaterBucketItem.create(spoilage, wearer);
                        notifyCollected(player);
                        return CauldronInteraction.fillBucket(state, level, pos, player, hand, stack,
                                filled,
                                s -> s.getValue(LayeredCauldronBlock.LEVEL) == LayeredCauldronBlock.MAX_FILL_LEVEL,
                                SoundEvents.BUCKET_FILL);
                    }
                }
                return vanillaScoop.interact(state, level, pos, player, hand, stack);
            });
        }

        // 玻璃瓶：舀出可饮用的变质水瓶
        CauldronInteraction vanillaBottle = CauldronInteraction.WATER.map().get(Items.GLASS_BOTTLE);
        if (vanillaBottle != null) {
            CauldronInteraction.WATER.map().put(Items.GLASS_BOTTLE, (state, level, pos, player, hand, stack) -> {
                if (level.isClientSide) {
                    return vanillaBottle.interact(state, level, pos, player, hand, stack);
                }
                if (isWaterCauldron(state) && level instanceof ServerLevel serverLevel) {
                    CauldronSpoilageData data = CauldronSpoilageData.get(serverLevel);
                    int spoilage = data.getSpoilage(pos);
                    if (spoilage > 0) {
                        ItemStack filled = ScentedWaterUtil.create(ToNekoItems.SPOILED_WATER_BOTTLE, spoilage, data.getWearer(pos));
                        notifyCollected(player);
                        return fillWithBottle(state, level, pos, player, hand, stack, filled);
                    }
                }
                return vanillaBottle.interact(state, level, pos, player, hand, stack);
            });
        }

        // 火药：把变质水调成喷溅型
        CauldronInteraction vanillaGunpowder = CauldronInteraction.WATER.map().get(Items.GUNPOWDER);
        if (vanillaGunpowder != null) {
            CauldronInteraction.WATER.map().put(Items.GUNPOWDER, (state, level, pos, player, hand, stack) -> {
                if (level.isClientSide) {
                    return vanillaGunpowder.interact(state, level, pos, player, hand, stack);
                }
                return mixCauldronWater(state, level, pos, player, hand, stack,
                        ToNekoItems.SPOILED_WATER_SPLASH);
            });
        }

        // 龙息：把变质水调成滞留型
        CauldronInteraction vanillaDragonBreath = CauldronInteraction.WATER.map().get(Items.DRAGON_BREATH);
        if (vanillaDragonBreath != null) {
            CauldronInteraction.WATER.map().put(Items.DRAGON_BREATH, (state, level, pos, player, hand, stack) -> {
                if (level.isClientSide) {
                    return vanillaDragonBreath.interact(state, level, pos, player, hand, stack);
                }
                return mixCauldronWater(state, level, pos, player, hand, stack,
                        ToNekoItems.SPOILED_WATER_LINGERING);
            });
        }

        // 糖：把变质水调成气味香水
        CauldronInteraction vanillaSugar = CauldronInteraction.WATER.map().get(Items.SUGAR);
        if (vanillaSugar != null) {
            CauldronInteraction.WATER.map().put(Items.SUGAR, (state, level, pos, player, hand, stack) -> {
                if (level.isClientSide) {
                    return vanillaSugar.interact(state, level, pos, player, hand, stack);
                }
                return mixCauldronWater(state, level, pos, player, hand, stack,
                        ToNekoItems.SCENT_PERFUME);
            });
        }

        // 普通水桶倒入空缸：清掉残留的旧水质记录
        CauldronInteraction vanillaFillWater = CauldronInteraction.EMPTY.map().get(Items.WATER_BUCKET);
        if (vanillaFillWater != null) {
            CauldronInteraction.EMPTY.map().put(Items.WATER_BUCKET, (state, level, pos, player, hand, stack) -> {
                if (level.isClientSide) {
                    return vanillaFillWater.interact(state, level, pos, player, hand, stack);
                }
                ItemInteractionResult result = vanillaFillWater.interact(state, level, pos, player, hand, stack);
                if (level instanceof ServerLevel serverLevel) {
                    CauldronSpoilageData.get(serverLevel).clearSpoilage(pos);
                }
                return result;
            });
        }
    }

    /** 用玻璃瓶舀变质水，降低一格水位并替换手中物品。 */
    private static ItemInteractionResult fillWithBottle(BlockState state, Level level, BlockPos pos,
                                                        net.minecraft.world.entity.player.Player player,
                                                        net.minecraft.world.InteractionHand hand,
                                                        ItemStack input, ItemStack filled) {
        if (!level.isClientSide) {
            LayeredCauldronBlock.lowerFillLevel(state, level, pos);
            player.setItemInHand(hand, ItemUtils.createFilledResult(input, player, filled));
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 用消耗品（火药/龙息/糖）调制炼药锅里的变质水。 */
    private static ItemInteractionResult mixCauldronWater(BlockState state, Level level, BlockPos pos,
                                                          net.minecraft.world.entity.player.Player player,
                                                          net.minecraft.world.InteractionHand hand,
                                                          ItemStack input, net.minecraft.world.level.ItemLike output) {
        if (level.isClientSide) return ItemInteractionResult.sidedSuccess(true);
        if (!isWaterCauldron(state) || !(level instanceof ServerLevel serverLevel)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        CauldronSpoilageData data = CauldronSpoilageData.get(serverLevel);
        int spoilage = data.getSpoilage(pos);
        if (spoilage <= 0) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        String wearer = data.getWearer(pos);
        ItemStack result = ScentedWaterUtil.create(output, spoilage, wearer);
        input.shrink(1);
        if (input.isEmpty()) {
            player.setItemInHand(hand, result);
        } else {
            player.setItemInHand(hand, input);
            if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }
        }
        LayeredCauldronBlock.lowerFillLevel(state, level, pos);
        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0f, 1.0f);
        notifyCollected(player);
        return ItemInteractionResult.sidedSuccess(false);
    }

    private static void notifyCollected(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer sp) {
            ToNekoCriteria.SPOILED_WATER_FIRST.trigger(sp);
            ToNekoCriteria.SPOILED_WATER_COLLECTOR.trigger(sp);
        }
    }

    private static boolean isWaterCauldron(BlockState state) {
        return state.is(Blocks.WATER_CAULDRON)
                && state.getValue(LayeredCauldronBlock.LEVEL) > 0;
    }

    private static boolean isFullWaterCauldron(BlockState state) {
        return state.is(Blocks.WATER_CAULDRON)
                && state.getValue(LayeredCauldronBlock.LEVEL) == LayeredCauldronBlock.MAX_FILL_LEVEL;
    }
}
