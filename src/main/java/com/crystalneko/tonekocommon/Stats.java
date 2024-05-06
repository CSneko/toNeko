package com.crystalneko.tonekocommon;

import com.crystalneko.tonekocommon.util.StringUtil;
import com.crystalneko.tonekocommon.util.ThreadFactories;
import org.cneko.ctlib.common.network.HttpGet;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class Stats {
    private static final ExecutorService executorService = Executors.newCachedThreadPool(new ThreadFactories.StatsThreadFactory());
    // 猫娘被撅统计
    public static void stick(String player,String neko){
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                HttpGet.SimpleHttpGet.get("https://api.toneko.cneko.org/stick/add?neko="+neko+"&&player="+player,null);
            } catch (IOException ignored) {
            }
        }, executorService);
    }

    // 聊天中包含喵字数量统计
    public static void meowInChat(String neko,String message){
        // 获取喵字数量
        int count = StringUtil.getCount(message,"喵");
        int nya = StringUtil.getCount(message,"nya");
        int total = count+nya;
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                HttpGet.SimpleHttpGet.get("https://api.toneko.cneko.org/meow/add?name="+neko+"&&meow="+total,null);
            } catch (IOException ignored) {
            }
        }, executorService);
    }


}
