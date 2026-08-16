package org.cneko.toneko.common.mod.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.cneko.toneko.common.mod.advencements.ToNekoCriteria;
import org.cneko.toneko.common.mod.misc.FireSourceUtil;
import org.cneko.toneko.common.mod.misc.ScentUtil;
import org.cneko.toneko.common.mod.misc.Scentable;
import org.cneko.toneko.common.mod.misc.ToNekoComponents;
import org.cneko.toneko.common.mod.misc.WetnessUtil;
import org.cneko.toneko.common.util.ConfigUtil;

/**
 * 晾衣架方块实体：单格存储 + 服务端每秒结算（自然晾干/下雨反湿/火源快干增味/干燥散味/滴水）。
 */
public class ClotheslineBlockEntity extends BlockEntity {
    private static final int DRIP_MIN_WETNESS = 25;
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    public ClotheslineBlockEntity(BlockPos pos, BlockState state) {
        super(ToNekoBlockEntities.CLOTHESLINE, pos, state);
    }

    public ItemStack getItem() {
        return items.get(0);
    }

    public void setItem(ItemStack stack) {
        items.set(0, stack);
        setChanged();
    }

    public void clearItem() {
        items.set(0, ItemStack.EMPTY);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    // 同步给客户端：默认 getUpdatePacket 返回 null，必须覆写否则客户端渲染器拿不到数据
    @org.jetbrains.annotations.Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ClotheslineBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!ConfigUtil.isClotheslineEnabled()) return;
        if (serverLevel.getGameTime() % 20 != 0) return;
        be.tick(serverLevel, pos);
    }

    private void tick(ServerLevel level, BlockPos pos) {
        ItemStack stack = items.get(0);
        if (stack.isEmpty() || !Scentable.isScentable(stack)) return;

        float heat = FireSourceUtil.heat(level, pos, ConfigUtil.getClotheslineFireRadius());
        boolean raining = level.isRainingAt(pos) && level.canSeeSky(pos);

        int wetness = WetnessUtil.get(stack);
        if (raining) {
            int next = Math.min(WetnessUtil.MAX_WETNESS, wetness + (int) Math.ceil(ConfigUtil.getWetnessRainRate() * 20));
            if (next != wetness) {
                stack.set(ToNekoComponents.LEGWEAR_WET_COMPONENT, next);
                setChanged();
                wetness = next;
            }
        } else if (wetness > 0) {
            float perSecond = heat > 1.0f
                    ? ConfigUtil.getClotheslineFireDryPerSecond()
                    : ConfigUtil.getClotheslineDryPerSecond();
            int next = Math.max(0, wetness - (int) Math.ceil(perSecond));
            if (next != wetness) {
                stack.set(ToNekoComponents.LEGWEAR_WET_COMPONENT, next);
                setChanged();
                wetness = next;
                if (next == 0) fireDried(level, pos);
            }
        }

        int scent = ScentUtil.getIntensity(stack);
        if (heat > 1.0f) {
            float ratio = wetness / 100.0f + scent / 200.0f;
            int delta = (int) Math.ceil(ConfigUtil.getClotheslineFireScentPerSecond() * ratio);
            if (delta > 0) {
                int next = Math.min(ScentUtil.MAX_INTENSITY, scent + delta);
                if (next != scent) {
                    stack.set(ToNekoComponents.LEGWEAR_SCENT_COMPONENT, ScentUtil.withClampedIntensity(stack, next));
                    setChanged();
                }
            }
        } else if (wetness == 0 && scent > 0) {
            int next = Math.max(0, scent - (int) Math.ceil(ConfigUtil.getClotheslineAirScentDecayPerSecond()));
            stack.set(ToNekoComponents.LEGWEAR_SCENT_COMPONENT, ScentUtil.withClampedIntensity(stack, next));
            setChanged();
        }

        if (WetnessUtil.get(stack) >= DRIP_MIN_WETNESS) {
            level.sendParticles(ParticleTypes.FALLING_WATER,
                    pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5,
                    WetnessUtil.get(stack) >= 80 ? 2 : 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private void fireDried(ServerLevel level, BlockPos pos) {
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            if (p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) <= 256) {
                ToNekoCriteria.LEGWEAR_DRIED.trigger(p);
            }
        }
    }
}
