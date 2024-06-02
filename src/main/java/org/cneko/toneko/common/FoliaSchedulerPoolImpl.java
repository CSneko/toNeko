package org.cneko.toneko.common;

import com.crystalneko.toneko.ToNeko;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class FoliaSchedulerPoolImpl implements ISchedulerPool{
    @Override
    public ScheduledTask scheduleAsync(@NotNull Runnable task, long delayedTicks) {
        final ScheduledTask wrapped = new ScheduledTask(task,this);
        wrapped.setTaskMarker(Bukkit.getAsyncScheduler().runDelayed(ToNeko.pluginInstance, ignored -> wrapped.run(),delayedTicks * 50, TimeUnit.MILLISECONDS));
        return wrapped;
    }

    @Override
    public ScheduledTask executeAsync(@NotNull Runnable task) {
        final ScheduledTask wrapped = new ScheduledTask(task,this);
        wrapped.setTaskMarker(Bukkit.getAsyncScheduler().runNow(ToNeko.pluginInstance,ignored -> wrapped.run()));
        return wrapped;
    }

    @Override
    public ScheduledTask scheduleSync(@NotNull Runnable task, long delayedTicks, int chunkX, int chunkZ,Object level) {
        final ScheduledTask wrapped = new ScheduledTask(task,this);

        if (level == null){
            wrapped.setTaskMarker(Bukkit.getGlobalRegionScheduler().runDelayed(ToNeko.pluginInstance,ignored -> wrapped.run(),delayedTicks));
            return wrapped;
        }

        wrapped.setTaskMarker(Bukkit.getRegionScheduler().runDelayed(ToNeko.pluginInstance, ((World) level),chunkX,chunkZ,ignored -> wrapped.run(),delayedTicks));
        return wrapped;
    }

    @Override
    public void cancelTask(@NotNull Object taskMaker) {
        final io.papermc.paper.threadedregions.scheduler.ScheduledTask scheduledTask = ((io.papermc.paper.threadedregions.scheduler.ScheduledTask) taskMaker);
        scheduledTask.cancel();
    }
}
