package org.cneko.toneko.common;

import com.crystalneko.tonekocommon.util.StringUtil;
import org.cneko.ctlib.common.network.HttpGet;
import org.cneko.toneko.common.util.SchedulerPoolProvider;

import java.io.IOException;

public class Stats {

    // 猫娘被撅统计
    public static void stick(String player,String neko){
        SchedulerPoolProvider.getINSTANCE().executeAsync(() -> {
            try {
                HttpGet.SimpleHttpGet.get("https://api.toneko.cneko.org/stick/add?neko="+neko+"&&player="+player,null);
            } catch (IOException ignored) {
            }
        });
    }

    // 聊天中包含喵字数量统计
    public static void meowInChat(String neko,String message){
        // 获取喵字数量
        int count = StringUtil.getCount(message,"喵");
        int nya = StringUtil.getCount(message,"nya");
        int total = count + nya;

        SchedulerPoolProvider.getINSTANCE().executeAsync(() -> {
            try {
                HttpGet.SimpleHttpGet.get("https://api.toneko.cneko.org/meow/add?name="+neko+"&&meow="+total,null);
            } catch (IOException ignored) {
            }
        });
    }

}
