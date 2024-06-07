package org.cneko.toneko.fabric.util;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.entity.Entity;

import static org.cneko.toneko.common.api.Permissions.*;

public class PermissionUtil {
    public static boolean installed = false;

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
