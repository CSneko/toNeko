package com.crystalneko.tonekocommon;

import com.crystalneko.tonekocommon.util.StringUtil;
import org.cneko.ctlib.common.network.HttpGet;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Stats {
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    // 猫娘被撅统计
    public static void stick(String player,String neko){
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                HttpGet.SimpleHttpGet.get("api.toneko.cneko.org/stick/add?neko="+neko+"player="+player,null);
            } catch (IOException ignored) {
            }
        }, executorService);
    }

    // 聊天中包含喵字数量统计
    public static void meowInChat(String neko,String message){
        // 获取喵字数量
        int count = StringUtil.getCount(message,"喵");
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                HttpGet.SimpleHttpGet.get("api.toneko.cneko.org/meow/add?name="+neko+"&&meow="+count,null);
            } catch (IOException ignored) {
            }
        }, executorService);
    }
}
