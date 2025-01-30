package org.cneko.toneko.common.util.scheduled;

import org.jetbrains.annotations.NotNull;

public class SchedulerPoolProvider {
    public static ISchedulerPool INSTANCE;


    @NotNull
    public static ISchedulerPool getINSTANCE() {
        return INSTANCE;
    }
}
