package org.cneko.toneko.bukkit.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.cneko.toneko.bukkit.util.PayloadSender;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.api.Permissions;
import org.cneko.toneko.common.util.AIUtil;
import org.cneko.toneko.common.util.ConfigUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.cneko.toneko.bukkit.ToNeko.INSTANCE;
import static org.cneko.toneko.bukkit.util.MsgUtil.sendTransTo;
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
                            .then(Commands.literal("chat")
                                    .requires(s -> checkAndNeko(s, Permissions.COMMAND_NEKO_CHAT))
                                    .then(Commands.argument("message", StringArgumentType.greedyString())
                                            .executes(NekoCommand::chatCommand)
                                    )
                            )
                            .then(Commands.literal("gui")
                                    .requires(s -> checkAndNeko(s, Permissions.COMMAND_NEKO_GUI))
                                    .executes(NekoCommand::guiCommand)
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
                            .then(Commands.literal("removeNickname")
                                    .requires(s -> checkAndNeko(s, Permissions.COMMAND_NEKO_NICKNAME))
                                    .executes(NekoCommand::removeNicknameCommand)
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

    public static int removeNicknameCommand(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        NekoQuery.Neko neko = NekoQuery.getNeko(player.getUniqueId());
        neko.setNickName("");
        sendTransTo(player, "command.neko.nickname.success", "");
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
        NekoQuery.Neko neko = NekoQuery.getNeko(player.getUniqueId());

        // Consume 100 energy (matches mod behavior)
        float energy = neko.getNekoEnergy();
        if (energy < 100) {
            sendTransTo(player, "command.neko.effect.not_enough_energy");
            return 1;
        }
        neko.setNekoEnergy(energy - 100);

        double level = neko.getLevel();
        int effectLevel = (int) (Math.sqrt(level)/2.00);
        int time = (int) (((((Math.sqrt(level+1)) * (Math.sqrt(player.getExpCooldown()+1))) / (player.getHealth()/4)))*100);
        effectLevel = Math.min(effectLevel, 10);
        time = Math.min(time, 20 * 3600);
        player.addPotionEffect(new PotionEffect(effect, time, effectLevel));
        return 1;
    }

    public static int guiCommand(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        if (org.cneko.toneko.bukkit.api.ClientStatus.isInstalled(player)) {
            PayloadSender.sendOpenNekoInfoScreen(player);
        } else {
            sendTransTo(player, "messages.toneko.mod_required");
        }
        return 1;
    }

    public static int rideCommand(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        // Use Bukkit API: find nearest entity within 3 blocks
        Collection<Entity> nearby = player.getNearbyEntities(3, 3, 3);
        Entity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (Entity e : nearby) {
            double d = e.getLocation().distanceSquared(player.getLocation());
            if (d < closestDist && e != player) {
                closestDist = d;
                closest = e;
            }
        }
        if (closest != null) {
            closest.addPassenger(player);
        }
        return 1;
    }

    public static int chatCommand(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        if (!ConfigUtil.isAIEnabled()) {
            sendTransTo(player, "messages.toneko.ai.not_enabled");
            return 1;
        }
        String message = StringArgumentType.getString(context, "message");
        if (message.isBlank()) return 1;

        // Find nearest neko player
        UUID nearestNeko = null;
        double nearest = 256;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == player) continue;
            NekoQuery.Neko n = NekoQuery.getNeko(p.getUniqueId());
            if (n != null && n.isNeko() && p.getWorld() == player.getWorld()) {
                double d = p.getLocation().distanceSquared(player.getLocation());
                if (d < nearest) { nearest = d; nearestNeko = p.getUniqueId(); }
            }
        }
        if (nearestNeko == null) {
            sendTransTo(player, "messages.toneko.chat.no_neko_nearby");
            return 1;
        }
        UUID finalNeko = nearestNeko;
        Player nekoPlayer = Bukkit.getPlayer(finalNeko);
        String prompt = ConfigUtil.getAIPrompt()
                .replace("%neko_name%", nekoPlayer != null ? nekoPlayer.getName() : "???")
                .replace("%player_name%", player.getName());
        AIUtil.sendMessage(finalNeko, player.getUniqueId(), prompt, message, response -> {
            Bukkit.getScheduler().runTask(INSTANCE, () -> {
                String reply = response.getResponse();
                if (reply != null && !reply.isEmpty()) {
                    String prefix = (nekoPlayer != null ? nekoPlayer.getName() : "???") + " §d>> §f";
                    player.sendMessage(Component.text(prefix + reply));
                    if (org.cneko.toneko.bukkit.api.ClientStatus.isInstalled(player)) {
                        PayloadSender.sendChatHistory(player, finalNeko.toString(),
                                List.of("user:" + message, "assistant:" + reply));
                        if (ConfigUtil.isAITTSEnabled()) PayloadSender.sendTTS(player, reply);
                    }
                }
            });
        });
        return 1;
    }

    public static int helpCommand(CommandContext<CommandSourceStack> context) {
        sendTransTo((Player) context.getSource().getSender(), "command.neko.help");
        return 1;
    }
}
