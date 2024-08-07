package org.cneko.toneko.fabric.util;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;

import static org.cneko.toneko.common.api.Permissions.*;

public class PermissionUtil {
    public static boolean installed = false;

    public static void init() {
        // 是否有permissions API 且安装了luckperms
        try {
            Class.forName("me.lucko.fabric.api.permissions.v0.Permissions");
            installed = FabricLoader.getInstance().isModLoaded("luckperms");
            registerAll();
        }catch (Exception e){
            installed = false;
        }
    }

    // 在luckperms中注册权限组
    public static void register(String perm){
        if(installed){
            Permissions.check(uuid, perm);
        }
    }
    // 注册所有权限
    public static void registerAll(){
        register(COMMAND_TONEKOADMIN_SET);
        register(COMMAND_TONEKOADMIN_RELOAD);
        register(COMMAND_TONEKO_PLAYER);
        register(COMMAND_TONEKO_ALIAS);
        register(COMMAND_TONEKO_BLOCK);
        register(COMMAND_TONEKO_XP);
        register(COMMAND_TONEKO_REMOVE);
        register(COMMAND_TONEKO_HELP);
        register(COMMAND_NEKO_SPEED);
        register(COMMAND_NEKO_VISION);
        register(COMMAND_NEKO_JUMP);
        register(COMMAND_QUIRK);
        register(COMMAND_QUIRK_HELP);
    }
    // 是否拥有权限
    public static boolean has(Entity entity, String perm){
        try {
            if (installed) {
                return Permissions.check(entity, perm);
            }
            // 没有权限API，默认拥有非管理员权限，3级默认拥有管理员权限
            if (entity.hasPermissions(3)) {
                return true;
            }
            return !isAdminPerm(perm);
        }catch (Exception e){
            return false;
        }
    }
    // 权限是否属于管理员权限
    public static boolean isAdminPerm(String perm){
        return perm.startsWith("command.tonekoadmin");
    }

    public static boolean has(CommandSourceStack source, String permission){
        return has(permission,source);
    }
    public static boolean has(String permission, CommandSourceStack source) {
        try {
            if (installed) {
                return Permissions.check(source, permission);
            }
            // 没有权限API，默认拥有非管理员权限，3级默认拥有管理员权限
            if (source.hasPermission(3)) {
                return true;
            }
            return !isAdminPerm(permission);
        }catch (Exception e){
            return false;
        }
    }
}
