package org.cneko.toneko.common.mod.items;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.genetics.api.IGeneticEntity;
import org.cneko.toneko.common.mod.packets.GenomeDataPayload;
import org.jetbrains.annotations.NotNull;

public class GeneEditorItem extends Item {
    public GeneEditorItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
        if (!player.level().isClientSide() && interactionTarget instanceof IGeneticEntity geneticEntity) {
            // 发送给客户端，标记 canEdit = true
            ServerPlayNetworking.send((ServerPlayer) player, new GenomeDataPayload(
                    interactionTarget.getId(),
                    geneticEntity.getGenome().save(),
                    true
            ));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}