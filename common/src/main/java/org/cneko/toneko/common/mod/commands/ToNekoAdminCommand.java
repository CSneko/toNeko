package org.cneko.toneko.common.mod.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.cneko.toneko.common.api.Permissions;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.api.NekoLevelRegistry;
import org.cneko.toneko.common.util.ConfigBuilder;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;
import org.cneko.toneko.common.mod.util.PermissionUtil;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static org.cneko.toneko.common.mod.util.TextUtil.translatable;

public class ToNekoAdminCommand {
    private static final SuggestionProvider<CommandSourceStack> CONFIG_KEYS = (context, builder) -> {
        ConfigUtil.CONFIG_BUILDER.getKeys().forEach(builder::suggest);
        return builder.buildFuture();
    };
    public static void init(){
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            //------------------------------------------------toneko-----------------------------------------------
            dispatcher.register(literal("tonekoadmin")
                    .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_TONEKOADMIN))
                    .then(literal("set")
                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_TONEKOADMIN_SET))
                            .then(argument("neko", EntityArgument.player())
                                    .then(argument("is",BoolArgumentType.bool())
                                            .executes(ToNekoAdminCommand::set)
                                    )
                            )

                    )
                    .then(literal("setLevel")
                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_TONEKOADMIN_SET_LEVEL))
                            .then(argument("neko",EntityArgument.player())
                                    .then(argument("level", FloatArgumentType.floatArg(0f,1000f))
                                            .executes(ToNekoAdminCommand::setLevel)
                                    )
                            )
                    )
                    .then(literal("reload")
                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_TONEKOADMIN_RELOAD))
                            .executes(ToNekoAdminCommand::reload)
                            .then(literal("config")
                                    .executes(ToNekoAdminCommand::reloadConfig)
                            )
                   )
                    .then(literal("config")
                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_TONEKOADMIN_CONFIG))
                            .then(literal("get")
                                    .then(argument("key", StringArgumentType.string())
                                            .suggests(CONFIG_KEYS)
                                            .executes(ToNekoAdminCommand::getConfig)
                                    )
                            )
                            .then(literal("set")
                                    .then(argument("key", StringArgumentType.string())
                                            .suggests(CONFIG_KEYS)
                                            .then(argument("value", StringArgumentType.string())
                                                    .executes(ToNekoAdminCommand::setConfig)
                                            )
                                    )
                            )
                            .then(literal("reload")
                                    .executes(ToNekoAdminCommand::reloadConfig)
                            )
                    )
                    .then(literal("neko")
                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_TONEKOADMIN_NEKO))
                            .then(argument("entity", EntityArgument.entity())
                                    .executes(ToNekoAdminCommand::nekoInfo)
                                    .then(literal("setNeko")
                                            .then(argument("value", BoolArgumentType.bool())
                                                    .executes(ToNekoAdminCommand::nekoSetNeko)
                                            )
                                    )
                                    .then(literal("setAge")
                                            .then(argument("value", IntegerArgumentType.integer())
                                                    .executes(ToNekoAdminCommand::nekoSetAge)
                                            )
                                    )
                                    .then(literal("setBaby")
                                            .then(argument("value", BoolArgumentType.bool())
                                                    .executes(ToNekoAdminCommand::nekoSetBaby)
                                            )
                                    )
                                    .then(literal("setEnergy")
                                            .then(argument("value", FloatArgumentType.floatArg(0))
                                                    .executes(ToNekoAdminCommand::nekoSetEnergy)
                                            )
                                    )
                                    .then(literal("setNickName")
                                            .then(argument("value", StringArgumentType.greedyString())
                                                    .executes(ToNekoAdminCommand::nekoSetNickName)
                                            )
                                    )
                                    .then(literal("setLevelFactor")
                                            .then(argument("factorId", StringArgumentType.string())
                                                    .then(argument("value", DoubleArgumentType.doubleArg(0))
                                                            .executes(ToNekoAdminCommand::nekoSetLevelFactor)
                                                    )
                                            )
                                    )
                            )
                    )
                    .then(literal("help")
                            .requires(source -> PermissionUtil.has(source, Permissions.COMMAND_TONEKOADMIN_HELP))
                            .executes(ToNekoAdminCommand::help)
                    )
            );
        });
    }

    private static int setLevel(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer neko;
        try {
            neko = EntityArgument.getPlayer(context, "neko");
        } catch (CommandSyntaxException e) {
            return 0;
        }
        float level = FloatArgumentType.getFloat(context, "level");
        org.cneko.toneko.common.mod.api.NekoLevelRegistry.base().setRaw(neko, level);
        source.sendSystemMessage(translatable("command.tonekoadmin.set_level", neko.getName().getString(), level));
        return 1;
    }

    private static int setConfig(CommandContext<CommandSourceStack> context) {
        String key = StringArgumentType.getString(context, "key");
        String value = StringArgumentType.getString(context, "value");
        try {
            // 检查类型
            final var builder = ConfigUtil.CONFIG_BUILDER;
            final var config = ConfigUtil.CONFIG;
            if(!config.contains(key)){
                context.getSource().sendSystemMessage(translatable("command.tonekoadmin.config.not_found", key));
                return 1;
            }
            final var type = builder.get(key).type();
            if (type == ConfigBuilder.Entry.Types.BOOLEAN &&(value.equals("true") || value.equals("false") || value.equals("1") || value.equals("0"))){
                config.set(key, Boolean.parseBoolean(value));
            } else if (type == ConfigBuilder.Entry.Types.NUMBER && value.matches("[0-9]+")) {
                config.set(key, Integer.parseInt(value));
            } else if (type == ConfigBuilder.Entry.Types.STRING) {
                config.set(key, value);
            }else {
                context.getSource().sendSystemMessage(translatable("command.tonekoadmin.config.type_error"));
                return 1;
            }
            config.save();
            context.getSource().sendSystemMessage(translatable("command.tonekoadmin.config.set", key, value));
        }catch (Exception ignored){}
        return 1;
    }

    private static int getConfig(CommandContext<CommandSourceStack> context) {
        String key = StringArgumentType.getString(context, "key");
        try {
            if (!ConfigUtil.CONFIG.contains(key)){
                context.getSource().sendSystemMessage(translatable("command.tonekoadmin.config.not_found", key));
            }
            Object value = ConfigUtil.CONFIG.get(key);
            context.getSource().sendSystemMessage(translatable("command.tonekoadmin.config.get", key, value));
        }catch (Exception ignored){}
        return 1;
    }



    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        // 重新加载配置文件和语言文件
        ConfigUtil.load();
        LanguageUtil.load();
        context.getSource().sendSystemMessage(translatable("command.tonekoadmin.reload"));
        return 1;
    }
    public static int reload(CommandContext<CommandSourceStack> context) {
        reloadConfig(context);
        return 1;
    }

    public static int help(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSystemMessage(translatable("command.tonekoadmin.help"));
        return 1;
    }


    public static int set(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer neko;
        try {
            neko = EntityArgument.getPlayer(context, "neko");
        } catch (CommandSyntaxException e) {
            return 0;
        }
        boolean isNeko = context.getArgument("is", Boolean.class);
        if(isNeko){
            neko.setNeko(true);
            source.sendSystemMessage(translatable("command.tonekoadmin.set.true", neko.getName().getString()));
        }else {
            neko.setNeko(false);
            source.sendSystemMessage(translatable("command.tonekoadmin.set.false", neko.getName().getString()));
        }
        return 1;
    }

    // ==================== neko entity management ====================

    private static INeko getNekoFromContext(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity entity = EntityArgument.getEntity(context, "entity");
        if (entity instanceof INeko neko) {
            return neko;
        }
        throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(
                translatable("command.tonekoadmin.neko.not_neko")
        ).create();
    }

    private static int nekoInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        INeko neko;
        try {
            neko = getNekoFromContext(context);
        } catch (CommandSyntaxException e) {
            source.sendSystemMessage(translatable("command.tonekoadmin.neko.not_neko"));
            return 0;
        }

        var entity = neko.getEntity();
        String name = entity.getName().getString();

        source.sendSystemMessage(Component.literal("§6===== Neko Info: " + name + " ====="));
        source.sendSystemMessage(Component.literal("§eType: §f" + (neko.isPlayer() ? "Player" : entity.getType().toString())));
        source.sendSystemMessage(Component.literal("§eIs Neko: §f" + neko.isNeko()));

        // Age
        int age = neko.getNekoAge();
        int maxAge = neko.getMaxAge();
        double ageProgress = age >= 0 ? 100.0 : Math.round((1.0 + (double) age / maxAge) * 1000.0) / 10.0;
        source.sendSystemMessage(Component.literal("§eIs Baby: §f" + neko.isNekoBaby()));
        source.sendSystemMessage(Component.literal("§eAge: §f" + age + " / " + maxAge + " §7(" + ageProgress + "%)"));
        source.sendSystemMessage(Component.literal("§eGrowth Scale: §f" + String.format("%.2f", neko.getNekoAgeScale()) + " §7(" + (int)Math.round((neko.getNekoAgeScale() - 0.3) / 0.7 * 100) + "%)"));

        // Energy
        source.sendSystemMessage(Component.literal("§eEnergy: §f" + String.format("%.1f", neko.getNekoEnergy()) + " / " + String.format("%.1f", neko.getMaxNekoEnergy())));

        // Level
        source.sendSystemMessage(Component.literal("§eLevel: §f" + String.format("%.1f", neko.getNekoLevel())));
        source.sendSystemMessage(Component.literal("§e  - base: §f" + String.format("%.1f", neko.getNekoLevelFactorRaw("base"))));
        source.sendSystemMessage(Component.literal("§e  - interaction: §f" + String.format("%.1f", neko.getNekoLevelFactorRaw("interaction"))));
        source.sendSystemMessage(Component.literal("§e  - combat: §f" + String.format("%.1f", neko.getNekoLevelFactorRaw("combat"))));

        // Nickname
        String nickName = neko.getNickName();
        source.sendSystemMessage(Component.literal("§eNickname: §f" + (!nickName.isEmpty() ? nickName : "§7(not set)")));

        // Owners
        int ownerCount = neko.getOwners().size();
        source.sendSystemMessage(Component.literal("§eOwners: §f" + ownerCount));

        // Quirks
        int quirkCount = neko.getQuirks().size();
        source.sendSystemMessage(Component.literal("§eQuirks: §f" + quirkCount));

        return 1;
    }

    private static int nekoSetNeko(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        INeko neko;
        try {
            neko = getNekoFromContext(context);
        } catch (CommandSyntaxException e) {
            source.sendSystemMessage(translatable("command.tonekoadmin.neko.not_neko"));
            return 0;
        }
        boolean value = context.getArgument("value", Boolean.class);
        neko.setNeko(value);
        source.sendSystemMessage(translatable("command.tonekoadmin.neko.set_neko", neko.getEntity().getName().getString(), value));
        return 1;
    }

    private static int nekoSetAge(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        INeko neko;
        try {
            neko = getNekoFromContext(context);
        } catch (CommandSyntaxException e) {
            source.sendSystemMessage(translatable("command.tonekoadmin.neko.not_neko"));
            return 0;
        }
        int value = context.getArgument("value", Integer.class);
        neko.setNekoAge(value);
        source.sendSystemMessage(translatable("command.tonekoadmin.neko.set_age", neko.getEntity().getName().getString(), value));
        return 1;
    }

    private static int nekoSetBaby(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        INeko neko;
        try {
            neko = getNekoFromContext(context);
        } catch (CommandSyntaxException e) {
            source.sendSystemMessage(translatable("command.tonekoadmin.neko.not_neko"));
            return 0;
        }
        boolean value = context.getArgument("value", Boolean.class);
        neko.setNekoBaby(value);
        source.sendSystemMessage(translatable("command.tonekoadmin.neko.set_baby", neko.getEntity().getName().getString(), value));
        return 1;
    }

    private static int nekoSetEnergy(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        INeko neko;
        try {
            neko = getNekoFromContext(context);
        } catch (CommandSyntaxException e) {
            source.sendSystemMessage(translatable("command.tonekoadmin.neko.not_neko"));
            return 0;
        }
        float value = context.getArgument("value", Float.class);
        neko.setNekoEnergy(value);
        source.sendSystemMessage(translatable("command.tonekoadmin.neko.set_energy", neko.getEntity().getName().getString(), String.format("%.1f", value)));
        return 1;
    }

    private static int nekoSetNickName(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        INeko neko;
        try {
            neko = getNekoFromContext(context);
        } catch (CommandSyntaxException e) {
            source.sendSystemMessage(translatable("command.tonekoadmin.neko.not_neko"));
            return 0;
        }
        String value = context.getArgument("value", String.class);
        neko.setNickName(value);
        source.sendSystemMessage(translatable("command.tonekoadmin.neko.set_nickname", neko.getEntity().getName().getString(), value));
        return 1;
    }

    private static int nekoSetLevelFactor(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        INeko neko;
        try {
            neko = getNekoFromContext(context);
        } catch (CommandSyntaxException e) {
            source.sendSystemMessage(translatable("command.tonekoadmin.neko.not_neko"));
            return 0;
        }
        String factorId = context.getArgument("factorId", String.class);
        double value = context.getArgument("value", Double.class);
        neko.setNekoLevelFactorRaw(factorId, value);
        source.sendSystemMessage(translatable("command.tonekoadmin.neko.set_level_factor", neko.getEntity().getName().getString(), factorId, String.format("%.1f", value)));
        return 1;
    }
}
