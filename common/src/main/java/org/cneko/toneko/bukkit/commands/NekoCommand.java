package org.cneko.toneko.bukkit.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.cneko.toneko.common.api.Permissions;
import org.jetbrains.annotations.NotNull;
import org.bukkit.craftbukkit.entity.CraftPlayer;

import java.util.List;

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
                            .then(Commands.literal("jump")
                                    .requires(s -> checkAndNeko(s, Permissions.COMMAND_NEKO_JUMP))
                                    .executes(NekoCommand::jumpCommand)
                            )
                            .then(Commands.literal("vision")
                                    .requires(s -> checkAndNeko(s, Permissions.COMMAND_NEKO_VISION))
                                    .executes(NekoCommand::visionCommand)
                            )
                            .then(Commands.literal("speed")
                                    .requires(s -> checkAndNeko(s, Permissions.COMMAND_NEKO_SPEED))
                                    .executes(NekoCommand::speedCommand)
                            )
                            .then(Commands.literal("level")
                                    .requires(s -> checkAndNeko(s, Permissions.COMMAND_NEKO_LEVEL))
                                    .executes(NekoCommand::levelCommand)
                            )
                            .then(Commands.literal("nickname")
                                    .requires(s -> checkAndNeko(s, Permissions.COMMAND_NEKO_NICKNAME))
                                    .then(Commands.argument("nickname", StringArgumentType.greedyString())
                                            .executes(NekoCommand::nicknameCommand)
                                    )
                            )
                            .then(Commands.literal("lore")
                                    .requires(s -> checkAndNeko(s, Permissions.COMMAND_NEKO_LORE))
                                    .then(Commands.argument("lore", StringArgumentType.greedyString())
                                            .executes(NekoCommand::loreCommand)
                                    )
                            )
                            .build()
            );
        });
    }

    public static int loreCommand(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        // 获取玩家手中的物品
        ItemStack stack = player.getInventory().getItemInMainHand();
        if (stack.isEmpty()) {
            sendTransTo(player,"command.neko.lore.no_item");
            return 1;
        }
        String lore = StringArgumentType.getString(context, "lore");
        stack.lore(List.of(Component.text(lore)));
        return 1;
    }

    public static int nicknameCommand(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        NekoQuery.Neko neko = NekoQuery.getNeko(player.getUniqueId());
        String nickname = StringArgumentType.getString(context, "nickname");
        // 设置昵称
        neko.setNickName(nickname);
        sendTransTo(player,"command.neko.nickname.success", nickname);
        return 1;
    }

    public static int levelCommand(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        NekoQuery.Neko neko = NekoQuery.getNeko(player.getUniqueId());
        if (neko.isNeko()){
            sendTransTo(player, "command.neko.level.success", neko.getLevel());
        }else{
            sendTransTo(player, "command.neko.not_neko");
        }
        return 1;
    }

    public static int speedCommand(CommandContext<CommandSourceStack> context) {
        return giveEffect(context, PotionEffectType.SPEED);
    }

    public static int visionCommand(CommandContext<CommandSourceStack> context) {
        return giveEffect(context, PotionEffectType.NIGHT_VISION);
    }

    public static int jumpCommand(CommandContext<CommandSourceStack> context) {
        return giveEffect(context, PotionEffectType.JUMP_BOOST);
    }

    private static int giveEffect(CommandContext<CommandSourceStack> context, PotionEffectType effect){
        Player player = (Player) context.getSource().getSender();

        double level = NekoQuery.getLevel(player.getUniqueId());
        // 等级开方/2
        int effectLevel = (int) (Math.sqrt(level)/2.00);
        // 对((((等级+1)的开方)乘以(玩家的经验值的开方))/(玩家生命值/4))*20来计算效果时间
        int time = (int) (((((Math.sqrt(level+1)) * (Math.sqrt(player.getExpCooldown()+1))) / (player.getHealth()/4)))*100);
        player.addPotionEffect(new PotionEffect(effect,time,effectLevel));
        return 1;
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
