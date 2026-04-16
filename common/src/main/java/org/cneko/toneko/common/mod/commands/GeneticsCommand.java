package org.cneko.toneko.common.mod.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.cneko.toneko.common.mod.genetics.api.IGeneticEntity;
import org.cneko.toneko.common.mod.packets.GenomeDataPayload;

public class GeneticsCommand {
    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("genetics")
                    .then(Commands.argument("target", EntityArgument.entity())
                                    .executes(context -> {
                                        Entity target = EntityArgument.getEntity(context, "target");
                                        ServerPlayer player = context.getSource().getPlayerOrException();

                                        if (target instanceof IGeneticEntity geneticEntity) {
                                            // 发送数据给客户端
                                            ServerPlayNetworking.send(player, new GenomeDataPayload(
                                                    target.getId(),
                                                    geneticEntity.getGenome().save()
                                            ));
                                            return 1;
                                        } else {
                                            context.getSource().sendFailure(net.minecraft.network.chat.Component.literal("该实体没有基因组！"));
                                            return 0;
                                        }
                                    })
                            )

            );
        });

    }
}