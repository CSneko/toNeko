package org.cneko.toneko.common.mod.items;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.packets.OpenPlotScreenPayload;
import org.jetbrains.annotations.NotNull;

public class PlotScrollItem extends Item {
    public PlotScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand usedHand) {
        if (player instanceof ServerPlayer sp){
            ServerPlayNetworking.send(sp, new OpenPlotScreenPayload());
        }
        return super.use(level, player, usedHand);
    }
}
