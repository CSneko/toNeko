package org.cneko.toneko.common.mod.testing;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.knot.Knot;

/** Load the production riding mixin through Fabric's real transformer, without starting a world. */
public final class CompanionMixinLauncher {
    public static void main(String[] args) throws Exception {
        System.setProperty("fabric.skipMcProvider", "true");
        boolean client = args.length > 0 && args[0].equals("client");
        var loader = new Knot(client ? EnvType.CLIENT : EnvType.SERVER).init(new String[0]);
        loader.loadClass(client ? "org.cneko.toneko.common.mod.client.renderers.MushroomClientRegressionTest"
                : "org.cneko.toneko.common.mod.entities.MushroomCompanionRegressionTest")
                .getMethod("main", String[].class).invoke(null, (Object) new String[0]);
    }
}
