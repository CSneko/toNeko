package org.cneko.toneko.common;

import org.jetbrains.annotations.NotNull;

public interface ISchedulerPool {
    ScheduledTask scheduleAsync(@NotNull Runnable task, long delayedTicks);

    ScheduledTask executeAsync(@NotNull Runnable task);

    void cancelTask(@NotNull Object taskMaker);
}
