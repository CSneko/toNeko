package org.cneko.toneko.common;

public class ScheduledTask implements Runnable{
    private final Runnable task;
    private final ISchedulerPool parent;
    private volatile Object taskMarker;
    private volatile boolean finished = false;

    public ScheduledTask(Runnable task, ISchedulerPool parent) {
        this.task = task;
        this.parent = parent;
    }

    public void cancel(){
        this.parent.cancelTask(this.taskMarker);
    }

    public void setTaskMarker(Object newMarker){
        this.taskMarker = newMarker;
    }

    @Override
    public void run() {
        try {
            this.task.run();
        }finally {
            this.finished = true;
        }
    }
}
