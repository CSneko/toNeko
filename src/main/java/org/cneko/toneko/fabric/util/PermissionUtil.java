package org.cneko.toneko.fabric.util;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.entity.Entity;

import java.util.UUID;

public class PermissionUtil {
    public static boolean installed = false;
    public static UUID uuid = UUID.fromString("bf6bd75f-333b-459b-b9dd-f38b57fb82e3");
    public static String COMMAND_TONEKOADMIN_SET = "command.tonekoadmin.set";
    public static String COMMAND_TONEKOADMIN_RELOAD = "command.tonekoadmin.reload";
    public static String COMMAND_TONEKO_PLAYER = "command.toneko.player";
    public static String COMMAND_TONEKO_ALIAS = "command.toneko.alias";
    public static String COMMAND_TONEKO_BLOCK = "command.toneko.block";
    public static String COMMAND_TONEKO_XP = "command.toneko.xp";
    public static String COMMAND_TONEKO_REMOVE = "command.toneko.remove";
    public static String COMMAND_TONEKO_HELP = "command.toneko.help";
    public static void init() {
        // 是否有permissions API
        try {
            Class.forName("me.lucko.fabric.api.permissions.v0.Permissions");
            installed = true;
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
    }
    // 是否拥有权限
    public static boolean has(Entity entity, String perm){
        if(installed){
            return Permissions.check(entity, perm);
        }
        // 没有权限API，默认拥有非管理员权限，3级默认拥有管理员权限
        if(entity.hasPermissionLevel(3)){
            return true;
        }
        return !isAdminPerm(perm);
    }
    // 权限是否属于管理员权限
    public static boolean isAdminPerm(String perm){
        return perm.startsWith("command.tonekoadmin");
    }
}
