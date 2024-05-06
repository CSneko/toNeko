package com.crystalneko.tonekocommon;

import com.crystalneko.tonekocommon.util.StringUtil;
import com.crystalneko.tonekocommon.util.ThreadFactories;
import org.cneko.ctlib.common.network.HttpGet;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Stats {
    private static final ExecutorService httpExecutor = Executors.newCachedThreadPool(new ThreadFactories.StatsThreadFactory());

    // 猫娘被撅统计
    public static void stick(String player,String neko){
        httpExecutor.execute(() -> {
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

        httpExecutor.execute(() -> {
            try {
                HttpGet.SimpleHttpGet.get("https://api.toneko.cneko.org/meow/add?name="+neko+"&&meow="+total,null);
            } catch (IOException ignored) {
            }
        });
    }

}
