package org.cneko.toneko.bukkit.util;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.cneko.toneko.bukkit.api.ClientStatus;
import org.cneko.toneko.common.util.LanguageUtil;

public class MsgUtil {
    public static void sendTransTo(Player player, String key, Object... args) {
        // 如果客户端安装了模组，直接使用 Component.translatable 发送
        if (ClientStatus.isInstalled(player)) {
            // 构建带有参数的 Component.translatable
            Component translatableMessage = Component.translatable(key, argsToComponents(args));
            player.sendMessage(translatableMessage);
        } else {
            // 未安装模组的情况下，使用 String.format 替换 %s 占位符
            String translatedMessage = LanguageUtil.translatable(key,args);
            player.sendMessage(Component.text(translatedMessage));
        }
    }

    // 辅助方法: 将 Object[] 转换为 Component[]
    private static Component[] argsToComponents(Object[] args) {
        Component[] components = new Component[args.length];
        for (int i = 0; i < args.length; i++) {
            components[i] = Component.text(args[i].toString());
        }
        return components;
    }
}
