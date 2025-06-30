package org.cneko.toneko.common;

import org.cneko.toneko.common.util.StringUtil;
import org.cneko.toneko.common.util.network.HttpGet;
import org.cneko.toneko.common.util.scheduled.SchedulerPoolProvider;

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
        int total = getMeow(message);
        meowInChat(neko,total);
    }
    public static void meowInChat(String neko,int count){
        SchedulerPoolProvider.getINSTANCE().executeAsync(() -> {
            try {
                HttpGet.SimpleHttpGet.get("https://api.toneko.cneko.org/meow/add?name="+neko+"&&meow="+count,null);
            } catch (IOException ignored) {
            }
        });
    }

    public static int getMeow(String message){
        // 获取喵字数量
        int count = StringUtil.getCount(message,"喵");
        int nya = StringUtil.getCount(message,"nya");
        int meow = StringUtil.getCount(message,"meow");
        return count + nya + meow;
    }

}
