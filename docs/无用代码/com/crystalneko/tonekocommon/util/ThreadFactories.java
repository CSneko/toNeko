package com.crystalneko.tonekocommon.util;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadFactories {
    public static class StatsThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final AtomicInteger count = new AtomicInteger(1);

        public StatsThreadFactory() {
            this.namePrefix = "ToNeko Stats Thread";
        }

        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread t = new Thread(r, namePrefix + "-pool-" + count.getAndIncrement());
            t.setPriority(3);
            return t;
        }
    }

    public static class ChatThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final AtomicInteger count = new AtomicInteger(1);

        public ChatThreadFactory() {
            this.namePrefix = "ToNeko Chat Thread";
        }

        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread t = new Thread(r, namePrefix + "-pool-" + count.getAndIncrement());
            t.setPriority(3);
            return t;
        }
    }
}
