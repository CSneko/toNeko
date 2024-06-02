package org.cneko.toneko.common;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FabricSchedulerPoolImpl implements ISchedulerPool {
    private final ScheduledExecutorService internal = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    @Override
    public ScheduledTask scheduleAsync(@NotNull Runnable task, long delayedTicks) {
        final ScheduledTask wrapped = new ScheduledTask(task,this);
        wrapped.setTaskMarker(this.internal.schedule(wrapped,delayedTicks * 50, TimeUnit.MILLISECONDS));
        return wrapped;
    }

    @Override
    public ScheduledTask executeAsync(@NotNull Runnable task) {
        final ScheduledTask wrapped = new ScheduledTask(task,this);
        wrapped.setTaskMarker(this.internal.submit(wrapped));
        return wrapped;
    }

    //TODO: Should we complete it?
    @Override
    public ScheduledTask scheduleSync(@NotNull Runnable task, long delayedTicks, int chunkX, int chunkZ,Object level) {
        throw new UnsupportedOperationException("TO DO");
    }

    @Override
    public void cancelTask(@NotNull Object taskMaker) {
        ((Future<?>) taskMaker).cancel(true);
    }
}
