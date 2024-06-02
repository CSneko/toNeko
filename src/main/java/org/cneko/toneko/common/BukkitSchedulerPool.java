package org.cneko.toneko.common;

import com.crystalneko.toneko.ToNeko;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public class BukkitSchedulerPool implements ISchedulerPool {
    @Override
    public ScheduledTask scheduleAsync(@NotNull Runnable task, long delayedTicks) {
        final ScheduledTask wrapped = new ScheduledTask(task,this);
        wrapped.setTaskMarker(Bukkit.getScheduler().scheduleAsyncDelayedTask(ToNeko.pluginInstance,wrapped,delayedTicks));
        return wrapped;
    }

    @Override
    public ScheduledTask executeAsync(@NotNull Runnable task) {
        final ScheduledTask wrapped = new ScheduledTask(task,this);
        wrapped.setTaskMarker(Bukkit.getScheduler().runTaskAsynchronously(ToNeko.pluginInstance,wrapped));
        return wrapped;
    }

    @Override
    public void cancelTask(@NotNull Object taskMaker) {
        if (taskMaker instanceof Integer taskId){
            Bukkit.getScheduler().cancelTask(taskId);
        }else if (taskMaker instanceof BukkitTask bTask){
            bTask.cancel();
        }
    }
}
