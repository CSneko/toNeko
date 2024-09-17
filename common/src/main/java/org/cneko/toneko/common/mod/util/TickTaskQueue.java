package org.cneko.toneko.common.mod.util;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TickTaskQueue implements ITickable {
    private final PriorityQueue<TickTask> tasks = new PriorityQueue<>(Comparator.comparingInt(t -> t.executeTick));
    private final AtomicInteger currentTick = new AtomicInteger(0);
    private int lastExecutionTick = 0;  // 上一个任务完成后的 tick 值

    public void addTask(int delayTicks, Runnable task) {
        // 计算任务的相对执行时间
        int executeTick = lastExecutionTick + delayTicks;
        tasks.offer(new TickTask(executeTick, task));
    }

    public void addTick() {
        addTick(1);
    }

    @Override
    public void addTick(int ticks) {
        currentTick.addAndGet(ticks);  // 更新当前 tick 值

        // 执行任务的逻辑
        while (!tasks.isEmpty() && currentTick.get() >= tasks.peek().executeTick) {
            TickTask nextTask = tasks.poll(); // 取出最早执行的任务
            nextTask.runTask();  // 执行任务
            lastExecutionTick = currentTick.get(); // 更新上一个任务的结束 tick
        }
    }

    @Override
    public void addRemoveTask() {
        // 移除所有已过期的任务
        while (!tasks.isEmpty() && currentTick.get() >= tasks.peek().executeTick) {
            tasks.poll();
        }
    }

    @Override
    public boolean isRemoved() {
        // 检查当前队列是否为空
        return tasks.isEmpty();
    }

    public void clear() {
        tasks.clear();
    }

    private static class TickTask {
        int executeTick;  // 相对执行时间
        Runnable task;

        TickTask(int executeTick, Runnable task) {
            this.executeTick = executeTick;
            this.task = task;
        }

        void runTask() {
            task.run();
        }
    }
}
