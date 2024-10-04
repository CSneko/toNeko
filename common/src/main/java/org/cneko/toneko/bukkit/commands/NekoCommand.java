package org.cneko.toneko.bukkit.commands;

import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.cneko.toneko.common.api.Permissions;
import org.jetbrains.annotations.NotNull;
import org.bukkit.craftbukkit.entity.CraftPlayer;

import static org.cneko.toneko.bukkit.ToNeko.INSTANCE;
import static org.cneko.toneko.bukkit.util.MsgUtil.sendTransTo;
import static org.cneko.toneko.bukkit.util.PermissionChecker.check;
import static org.cneko.toneko.bukkit.util.PermissionChecker.checkAndNeko;
@SuppressWarnings("UnstableApiUsage")
public class NekoCommand {
    public static void init() {
        LifecycleEventManager<@NotNull Plugin> manager = INSTANCE.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register(
                    Commands.literal("neko")
                            .requires(s -> checkAndNeko(s, Permissions.COMMAND_NEKO_HELP))
                            .executes(NekoCommand::helpCommand)
                            .then(Commands.literal("help")
                                    .requires(s -> checkAndNeko(s, Permissions.COMMAND_NEKO_HELP))
                                    .executes(NekoCommand::helpCommand)
                            )
                            .then(Commands.literal("ride")
                                    .requires(s -> checkAndNeko(s, Permissions.COMMAND_NEKO_RIDE))
                                    .executes(NekoCommand::rideCommand)
                            )
                            .build()
            );
        });
    }

    public static int rideCommand(CommandContext<CommandSourceStack> context) {
        ServerPlayer entity = ((CraftPlayer)context.getSource().getSender()).getHandle();
        ServerLevel world = (ServerLevel) entity.level();
        // 获取玩家3格方块内的实体
        float radius = 3.0f;
        LivingEntity target = EntityUtil.findNearestEntityInRange(entity, world, radius);

        if (target != null && target != entity){
            entity.startRiding(target,true);
        }

        if (target instanceof ServerPlayer sp){
            sp.connection.send(new ClientboundSetPassengersPacket(target));
        }
        return 1;
    }

    public static int helpCommand(CommandContext<CommandSourceStack> context) {
        sendTransTo((Player) context.getSource().getSender(), "command.neko.help");
        return 1;
    }
}
