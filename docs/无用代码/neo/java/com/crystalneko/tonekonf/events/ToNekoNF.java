package com.crystalneko.tonekonf.events;

import com.crystalneko.tonekofabric.entity.nekoEntity;
import com.crystalneko.tonekofabric.libs.base;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.cneko.ctlib.common.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Path;

@Mod(ToNekoNF.MODID)
public class ToNekoNF {
    public static final String MODID = "toneko";

    public static Boolean started = false;
    public static YamlConfiguration config;

    public static EntityType<nekoEntity> NEKO;
    public static Item NEKO_SPAWN_EGG;
    public static boolean isNewVersion = false;
    public ToNekoNF(IEventBus modEventBus){
        new base();
        //加载配置文件
        try {
            config = new YamlConfiguration(Path.of("ctlib/toneko/config.yml"));
        } catch (IOException e) {
            System.out.println("无法加载配置文件" + e.getMessage());
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {


    }
}
