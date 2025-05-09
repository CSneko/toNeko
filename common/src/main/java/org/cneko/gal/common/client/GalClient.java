package org.cneko.gal.common.client;

import net.fabricmc.fabric.impl.resource.loader.ModResourcePackUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.PackRepository;
import org.cneko.gal.common.util.pack.ExternalPack;

import java.nio.file.Path;

import static org.cneko.gal.common.Gal.GAL_MODID;

public class GalClient {
    public static void init(){
        ExternalPack.addResource(ResourceLocation.fromNamespaceAndPath("minecraft","textures/item/diamond_sword.png"),
                Path.of("test/test.png")
                );


    }
}
