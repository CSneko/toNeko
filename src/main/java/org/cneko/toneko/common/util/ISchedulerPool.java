package org.cneko.toneko.common.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ISchedulerPool {
    ScheduledTask scheduleAsync(@NotNull Runnable task, long delayedTicks);

    ScheduledTask executeAsync(@NotNull Runnable task);

    ScheduledTask scheduleSync(@NotNull Runnable task,long delayedTicks,int chunkX,int chunkZ, @Nullable Object level);

    void cancelTask(@NotNull Object taskMaker);
}
