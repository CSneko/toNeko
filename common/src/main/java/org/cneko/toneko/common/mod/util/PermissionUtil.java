package org.cneko.toneko.common.mod.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.entity.Entity;

import static org.cneko.toneko.common.api.Permissions.*;

/**
 * 26.x：原 fabric-permissions-api 依赖仍指向 yarn 中间名（class_2172 等），无法解析，
 * 且 1.21.9+ 权限系统重构为 {@link net.minecraft.server.permissions.PermissionSet}。
 * 这里统一改为使用原版基于等级的权限集合；
 * LuckPerms 集成留给后续版本的 permissions.json / 数据包方式实现。
 */
public class PermissionUtil {
    public static boolean installed = false;

    public static void init() {
        // 原权限 API 在 26.x 下不可用，仅保留接口占位
        installed = false;
    }

    /** 兼容保留：注册全部权限节点（当前为 no-op） */
    public static void register(String perm) {
        // no-op: 数据包/permissions.json 注册未来在这里接入
    }

    /** 兼容保留 */
    public static void registerAll() {
        register(COMMAND_TONEKOADMIN);
        register(COMMAND_TONEKOADMIN_SET);
        register(COMMAND_TONEKOADMIN_SET_LEVEL);
        register(COMMAND_TONEKOADMIN_RELOAD);
        register(COMMAND_TONEKOADMIN_HELP);
        register(COMMAND_TONEKOADMIN_DATA);
        register(COMMAND_TONEKOADMIN_CONFIG);
        register(COMMAND_TONEKO_PLAYER);
        register(COMMAND_TONEKO_ACCEPT);
        register(COMMAND_TONEKO_DENY);
        register(COMMAND_TONEKO_ALIAS);
        register(COMMAND_TONEKO_BLOCK);
        register(COMMAND_TONEKO_XP);
        register(COMMAND_TONEKO_REMOVE);
        register(COMMAND_TONEKO_HELP);
        register(COMMAND_TONEKO_GUI);
        register(COMMAND_NEKO_SPEED);
        register(COMMAND_NEKO_VISION);
        register(COMMAND_NEKO_JUMP);
        register(COMMAND_NEKO_LEVEL);
        register(COMMAND_NEKO_LORE);
        register(COMMAND_NEKO_GET_DOWN);
        register(COMMAND_NEKO_LIE);
        register(COMMAND_NEKO_NICKNAME);
        register(COMMAND_QUIRK);
        register(COMMAND_QUIRK_ADD);
        register(COMMAND_QUIRK_GUI);
        register(COMMAND_QUIRK_LIST);
        register(COMMAND_QUIRK_HELP);
        register(COMMAND_GENETICS);
    }

    /**
     * 是否拥有权限：玩家走其权限集合与需求等级比较；非玩家实体一律 false。
     */
    public static boolean has(Entity entity, String perm) {
        if (!(entity instanceof ServerPlayer player)) {
            return false;
        }
        return levelAtLeast(player.permissions(), getPermLevel(perm));
    }

    /** 权限等级映射：admin 节点 4 级，玩家可用节点 0 级。 */
    public static int getPermLevel(String perm) {
        if (perm.startsWith("command.tonekoadmin")) {
            return 4;
        } else if (perm.startsWith("command.neko") || perm.startsWith("command.quirk") || perm.startsWith("command.toneko")) {
            return 0;
        }
        return 0;
    }

    /**
     * 命令来源权限校验：
     * 控制台（无实体来源）恒通过；命令块/函数等按来源的权限集合检查，
     * 防止命令块绕过 /tonekoadmin 的管理员限制。
     */
    public static boolean has(CommandSourceStack source, String permission) {
        try {
            if (source.getEntity() == null) {
                // 控制台永远放行
                return true;
            }
            return levelAtLeast(source.permissions(), getPermLevel(permission));
        } catch (Exception e) {
            return false;
        }
    }

    @Deprecated
    public static boolean has(String permission, CommandSourceStack source) {
        return has(source, permission);
    }

    private static boolean levelAtLeast(PermissionSet set, int requiredLevel) {
        if (set == null) return false;
        if (set == PermissionSet.ALL_PERMISSIONS) return true;
        if (set instanceof LevelBasedPermissionSet levelBased) {
            return levelBased.level().isEqualOrHigherThan(levelFromInt(requiredLevel));
        }
        return false;
    }

    private static PermissionLevel levelFromInt(int level) {
        return switch (level) {
            case 0 -> PermissionLevel.ALL;
            case 1 -> PermissionLevel.MODERATORS;
            case 2 -> PermissionLevel.GAMEMASTERS;
            case 3 -> PermissionLevel.ADMINS;
            default -> PermissionLevel.OWNERS;
        };
    }
}
