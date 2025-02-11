package org.cneko.toneko.common.mod.util;

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
        register(COMMAND_TONEKOADMIN_HELP);
        register(COMMAND_TONEKOADMIN_DATA);
        register(COMMAND_TONEKO_PLAYER);
        register(COMMAND_TONEKO_ACCEPT);
        register(COMMAND_TONEKO_DENY);
        register(COMMAND_TONEKO_ALIAS);
        register(COMMAND_TONEKO_BLOCK);
        register(COMMAND_TONEKO_XP);
        register(COMMAND_TONEKO_REMOVE);
        register(COMMAND_TONEKO_HELP);
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
    }
    // 是否拥有权限
    public static boolean has(Entity entity, String perm){
        try {
            if (installed) {
                return Permissions.check(entity, perm);
            }
            // 没有权限API
            return entity.hasPermissions(getPermLevel(perm));
        }catch (Exception e){
            return false;
        }
    }
    // 权限是否属于管理员权限
    public static int getPermLevel(String perm){
        if( perm.startsWith("command.tonekoadmin")){
            return 4;
        }else if (perm.startsWith("command.neko") || perm.startsWith("command.quirk") || perm.startsWith("command.toneko")){
            return 1;
        }
        return 1;
    }


    public static boolean has(CommandSourceStack source, String permission){
        try {
            // 如果是终端执行，则直接返回true
            if (source.getEntity() == null) {
                return true;
            }
            if (installed) {
                return Permissions.check(source, permission);
            }
            // 没有权限API
            return source.hasPermission(getPermLevel(permission));
        }catch (Exception e){
            return false;
        }
    }
    @Deprecated
    public static boolean has(String permission, CommandSourceStack source) {
        return has(source, permission);
    }
}
