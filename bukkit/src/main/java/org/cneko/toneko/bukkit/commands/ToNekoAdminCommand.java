package org.cneko.toneko.bukkit.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.kyori.adventure.text.Component;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.cneko.toneko.bukkit.api.NekoStatus;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.api.Permissions;
import org.cneko.toneko.common.mod.ai.provider.AIServiceProvider;
import org.cneko.toneko.common.mod.ai.provider.AIServiceProviderRegistry;
import org.cneko.toneko.common.util.AIUtil;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static org.cneko.toneko.bukkit.ToNeko.INSTANCE;
import static org.cneko.toneko.bukkit.util.MsgUtil.sendTransTo;
import static org.cneko.toneko.bukkit.util.PermissionChecker.check;

@SuppressWarnings("UnstableApiUsage")
public class ToNekoAdminCommand {
    public static void init() {
        LifecycleEventManager<@NotNull Plugin> manager = INSTANCE.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register(
                    Commands.literal("tonekoadmin")
                            .requires(s -> check(s, Permissions.COMMAND_TONEKOADMIN))
                            .then(Commands.literal("help")
                                    .requires(s -> check(s, Permissions.COMMAND_TONEKOADMIN_HELP))
                                    .executes(ToNekoAdminCommand::helpCommand)
                            )
                            .executes(ToNekoAdminCommand::helpCommand)
                            .then(Commands.literal("set")
                                    .requires(s -> check(s, Permissions.COMMAND_TONEKOADMIN_SET))
                                    .then(Commands.argument("player", ArgumentTypes.player())
                                            .executes(ToNekoAdminCommand::setCommand)
                                    )
                            )
                            .then(Commands.literal("config")
                                    .requires(s -> check(s, Permissions.COMMAND_TONEKOADMIN_CONFIG))
                                    .then(Commands.literal("get")
                                            .then(Commands.argument("key", StringArgumentType.word())
                                                    .executes(ToNekoAdminCommand::configGet)
                                            )
                                    )
                                    .then(Commands.literal("set")
                                            .then(Commands.argument("key", StringArgumentType.word())
                                                    .then(Commands.argument("value", StringArgumentType.greedyString())
                                                            .executes(ToNekoAdminCommand::configSet)
                                                    )
                                            )
                                    )
                            )
                            .then(Commands.literal("reload")
                                    .requires(s -> check(s, Permissions.COMMAND_TONEKOADMIN_RELOAD))
                                    .executes(ToNekoAdminCommand::reloadCommand)
                                    .then(Commands.literal("data")
                                            .executes(ToNekoAdminCommand::reloadDataCommand)
                                    )
                                    .then(Commands.literal("config")
                                            .executes(ToNekoAdminCommand::reloadConfigCommand)
                                    )
                            )
                            .then(Commands.literal("data")
                                    .requires(s -> check(s, Permissions.COMMAND_TONEKOADMIN_DATA))
                                    .then(Commands.literal("loadedCount")
                                            .executes(ToNekoAdminCommand::dataLoadedCount)
                                    )
                                    .then(Commands.literal("allCount")
                                            .executes(ToNekoAdminCommand::dataAllCount)
                                    )
                            )
                            .then(Commands.literal("neko")
                                    .requires(s -> check(s, Permissions.COMMAND_TONEKOADMIN_NEKO))
                                    .then(Commands.argument("player", ArgumentTypes.player())
                                            .executes(ToNekoAdminCommand::nekoInfoCommand)
                                            .then(Commands.literal("setBaby")
                                                    .then(Commands.argument("value", StringArgumentType.word())
                                                            .executes(ToNekoAdminCommand::nekoSetBaby)
                                                    )
                                            )
                                            .then(Commands.literal("setEnergy")
                                                    .then(Commands.argument("value", StringArgumentType.word())
                                                            .executes(ToNekoAdminCommand::nekoSetEnergy)
                                                    )
                                            )
                                            .then(Commands.literal("setNickName")
                                                    .then(Commands.argument("value", StringArgumentType.greedyString())
                                                            .executes(ToNekoAdminCommand::nekoSetNickName)
                                                    )
                                            )
                                            .then(Commands.literal("setAge")
                                                    .then(Commands.argument("value", StringArgumentType.word())
                                                            .executes(ToNekoAdminCommand::nekoSetAge)
                                                    )
                                            )
                                    )
                            )
                            .then(Commands.literal("ai")
                                    .requires(s -> check(s, Permissions.COMMAND_TONEKOADMIN))
                                    .then(Commands.literal("list")
                                            .executes(ToNekoAdminCommand::aiList)
                                    )
                                    .then(Commands.literal("switch")
                                            .then(Commands.argument("provider", StringArgumentType.word())
                                                    .executes(ToNekoAdminCommand::aiSwitch)
                                            )
                                    )
                                    .then(Commands.literal("config")
                                            .then(Commands.argument("provider", StringArgumentType.word())
                                                    .then(Commands.argument("key", StringArgumentType.word())
                                                            .then(Commands.argument("value", StringArgumentType.greedyString())
                                                                    .executes(ToNekoAdminCommand::aiConfig)
                                                            )
                                                    )
                                            )
                                    )
                                    .then(Commands.literal("test")
                                            .then(Commands.argument("message", StringArgumentType.greedyString())
                                                    .executes(ToNekoAdminCommand::aiTest)
                                            )
                                    )
                            )
                            .build()
            );
        });
    }

    private static int dataAllCount(CommandContext<CommandSourceStack> context) {
        NekoQuery.NekoData.asyncGetAllNekoCount(count -> sendTransTo((Player) context.getSource().getSender(), "command.tonekoadmin.data.all_count", count));
        return 1;
    }

    private static int dataLoadedCount(CommandContext<CommandSourceStack> context) {
        sendTransTo((Player) context.getSource().getSender(), "command.tonekoadmin.data.loaded_count", NekoQuery.NekoData.getNekoCount());
        return 1;
    }

    public static int reloadConfigCommand(CommandContext<CommandSourceStack> context) {
        // 重新加载配置文件和语言文件
        ConfigUtil.load();
        LanguageUtil.load();
        sendTransTo((Player) context.getSource().getSender(), "command.tonekoadmin.reload");
        return 1;
    }

    public static int reloadDataCommand(CommandContext<CommandSourceStack> context) {
        long startTime = System.currentTimeMillis(); // 记录开始时间

        NekoQuery.NekoData.saveAllAsync(() -> {
            long elapsedTime = (System.currentTimeMillis() - startTime) / 1000; // 计算耗时（秒）
            NekoQuery.NekoData.removeAll();
            sendTransTo((Player) context.getSource().getSender(),
                    "command.tonekoadmin.reload.data",
                    elapsedTime); // 将耗时传递给方法
        });
        return 1;
    }

    public static int configGet(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        String key = context.getArgument("key", String.class);
        try {
            Object value = ConfigUtil.CONFIG.get(key);
            if (value == null) {
                sendTransTo(player, "command.tonekoadmin.config.not_found", key);
            } else {
                player.sendMessage(Component.text("§e" + key + "§f = §a" + value));
            }
        } catch (Exception e) { sendTransTo(player, "command.tonekoadmin.config.not_found", key); }
        return 1;
    }

    public static int configSet(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        String key = context.getArgument("key", String.class);
        String value = context.getArgument("value", String.class);
        ConfigUtil.CONFIG.set(key, value);
        ConfigUtil.CONFIG.save();
        ConfigUtil.load(); // hot-reload subsystems
        sendTransTo(player, "command.tonekoadmin.config.set", key, value);
        return 1;
    }

    public static int reloadCommand(CommandContext<CommandSourceStack> context) {
        reloadConfigCommand(context);
        reloadDataCommand(context);
        return 1;
    }

    public static int helpCommand(CommandContext<CommandSourceStack> context) {
        sendTransTo((Player) context.getSource().getSender(), "command.tonekoadmin.help");
        return 1;
    }

    // === Neko management ===

    private static Player resolveNeko(CommandContext<CommandSourceStack> context) {
        try {
            return context.getArgument("player", PlayerSelectorArgumentResolver.class)
                    .resolve(context.getSource()).getFirst();
        } catch (CommandSyntaxException e) { return null; }
    }

    private static int nekoInfoCommand(CommandContext<CommandSourceStack> context) {
        Player p = resolveNeko(context);
        Player sender = (Player) context.getSource().getSender();
        if (p == null) return 0;
        NekoQuery.Neko n = NekoQuery.getNeko(p.getUniqueId());
        sender.sendMessage(Component.text("§6===== Neko: " + p.getName() + " ====="));
        sender.sendMessage(Component.text("§eIsNeko: §f" + n.isNeko()));
        sender.sendMessage(Component.text("§eLevel: §f" + String.format("%.1f", n.getLevel())));
        sender.sendMessage(Component.text("§eEnergy: §f" + String.format("%.1f", n.getNekoEnergy()) + " / " + String.format("%.1f", n.getMaxNekoEnergy())));
        sender.sendMessage(Component.text("§eAge: §f" + n.getNekoAge() + " §7(baby=" + n.isNekoBaby() + ")"));
        sender.sendMessage(Component.text("§eNick: §f" + (n.getNickName().isEmpty() ? "§7(none)" : n.getNickName())));
        sender.sendMessage(Component.text("§eOwners: §f" + n.getOwners().size()));
        sender.sendMessage(Component.text("§eQuirks: §f" + n.getQuirks().size()));
        return 1;
    }

    private static int nekoSetBaby(CommandContext<CommandSourceStack> context) {
        Player p = resolveNeko(context);
        if (p == null) return 0;
        String sv = context.getArgument("value", String.class);
        boolean val = sv.equalsIgnoreCase("true") || sv.equals("1");
        NekoQuery.getNeko(p.getUniqueId()).setNekoBaby(val);
        sendTransTo(p, val ? "command.tonekoadmin.neko.set_baby.true" : "command.tonekoadmin.neko.set_baby.false");
        return 1;
    }

    private static int nekoSetEnergy(CommandContext<CommandSourceStack> context) {
        Player p = resolveNeko(context);
        if (p == null) return 0;
        float val;
        try { val = Float.parseFloat(context.getArgument("value", String.class)); } catch (NumberFormatException e) { return 0; }
        NekoQuery.getNeko(p.getUniqueId()).setNekoEnergy(val);
        return 1;
    }

    private static int nekoSetAge(CommandContext<CommandSourceStack> context) {
        Player p = resolveNeko(context);
        if (p == null) return 0;
        int val;
        try { val = Integer.parseInt(context.getArgument("value", String.class)); } catch (NumberFormatException e) { return 0; }
        NekoQuery.getNeko(p.getUniqueId()).setNekoAge(val);
        sendTransTo(p, "command.tonekoadmin.neko.set_age", String.valueOf(val));
        return 1;
    }

    private static int nekoSetNickName(CommandContext<CommandSourceStack> context) {
        Player p = resolveNeko(context);
        if (p == null) return 0;
        String val = context.getArgument("value", String.class);
        NekoQuery.getNeko(p.getUniqueId()).setNickName(val);
        sendTransTo(p, "command.tonekoadmin.neko.set_nickname", val);
        return 1;
    }

    public static int setCommand(CommandContext<CommandSourceStack> context) {
        Player nekoPlayer;
        try {
            nekoPlayer = context.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(context.getSource()).getFirst();
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
        boolean isNeko = NekoQuery.isNeko(nekoPlayer.getUniqueId());
        if (isNeko){
            NekoQuery.setNeko(nekoPlayer.getUniqueId(), false);
            sendTransTo(nekoPlayer,"command.tonekoadmin.set.false", nekoPlayer.getName());
        }else {
            NekoQuery.setNeko(nekoPlayer.getUniqueId(), true);
            sendTransTo(nekoPlayer,"command.tonekoadmin.set.true", nekoPlayer.getName());
        }
        return 1;
    }

    // === AI management ===

    private static int aiList(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        String activeId = ConfigUtil.getAIService();
        StringBuilder sb = new StringBuilder("§6===== AI Providers =====\n");
        for (AIServiceProvider p : AIServiceProviderRegistry.getAll()) {
            String marker = p.getProviderId().equalsIgnoreCase(activeId) ? "§a[ACTIVE] " : "§7";
            sb.append(marker).append("§e").append(p.getProviderId())
                    .append(" §7- ").append(p.getDisplayName())
                    .append(" §7(").append(p.getDefaultModel()).append(")\n");
        }
        player.sendMessage(Component.text(sb.toString().trim()));
        return 1;
    }

    private static int aiSwitch(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        String providerId = context.getArgument("provider", String.class);
        AIServiceProvider provider = AIServiceProviderRegistry.get(providerId);
        if (provider == null) {
            sendTransTo(player, "messages.toneko.ai.provider_not_found", providerId);
            return 0;
        }
        ConfigUtil.saveProviderConfig(ConfigUtil.getAIService());
        ConfigUtil.CONFIG.set("ai.service", providerId);
        ConfigUtil.loadProviderConfig(providerId);
        ConfigUtil.CONFIG.save();
        sendTransTo(player, "command.tonekoadmin.ai.switch", provider.getDisplayName());
        return 1;
    }

    private static int aiConfig(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        String providerId = context.getArgument("provider", String.class);
        String key = context.getArgument("key", String.class);
        String value = context.getArgument("value", String.class);
        if (AIServiceProviderRegistry.get(providerId) == null) {
            sendTransTo(player, "messages.toneko.ai.provider_not_found", providerId);
            return 0;
        }
        if (key.equalsIgnoreCase("key") || key.equalsIgnoreCase("model") || key.equalsIgnoreCase("base_url")) {
            ConfigUtil.CONFIG.set("ai.providers." + providerId + "." + key, value);
            if (providerId.equalsIgnoreCase(ConfigUtil.getAIService())) {
                ConfigUtil.CONFIG.set("ai." + key, value);
            }
            ConfigUtil.CONFIG.save();
            sendTransTo(player, "command.tonekoadmin.ai.config", providerId, key, value);
        } else {
            sendTransTo(player, "command.tonekoadmin.ai.config.invalid_key", key);
        }
        return 1;
    }

    private static int aiTest(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        String message = context.getArgument("message", String.class);
        if (!ConfigUtil.isAIEnabled()) {
            sendTransTo(player, "messages.toneko.ai.not_enabled");
            return 0;
        }
        String providerId = ConfigUtil.getAIService();
        AIServiceProvider provider = AIServiceProviderRegistry.get(providerId);
        sendTransTo(player, "command.tonekoadmin.ai.test.sending",
                provider != null ? provider.getDisplayName() : providerId);
        UUID testUuid = UUID.randomUUID();
        AIUtil.sendMessage(testUuid, player.getUniqueId(), ConfigUtil.getAIPrompt(), message, response -> {
            Bukkit.getScheduler().runTask(INSTANCE, () -> {
                player.sendMessage(Component.text("§b[AI Test] §f" + response.getResponse()));
            });
        });
        return 1;
    }
}
