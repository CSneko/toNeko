package org.cneko.toneko.common.util.scheduled;

import org.cneko.toneko.bukkit.util.BukkitSchedulerPool;
import org.cneko.toneko.bukkit.util.FoliaSchedulerPoolImpl;
import org.cneko.toneko.fabric.util.FabricSchedulerPoolImpl;
import org.jetbrains.annotations.NotNull;

public class SchedulerPoolProvider {
    private static final ISchedulerPool INSTANCE;

    static {
        ISchedulerPool warpped;

        try {
            Class.forName("org.bukkit.Server"); //Check if is bukkit

            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                //Folia platform
                warpped = new FoliaSchedulerPoolImpl();
            }catch (Exception ignored){
                //Non folia platform
                warpped = new BukkitSchedulerPool();
            }
        }catch (Exception ignored){
            warpped = new FabricSchedulerPoolImpl();
        }

        INSTANCE = warpped;
    }

    @NotNull
    public static ISchedulerPool getINSTANCE() {
        return INSTANCE;
    }
}
