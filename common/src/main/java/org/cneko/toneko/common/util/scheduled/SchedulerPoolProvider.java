package org.cneko.toneko.common.util.scheduled;

import org.jetbrains.annotations.NotNull;

public class SchedulerPoolProvider {
    private static final ISchedulerPool INSTANCE;

    static {
        ISchedulerPool warpped;
        warpped = new FabricSchedulerPoolImpl();


        INSTANCE = warpped;
    }

    @NotNull
    public static ISchedulerPool getINSTANCE() {
        return INSTANCE;
    }
}
