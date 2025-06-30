package org.cneko.toneko.common.mod;

import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import org.cneko.toneko.common.mod.ai.Prompts;
import org.cneko.toneko.common.mod.commands.arguments.CustomStringArgument;
import org.cneko.toneko.common.mod.commands.arguments.NekoArgument;
import org.cneko.toneko.common.mod.quirks.ToNekoQuirks;
import org.cneko.toneko.common.util.scheduled.FabricSchedulerPoolImpl;
import org.cneko.toneko.common.util.scheduled.SchedulerPoolProvider;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

public class ModBootstrap {
    public static void bootstrap() {
        SchedulerPoolProvider.INSTANCE = new FabricSchedulerPoolImpl();
        ArgumentTypeRegistry.registerArgumentType(
                toNekoLoc("neko"),
                NekoArgument.class, SingletonArgumentInfo.contextFree(NekoArgument::neko));
        ArgumentTypeRegistry.registerArgumentType(
                toNekoLoc("custom_string"),
                CustomStringArgument.class,SingletonArgumentInfo.contextFree(CustomStringArgument::replaceWord)
        );
        Prompts.init();
        ToNekoQuirks.init();
    }
}
